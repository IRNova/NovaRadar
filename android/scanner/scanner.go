package main

import (
	"crypto/rand"
	"crypto/tls"
	"encoding/binary"
	"fmt"
	"io"
	"math/big"
	"net"
	"sort"
	"strconv"
	"sync"
	"sync/atomic"
	"time"
)

const (
	vlessUUID = "b9c40223-bbc5-4311-89d3-f1ed54bbca86"
	vlessSNI  = "nova2.altramax083.workers.dev"
)

var tlsPorts = map[int]bool{
	443: true, 2053: true, 2083: true, 2087: true, 2096: true, 8443: true,
}

type ScanResult struct {
	IP      string `json:"ip"`
	Port    int    `json:"port"`
	Link    string `json:"link"`
	Latency int64  `json:"latencyMs"`
}

type ScanStats struct {
	TotalScanned  int64  `json:"totalScanned"`
	TotalToScan   int64  `json:"totalToScan"`
	AliveCount    int32  `json:"aliveCount"`
	DeadCount     int32  `json:"deadCount"`
	Scanning      bool   `json:"scanning"`
	CurrentIP     string `json:"currentIP"`
	CurrentPort   int    `json:"currentPort"`
	ElapsedSec    int64  `json:"elapsedSec"`
	RemainingSec  int64  `json:"remainingSec"`
	SecondPass    bool   `json:"secondPass"`
}

func (a *App) StartScan(ports []int) {
	if a.scanning.Load() {
		return
	}
	a.scanning.Store(true)
	a.stopScan = make(chan struct{})
	a.resultsMu.Lock()
	a.results = nil
	a.resultsMu.Unlock()
	atomic.StoreInt64(&a.totalScanned, 0)
	atomic.StoreInt32(&a.aliveCount, 0)
	atomic.StoreInt32(&a.deadCount, 0)
	atomic.StoreInt64(&a.scanStartTime, 0)
	a.currentIP = ""
	a.currentPort = 0
	a.secondPass = false

	a.sourcesMu.Lock()
	sources := make([]IPSource, len(a.sources))
	copy(sources, a.sources)
	a.sourcesMu.Unlock()

	cidrs, directIPs := fetchIPsFromSources(sources)
	ips := generateRandomIPs(cidrs, 512)
	ips = append(ips, directIPs...)

	if len(ips) == 0 {
		a.scanning.Store(false)
		return
	}

	atomic.StoreInt64(&a.totalToScan, int64(len(ips)*len(ports))*2)
	a.scanIPs = ips

	go a.runScan(ports)
}

func (a *App) StopScan() {
	if !a.scanning.Load() {
		return
	}
	a.scanning.Store(false)
	if a.stopScan != nil {
		close(a.stopScan)
	}
}

func (a *App) GetScanResults() []ScanResult {
	a.resultsMu.Lock()
	defer a.resultsMu.Unlock()
	results := make([]ScanResult, len(a.results))
	copy(results, a.results)
	return results
}

func (a *App) GetScanStats() ScanStats {
	elapsed := int64(0)
	remaining := int64(0)
	start := atomic.LoadInt64(&a.scanStartTime)
	if start > 0 {
		elapsed = time.Now().Unix() - start
	}
	total := atomic.LoadInt64(&a.totalToScan)
	scanned := atomic.LoadInt64(&a.totalScanned)
	if scanned > 0 && elapsed > 5 {
		rate := scanned / elapsed
		if rate > 0 {
			remaining = (total - scanned) / rate
		}
	}
	return ScanStats{
		TotalScanned: scanned,
		TotalToScan:  total,
		AliveCount:   atomic.LoadInt32(&a.aliveCount),
		DeadCount:    atomic.LoadInt32(&a.deadCount),
		Scanning:     a.scanning.Load(),
		CurrentIP:    a.currentIP,
		CurrentPort:  a.currentPort,
		ElapsedSec:   elapsed,
		RemainingSec: remaining,
		SecondPass:   a.secondPass,
	}
}

func (a *App) runScan(ports []int) {
	atomic.StoreInt64(&a.scanStartTime, time.Now().Unix())
	defer func() {
		a.scanning.Store(false)
		a.emitEvent("scan:complete", a.GetScanStats())
	}()

	candidates := a.quickScan(ports)
	if !a.scanning.Load() || len(candidates) == 0 {
		return
	}

	a.secondPass = true
	a.emitEvent("scan:second_pass", map[string]any{
		"count":  len(candidates),
		"action": "Running real protocol verification...",
	})
	a.deepTest(candidates)
	a.secondPass = false
}

func (a *App) quickScan(ports []int) []ScanResult {
	timeout := 2 * time.Second
	maxConcurrent := 800

	workers := make(chan struct{}, maxConcurrent)
	var mu sync.Mutex
	var wg sync.WaitGroup
	candidates := make([]ScanResult, 0)

	ips := a.scanIPs
	offset := uint32(0)
	if len(ips) > 1 {
		n, err := rand.Int(rand.Reader, big.NewInt(int64(len(ips))))
		if err == nil {
			offset = uint32(n.Int64())
		}
	}

	for i := 0; i < len(ips); i++ {
		select {
		case <-a.stopScan:
			return candidates
		default:
		}

		ip := ips[(int(offset)+i)%len(ips)]

		for _, port := range ports {
			select {
			case <-a.stopScan:
				return candidates
			default:
			}

			a.currentIP = ip
			a.currentPort = port
			atomic.AddInt64(&a.totalScanned, 1)

			select {
			case <-a.stopScan:
				return candidates
			case workers <- struct{}{}:
			}

			wg.Add(1)
			go func(ip string, port int) {
				defer func() {
					<-workers
					wg.Done()
				}()

				start := time.Now()
				if tryConnect(ip, port, timeout) {
					latency := time.Since(start).Milliseconds()
					result := ScanResult{
						IP:      ip,
						Port:    port,
						Link:    fmt.Sprintf("%s:%d#Nova-%s", ip, port, generateNovaID()),
						Latency: latency,
					}
					mu.Lock()
					candidates = append(candidates, result)
					mu.Unlock()
					atomic.AddInt32(&a.aliveCount, 1)
				} else {
					atomic.AddInt32(&a.deadCount, 1)
				}
				a.emitEvent("scan:progress", a.GetScanStats())
			}(ip, port)
		}
	}

	done := make(chan struct{})
	go func() {
		wg.Wait()
		close(done)
	}()
	select {
	case <-done:
	case <-a.stopScan:
	}
	return candidates
}

func (a *App) deepTest(candidates []ScanResult) {
	maxConcurrent := 100
	workers := make(chan struct{}, maxConcurrent)
	var mu sync.Mutex
	var wg sync.WaitGroup

	type verified struct {
		result  ScanResult
		success int
		latency int64
	}

	vlist := make([]verified, len(candidates))
	for i, c := range candidates {
		vlist[i] = verified{result: c}
	}

	for idx := range vlist {
		select {
		case <-a.stopScan:
			goto finish
		default:
		}

		a.currentIP = vlist[idx].result.IP
		a.currentPort = vlist[idx].result.Port

		for attempt := 0; attempt < 3; attempt++ {
			select {
			case <-a.stopScan:
				goto finish
			default:
			}

			atomic.AddInt64(&a.totalScanned, 1)
			workers <- struct{}{}
			wg.Add(1)

			go func(idx, attempt int) {
				defer func() {
					<-workers
					wg.Done()
				}()

				start := time.Now()
				ok := deepTestConnect(
					vlist[idx].result.IP,
					vlist[idx].result.Port,
				)
				lat := time.Since(start).Milliseconds()

				mu.Lock()
				if ok {
					vlist[idx].success++
					if vlist[idx].latency == 0 || lat < vlist[idx].latency {
						vlist[idx].latency = lat
					}
				} else {
					atomic.AddInt32(&a.deadCount, 1)
				}
				mu.Unlock()
				a.emitEvent("scan:progress", a.GetScanStats())
			}(idx, attempt)
		}
	}

finish:
	done := make(chan struct{})
	go func() {
		wg.Wait()
		close(done)
	}()
	select {
	case <-done:
	case <-a.stopScan:
	}

	newAlive := int32(0)
	a.resultsMu.Lock()
	a.results = nil
	for _, v := range vlist {
		if v.success >= 2 {
			v.result.Latency = v.latency
			v.result.Link = fmt.Sprintf("%s:%d#Nova-%s", v.result.IP, v.result.Port, generateNovaID())
			a.results = append(a.results, v.result)
			newAlive++
			a.emitEvent("scan:result", v.result)
		}
	}
	sort.Slice(a.results, func(i, j int) bool {
		return a.results[i].Latency < a.results[j].Latency
	})
	a.resultsMu.Unlock()
	atomic.StoreInt32(&a.aliveCount, newAlive)
}

func deepTestConnect(ip string, port int) bool {
	addr := net.JoinHostPort(ip, strconv.Itoa(port))
	timeout := 3 * time.Second

	if tlsPorts[port] {
		conn, err := net.DialTimeout("tcp", addr, timeout)
		if err != nil {
			return false
		}
		tlsConn := tls.Client(conn, &tls.Config{
			ServerName:         vlessSNI,
			InsecureSkipVerify: true,
			MinVersion:         tls.VersionTLS12,
		})
		tlsConn.SetDeadline(time.Now().Add(timeout))
		if err := tlsConn.Handshake(); err != nil {
			conn.Close()
			return false
		}
		tlsConn.Close()
		return true
	}

	conn, err := net.DialTimeout("tcp", addr, timeout)
	if err != nil {
		return false
	}
	conn.SetDeadline(time.Now().Add(timeout))

	buf := make([]byte, 1)
	_, err = io.ReadFull(conn, buf)
	conn.Close()
	return err == nil
}

func tryConnect(ip string, port int, timeout time.Duration) bool {
	conn, err := net.DialTimeout("tcp", net.JoinHostPort(ip, strconv.Itoa(port)), timeout)
	if err != nil {
		return false
	}
	conn.Close()
	return true
}

func parseCIDR(cidr string) (uint32, uint32, error) {
	_, ipnet, err := net.ParseCIDR(cidr)
	if err != nil {
		return 0, 0, err
	}
	ones, bits := ipnet.Mask.Size()
	start := ipToUint32(ipnet.IP)
	count := uint32(1) << (bits - ones)
	end := start + count - 1
	return start, end, nil
}

func ipToUint32(ip net.IP) uint32 {
	ip = ip.To4()
	if ip == nil {
		return 0
	}
	return binary.BigEndian.Uint32(ip)
}

func uint32ToIP(n uint32) net.IP {
	ip := make(net.IP, 4)
	binary.BigEndian.PutUint32(ip, n)
	return ip
}

func generateNovaID() string {
	const chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
	b := make([]byte, 5)
	for i := range b {
		n, err := rand.Int(rand.Reader, big.NewInt(int64(len(chars))))
		if err != nil {
			b[i] = 'A'
			continue
		}
		b[i] = chars[n.Int64()]
	}
	return string(b)
}

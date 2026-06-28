package main

import (
	"crypto/rand"
	"io"
	"math/big"
	"net"
	"net/http"
	"strings"
	"sync"
)

type SourceType string

const (
	SourceCIDR    SourceType = "cidr"
	SourceProxyIP SourceType = "proxyip"
	SourceDomain  SourceType = "domain"
)

type IPSource struct {
	ID      string     `json:"id"`
	Name    string     `json:"name"`
	URL     string     `json:"url"`
	Type    SourceType `json:"type"`
	Enabled bool       `json:"enabled"`
}

var DefaultSources = []IPSource{
	{ID: "official", Name: "Cloudflare Official", URL: "https://www.cloudflare.com/ips-v4/", Type: SourceCIDR, Enabled: true},
	{ID: "cm", Name: "CM List", URL: "https://raw.githubusercontent.com/cmliu/cmliu/main/CF-CIDR.txt", Type: SourceCIDR, Enabled: false},
	{ID: "as13335", Name: "AS13335 (Cloudflare)", URL: "https://raw.githubusercontent.com/ipverse/asn-ip/master/as/13335/ipv4-aggregated.txt", Type: SourceCIDR, Enabled: false},
	{ID: "as209242", Name: "AS209242 (Cloudflare)", URL: "https://raw.githubusercontent.com/ipverse/asn-ip/master/as/209242/ipv4-aggregated.txt", Type: SourceCIDR, Enabled: false},
	{ID: "as24429", Name: "AS24429 (Alibaba)", URL: "https://raw.githubusercontent.com/ipverse/asn-ip/master/as/24429/ipv4-aggregated.txt", Type: SourceCIDR, Enabled: false},
	{ID: "as199524", Name: "AS199524 (G-Core)", URL: "https://raw.githubusercontent.com/ipverse/asn-ip/master/as/199524/ipv4-aggregated.txt", Type: SourceCIDR, Enabled: false},
	{ID: "proxyip", Name: "Reverse Proxy IPs", URL: "https://raw.githubusercontent.com/cmliu/ACL4SSR/main/baipiao.txt", Type: SourceProxyIP, Enabled: false},
	{ID: "dominos", Name: "Foreign Domains", URL: "https://raw.githubusercontent.com/Blacknuno/Nova-Proxy/refs/heads/main/dominos.text", Type: SourceDomain, Enabled: false},
	{ID: "irdominos", Name: "Iranian Domains", URL: "https://raw.githubusercontent.com/Blacknuno/Nova-Proxy/refs/heads/main/IRdominos.text", Type: SourceDomain, Enabled: false},
}

var fallbackCIDRs = []string{
	"173.245.48.0/20", "103.21.244.0/22", "103.22.200.0/22", "103.31.4.0/22",
	"141.101.64.0/18", "108.162.192.0/18", "190.93.240.0/20", "188.114.96.0/20",
	"197.234.240.0/22", "198.41.128.0/17", "162.158.0.0/15", "104.16.0.0/13",
	"104.24.0.0/14", "172.64.0.0/13", "131.0.72.0/22",
}

func fetchIPsFromSources(sources []IPSource) (cidrs []string, directIPs []string) {
	var mu sync.Mutex
	var wg sync.WaitGroup

	for _, src := range sources {
		if !src.Enabled {
			continue
		}
		wg.Add(1)
		go func(s IPSource) {
			defer wg.Done()
			switch s.Type {
			case SourceCIDR:
				lines, err := fetchURL(s.URL)
				if err != nil {
					return
				}
				parsed := parseCIDRLines(lines)
				mu.Lock()
				cidrs = append(cidrs, parsed...)
				mu.Unlock()
			case SourceProxyIP:
				lines, err := fetchURL(s.URL)
				if err != nil {
					return
				}
				ips := parseProxyIPLines(lines)
				mu.Lock()
				directIPs = append(directIPs, ips...)
				mu.Unlock()
			case SourceDomain:
				lines, err := fetchURL(s.URL)
				if err != nil {
					return
				}
				domains := parseDomainLines(lines)
				ips := resolveDomains(domains)
				mu.Lock()
				directIPs = append(directIPs, ips...)
				mu.Unlock()
			}
		}(src)
	}
	wg.Wait()

	if len(cidrs) == 0 {
		cidrs = fallbackCIDRs
	}
	return
}

func generateRandomIPs(cidrs []string, count int) []string {
	ips := make(map[string]struct{})
	var mu sync.Mutex
	var wg sync.WaitGroup
	sem := make(chan struct{}, 20)

	for _, cidr := range cidrs {
		wg.Add(1)
		sem <- struct{}{}
		go func(c string) {
			defer func() { <-sem; wg.Done() }()
			start, end, err := parseCIDR(c)
			if err != nil {
				return
			}
			total := end - start
			if total < 2 {
				return
			}

			perCIDR := (count / len(cidrs)) + 1
			for j := 0; j < perCIDR; j++ {
				offset := uint32(0)
				if total > 1 {
					n, err := rand.Int(rand.Reader, big.NewInt(int64(total)))
					if err == nil {
						offset = uint32(n.Int64())
					}
				}
				ip := uint32ToIP(start + 1 + offset%total).String()
				mu.Lock()
				if len(ips) < count {
					ips[ip] = struct{}{}
				}
				mu.Unlock()
			}
		}(cidr)
	}
	wg.Wait()

	result := make([]string, 0, len(ips))
	for ip := range ips {
		result = append(result, ip)
	}
	return result
}

func fetchURL(url string) (string, error) {
	client := &http.Client{Transport: &http.Transport{
		DisableKeepAlives: true,
	}}
	resp, err := client.Get(url)
	if err != nil {
		return "", err
	}
	defer resp.Body.Close()
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return "", err
	}
	return string(body), nil
}

func parseCIDRLines(text string) []string {
	var cidrs []string
	for _, line := range strings.Split(text, "\n") {
		line = strings.TrimSpace(line)
		if line == "" || strings.HasPrefix(line, "#") {
			continue
		}
		if _, _, err := net.ParseCIDR(line); err == nil {
			cidrs = append(cidrs, line)
		}
	}
	return cidrs
}

func parseProxyIPLines(text string) []string {
	seen := make(map[string]struct{})
	var ips []string
	for _, line := range strings.Split(text, "\n") {
		line = strings.TrimSpace(line)
		if line == "" || strings.HasPrefix(line, "#") {
			continue
		}
		if strings.Contains(line, "#") {
			line = strings.Split(line, "#")[0]
			line = strings.TrimSpace(line)
		}
		var ip string
		if strings.Contains(line, ":") {
			parts := strings.Split(line, ":")
			ip = strings.TrimSpace(parts[0])
		} else {
			ip = line
		}
		if net.ParseIP(ip) != nil {
			if _, exists := seen[ip]; !exists {
				seen[ip] = struct{}{}
				ips = append(ips, ip)
			}
		}
	}
	return ips
}

func parseDomainLines(text string) []string {
	var domains []string
	for _, line := range strings.Split(text, "\n") {
		line = strings.TrimSpace(line)
		if line == "" || strings.HasPrefix(line, "#") {
			continue
		}
		if strings.Contains(line, "#") {
			line = strings.Split(line, "#")[0]
			line = strings.TrimSpace(line)
		}
		if line != "" {
			domains = append(domains, line)
		}
	}
	return domains
}

func resolveDomains(domains []string) []string {
	var ips []string
	var mu sync.Mutex
	var wg sync.WaitGroup
	sem := make(chan struct{}, 10)

	limit := 100
	if len(domains) < limit {
		limit = len(domains)
	}

	for _, domain := range domains[:limit] {
		wg.Add(1)
		sem <- struct{}{}
		go func(d string) {
			defer func() { <-sem; wg.Done() }()
			addrs, err := net.LookupHost(d)
			if err != nil {
				return
			}
			mu.Lock()
			for _, addr := range addrs {
				if net.ParseIP(addr) != nil && strings.Count(addr, ":") == 0 {
					ips = append(ips, addr)
				}
			}
			mu.Unlock()
		}(domain)
	}
	wg.Wait()
	return ips
}

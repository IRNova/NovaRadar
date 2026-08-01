package main

import (
	"os"
	"os/exec"
	"sync"
	"sync/atomic"

	"github.com/wailsapp/wails/v3/pkg/application"
)

type App struct {
	wailsApp   *application.App
	mainWindow *application.WebviewWindow
	shouldQuit atomic.Bool

	scanning     atomic.Bool
	stopScan     chan struct{}
	results      []ScanResult
	resultsMu    sync.Mutex
	totalScanned int64
	totalToScan  int64
	aliveCount   int32
	deadCount    int32
	scanStartTime int64
	currentIP    string
	currentPort  int
	secondPass   bool
	scanIPs      []string

	sources   []IPSource
	sourcesMu sync.Mutex
}

func NewApp() *App {
	srcs := make([]IPSource, len(DefaultSources))
	copy(srcs, DefaultSources)
	return &App{
		sources: srcs,
	}
}

func (a *App) emitEvent(event string, data any) {
	if a.wailsApp != nil {
		a.wailsApp.Event.Emit(event, data)
	}
}

func (a *App) GetIPSources() []IPSource {
	a.sourcesMu.Lock()
	defer a.sourcesMu.Unlock()
	srcs := make([]IPSource, len(a.sources))
	copy(srcs, a.sources)
	return srcs
}

func (a *App) SetIPSources(sources []IPSource) {
	a.sourcesMu.Lock()
	a.sources = make([]IPSource, len(sources))
	copy(a.sources, sources)
	a.sourcesMu.Unlock()
}

func (a *App) ResetDefaultSources() {
	a.sourcesMu.Lock()
	a.sources = make([]IPSource, len(DefaultSources))
	copy(a.sources, DefaultSources)
	a.sourcesMu.Unlock()
}

func (a *App) OpenURL(url string) {
	exec.Command("rundll32", "url.dll,FileProtocolHandler", url).Start()
}

func (a *App) RevealMainWindow() {
	if a.mainWindow != nil {
		if a.mainWindow.IsMinimised() {
			a.mainWindow.UnMinimise()
		}
		a.mainWindow.Show()
	}
}

func (a *App) QuitApp() {
	a.shouldQuit.Store(true)
	if a.scanning.Load() {
		a.StopScan()
	}
	if a.mainWindow != nil {
		a.mainWindow.Close()
	}
}

func (a *App) HandleWindowClose() {
	a.QuitApp()
}

func (a *App) WindowMinimise() {
	if a.mainWindow != nil {
		a.mainWindow.Minimise()
	}
}

func (a *App) WindowToggleMaximise() {
	if a.mainWindow != nil {
		if a.mainWindow.IsMaximised() {
			a.mainWindow.UnMaximise()
		} else {
			a.mainWindow.Maximise()
		}
	}
}

func (a *App) ShouldStartHidden() bool {
	return false
}

func (a *App) GetCloseToTray() bool {
	return false
}

func hasLaunchArg(flag string) bool {
	for _, arg := range os.Args[1:] {
		if arg == flag {
			return true
		}
	}
	return false
}

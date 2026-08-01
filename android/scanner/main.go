package main

import (
	"embed"
	"log"

	"github.com/wailsapp/wails/v3/pkg/application"
	"github.com/wailsapp/wails/v3/pkg/events"
)

//go:embed all:frontend/dist
var assets embed.FS

//go:embed build/appicon.png
var trayIcon []byte

func main() {
	app := NewApp()

	wailsApp := application.New(application.Options{
		Name:        "NovaRadar",
		Description: "NovaRadar - Cloudflare IP Scanner",
		Assets: application.AssetOptions{
			Handler: application.BundledAssetFileServer(assets),
		},
		Services: []application.Service{
			application.NewService(app),
		},
		Icon: trayIcon,
	})

	app.wailsApp = wailsApp

	tray := wailsApp.SystemTray.New()
	tray.SetIcon(trayIcon)
	tray.SetDarkModeIcon(trayIcon)
	tray.SetTooltip("NovaRadar")
	trayMenu := application.NewMenu()
	trayMenu.Add("Show").OnClick(func(ctx *application.Context) {
		app.RevealMainWindow()
	})
	trayMenu.AddSeparator()
	trayMenu.Add("Quit").OnClick(func(ctx *application.Context) {
		app.QuitApp()
	})
	tray.SetMenu(trayMenu)

	app.mainWindow = wailsApp.Window.NewWithOptions(application.WebviewWindowOptions{
		Name:             "main",
		Title:            "NovaRadar - Cloudflare IP Scanner",
		Width:            900,
		Height:           700,
		URL:              "/",
		Frameless:        true,
		BackgroundColour: application.NewRGB(6, 9, 15),
	})
	app.mainWindow.OnWindowEvent(events.Common.WindowClosing, func(event *application.WindowEvent) {
		app.QuitApp()
	})
	tray.OnClick(func() {
		app.RevealMainWindow()
	})

	err := wailsApp.Run()
	if err != nil {
		log.Fatal(err)
	}
}

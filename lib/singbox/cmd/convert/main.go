// Command convert prints the sing-box options that a Clash profile converts to.
//
// The Android core converts internally, so this exists for diagnosis: warnings go to stderr and the
// JSON document to stdout, which makes it trivial to diff against a hand-written sing-box config or
// pipe into cmd/check.
//
//	go run ./cmd/convert --gateway 172.19.0.1/30 profile.yaml > profile.json
//	go run ./cmd/check profile.json
package main

import (
	"encoding/json"
	"flag"
	"fmt"
	"os"

	"github.com/yumeyucca/yumebox/singbox/internal/clash"
)

func main() {
	gateway := flag.String("gateway", "", "tun gateway prefix; empty means the config is not a VPN run")
	tunName := flag.String("tun-name", "", "tun interface name")
	modeSwitching := flag.Bool("mode-switching", true, "emit the clash_mode rules the app needs for Global/Direct")
	flag.Parse()

	if flag.NArg() != 1 {
		fmt.Fprintln(os.Stderr, "usage: convert [flags] <clash-profile>")
		os.Exit(2)
	}
	content, err := os.ReadFile(flag.Arg(0))
	if err != nil {
		fmt.Fprintf(os.Stderr, "read profile: %v\n", err)
		os.Exit(1)
	}

	result, err := clash.Convert(content, clash.Options{
		Gateway:             *gateway,
		TunName:             *tunName,
		EnableModeSwitching: *modeSwitching,
	})
	if err != nil {
		fmt.Fprintf(os.Stderr, "convert: %v\n", err)
		os.Exit(1)
	}
	for _, warning := range result.Warnings {
		fmt.Fprintf(os.Stderr, "warning: %s\n", warning)
	}
	encoder := json.NewEncoder(os.Stdout)
	encoder.SetIndent("", "  ")
	if err := encoder.Encode(result.Options); err != nil {
		fmt.Fprintf(os.Stderr, "encode: %v\n", err)
		os.Exit(1)
	}
}

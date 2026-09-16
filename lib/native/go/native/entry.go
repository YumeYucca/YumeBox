// This file is part of YumeBox.
//
// YumeBox is free software: you can redistribute it and/or modify it under the
// terms of the GNU Affero General Public License as published by the Free
// Software Foundation, either version 3 of the License.
//
// Copyright (c) YumeYucca 2025 - Present

// Package main is built as libmihomocore.so. A tiny PIE launcher dlopens the library and calls
// MihomoMain so the heavyweight Go payload can stay XZ-compressed in the APK.
package main

/*
#include <stdint.h>
*/
import "C"

import (
	"flag"
	"fmt"
	"os"
	"unsafe"

	"github.com/metacubex/mihomo/config"
	"github.com/metacubex/mihomo/constant"
	"github.com/metacubex/mihomo/log"
)

type options struct {
	home       string
	controller string
	gateway    string
	dns        string
	mode       string
	configPath string
	channel    string
	stack      string
	sdk        int
	test       bool
}

func parseOptions(args []string) options {
	var o options
	flags := flag.NewFlagSet("mihomo", flag.ContinueOnError)
	flags.SetOutput(os.Stderr)

	flags.StringVar(&o.home, "home", "", "core home directory")
	flags.StringVar(&o.controller, "controller", "", "external-controller-unix socket path")
	flags.StringVar(&o.gateway, "gateway", "", "tun gateway CIDR(s)")
	flags.StringVar(&o.dns, "dns", "", "tun DNS hijack address(es)")
	flags.StringVar(&o.mode, "mode", "vpn", "run mode: vpn | tun | ebpf | preview")
	flags.StringVar(&o.stack, "stack", "gvisor", "vpn tun stack: gvisor | mips")
	flags.StringVar(&o.configPath, "config", "", "compiled config path; root modes read the config here instead of the channel")
	flags.IntVar(&o.sdk, "sdk", 0, "android platform SDK int")
	flags.BoolVar(&o.test, "test", false, "parse config and exit (mihomo -t equivalent); requires --config")
	if err := flags.Parse(args[1:]); err != nil {
		fatal("parse arguments: %v", err)
	}

	return o
}

func runMain(args []string, channel string) int {
	opts := parseOptions(args)
	opts.channel = channel

	// SetHomeDir must precede config.Parse: geo paths resolve through constant.Path.
	log.SetLevel(log.ERROR)
	if opts.home != "" {
		constant.SetHomeDir(opts.home)
	}

	if opts.test {
		testConfig(opts)
		return 0
	}
	run(opts)
	return 0
}

// main is intentionally empty: -buildmode=c-shared initializes the Go runtime at dlopen time,
// then the PIE launcher enters through MihomoMain.
func main() {}

//export MihomoMain
func MihomoMain(argc C.int, argv **C.char, channel *C.char) C.int {
	count := int(argc)
	if count <= 0 || argv == nil {
		fatal("launcher supplied an empty argument vector")
	}
	cArgs := unsafe.Slice(argv, count)
	args := make([]string, count)
	for index, value := range cArgs {
		args[index] = C.GoString(value)
	}
	channelValue := ""
	if channel != nil {
		channelValue = C.GoString(channel)
	}
	return C.int(runMain(args, channelValue))
}

// testConfig implements --test: parse only. Wording and exit codes are a contract with
// ProfileConfigTester.kt.
func testConfig(opts options) {
	if opts.configPath == "" {
		fatal("--test requires --config")
	}
	if opts.home == "" {
		// Without a home the geo databases resolve to a cwd path where nothing exists, and a
		// valid config is reported as failing.
		fatal("--test requires --home")
	}

	data, err := os.ReadFile(opts.configPath)
	if err != nil {
		fatal("read config %q: %v", opts.configPath, err)
	}
	if _, err := config.Parse(data); err != nil {
		fatal("configuration file %s test failed: %v", opts.configPath, err)
	}

	fmt.Fprintln(os.Stdout, "configuration file", opts.configPath, "test is successful")
}

func fatal(format string, args ...any) {
	fmt.Fprintf(os.Stderr, "mihomo: "+format+"\n", args...)
	os.Exit(1)
}

package core

import (
	"fmt"
	"net"

	"github.com/sagernet/sing-box/option"
)

// applyRuntime encodes what the app's process model requires from the core, independent of what
// the profile asked for. This replaces lib/native/rust/src/compiler/patch/runtime.rs, which did
// the same job for mihomo.
func applyRuntime(options *option.Options, opts Options) (string, error) {
	controllerAddr := opts.APIAddr
	if controllerAddr == "" {
		var err error
		controllerAddr, err = freeLoopbackAddr()
		if err != nil {
			return "", err
		}
	}

	if options.Experimental == nil {
		options.Experimental = &option.ExperimentalOptions{}
	}
	if options.Experimental.ClashAPI == nil {
		options.Experimental.ClashAPI = &option.ClashAPIOptions{}
	}
	// The app owns the controller endpoint: a profile-supplied listen address would either
	// collide with it or expose the API on the LAN.
	api := options.Experimental.ClashAPI
	api.ExternalController = controllerAddr
	if opts.Secret != "" {
		api.Secret = opts.Secret
	}

	if options.Log == nil {
		options.Log = &option.LogOptions{}
	}
	if options.Log.Level == "" {
		options.Log.Level = "info"
	}
	// The profile must not be able to point the core's log at a file the app cannot read.
	options.Log.Output = ""

	// A PlatformWriter forces the cache file on, but with an implicit path it would land in the
	// process cwd. Pin it to the home directory the launcher already cleaned, and keep fake-ip
	// mappings across restarts the way profile.store-fake-ip did for mihomo.
	if options.Experimental.CacheFile == nil {
		options.Experimental.CacheFile = &option.CacheFileOptions{}
	}
	cacheFile := options.Experimental.CacheFile
	cacheFile.Enabled = true
	if cacheFile.Path == "" {
		cacheFile.Path = "cache.db"
	}
	cacheFile.StoreFakeIP = true

	// mihomo accepted "mixed" and silently fell back to gvisor; sing-box only knows the two real
	// stacks, so the launcher flag wins over whatever the profile carried.
	if opts.Stack != "" {
		applyStack(options, opts.Stack)
	}
	return controllerAddr, nil
}

func applyStack(options *option.Options, stack string) {
	var value string
	switch stack {
	case "system":
		value = "system"
	default:
		value = "gvisor"
	}
	for index := range options.Inbounds {
		tunOptions, loaded := options.Inbounds[index].Options.(*option.TunInboundOptions)
		if !loaded {
			continue
		}
		tunOptions.Stack = value
	}
}

// freeLoopbackAddr reserves a loopback port for the clash API. The listener is closed right away;
// the core binds it moments later, and a collision only costs a failed start at boot time.
func freeLoopbackAddr() (string, error) {
	listener, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		return "", fmt.Errorf("reserve controller port: %w", err)
	}
	addr := listener.Addr().String()
	if err = listener.Close(); err != nil {
		return "", fmt.Errorf("release controller port: %w", err)
	}
	return addr, nil
}

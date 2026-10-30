//go:build !linux && !android

package core

import "fmt"

// The launcher channel is a Linux socketpair; every other platform only needs the compile-time
// shape so config checking stays usable on a developer desktop.
type launcherRPC struct{}

func newLauncherRPC(int) *launcherRPC { return nil }

func readSetup(int) ([]byte, int, error) {
	return nil, -1, fmt.Errorf("the launcher channel is only available on android")
}

func readPreviewSetup(int) ([]byte, error) {
	return nil, fmt.Errorf("the launcher channel is only available on android")
}

func socketOwnerQuery(*launcherRPC) ownerQuery { return nil }

func (rpc *launcherRPC) protect(int) error { return fmt.Errorf("unsupported platform") }

func closeFd(int) error { return nil }

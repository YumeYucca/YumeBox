// Command check validates a sing-box JSON config with the same code path the Android core uses.
// It is the desktop half of the P0 spike: it proves the option decoding and instance construction
// work before anything runs on a device, and later becomes the compiler's post-conversion check.
package main

import (
	"fmt"
	"os"

	"github.com/yumeyucca/yumebox/singbox/internal/core"
)

func main() {
	if len(os.Args) < 2 {
		fmt.Fprintln(os.Stderr, "usage: check [-home <dir>] <config.json>")
		os.Exit(2)
	}
	home := ""
	path := os.Args[1]
	if path == "-home" && len(os.Args) >= 4 {
		home = os.Args[2]
		path = os.Args[3]
	}
	content, err := os.ReadFile(path)
	if err != nil {
		fmt.Fprintf(os.Stderr, "read %s: %v\n", path, err)
		os.Exit(1)
	}
	if err = core.Check(home, content); err != nil {
		fmt.Fprintf(os.Stderr, "invalid config %s: %v\n", path, err)
		os.Exit(1)
	}
	fmt.Printf("%s is a valid sing-box config\n", path)
}

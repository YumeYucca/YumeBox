// Package main builds the YumeBox sing-box core as a c-shared library (libsingboxcore.so).
//
// This is the P0 spike of the mihomo -> sing-box migration: it keeps the launcher contract
// (socketpair carrying the compiled config plus the VpnService TUN fd, then owner/protect RPCs)
// and swaps the engine from mihomo to sing-box.
package main

// main is intentionally empty. With -buildmode=c-shared the Go runtime initializes when the
// library is dlopen'd; the PIE launcher enters through SingboxMain (see main_android.go).
func main() {}

package core

import (
	"fmt"
	"io"
	"net"
	"os"
)

// serveControllerSocket keeps the unix-socket contract the Android client relies on (see
// core/src/core/bridge/UnixSocket.kt): sing-box's clash API only listens on TCP, so the socket is
// ours and forwards byte-for-byte into the loopback listener.
func serveControllerSocket(path string, target string) error {
	_ = os.Remove(path)
	listener, err := net.Listen("unix", path)
	if err != nil {
		return fmt.Errorf("listen unix %q: %w", path, err)
	}
	// The controller secret is the only authentication; keep the socket owner-only.
	if err = os.Chmod(path, 0o600); err != nil {
		_ = listener.Close()
		return fmt.Errorf("chmod controller socket: %w", err)
	}
	for {
		client, acceptErr := listener.Accept()
		if acceptErr != nil {
			return fmt.Errorf("accept on controller socket: %w", acceptErr)
		}
		go forwardController(client, target)
	}
}

func forwardController(client net.Conn, target string) {
	defer client.Close()
	upstream, err := net.Dial("tcp", target)
	if err != nil {
		return
	}
	defer upstream.Close()

	done := make(chan struct{}, 2)
	go func() {
		_, _ = io.Copy(upstream, client)
		done <- struct{}{}
	}()
	go func() {
		_, _ = io.Copy(client, upstream)
		done <- struct{}{}
	}()
	<-done
}

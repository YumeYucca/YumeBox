//go:build linux || android

package core

import (
	"bytes"
	"fmt"
	"os"
	"strconv"
	"sync"
	"syscall"
)

// The client half of the launcher socketpair, wire-compatible with Runtime/service's
// CoreProcess.kt: config + TUN fd inbound at startup, then socket-owner and VpnService.protect
// RPCs for the rest of the session.
const (
	setupBufSize      = 64 * 1024
	ownerReplyBufSize = 4096
	protectRequest    = "protect"
	protectReply      = "protected"
)

// readSetup drains the config; the launcher terminates it with the TUN fd via SCM_RIGHTS.
func readSetup(channelFd int) ([]byte, int, error) {
	payload := make([]byte, 0, setupBufSize)
	buf := make([]byte, setupBufSize)
	oob := make([]byte, syscall.CmsgSpace(4))
	for {
		n, oobn, flags, _, err := syscall.Recvmsg(channelFd, buf, oob, 0)
		if err != nil {
			return nil, -1, err
		}
		// SEQPACKET discards what does not fit; MSG_CTRUNC means the TUN fd itself is gone.
		if flags&(syscall.MSG_TRUNC|syscall.MSG_CTRUNC) != 0 {
			return nil, -1, fmt.Errorf("setup datagram truncated (flags=%#x, %d of %d bytes)", flags, n, len(buf))
		}
		if fd := parseRightsFd(oob[:oobn]); fd >= 0 {
			return payload, fd, nil
		}
		if n == 0 {
			return nil, -1, fmt.Errorf("channel closed after %d config bytes, before the tun fd arrived", len(payload))
		}
		payload = append(payload, buf[:n]...)
	}
}

// readPreviewSetup receives only the compiled config: no SCM_RIGHTS handling, EOF terminates.
func readPreviewSetup(channelFd int) ([]byte, error) {
	payload := make([]byte, 0, setupBufSize)
	buf := make([]byte, setupBufSize)
	for {
		n, _, flags, _, err := syscall.Recvmsg(channelFd, buf, nil, 0)
		if err != nil {
			return nil, err
		}
		if flags&(syscall.MSG_TRUNC|syscall.MSG_CTRUNC) != 0 {
			return nil, fmt.Errorf("preview setup datagram truncated (flags=%#x, n=%d)", flags, n)
		}
		if n == 0 {
			return payload, nil
		}
		payload = append(payload, buf[:n]...)
	}
}

type launcherRPC struct {
	fd      int
	mutex   sync.Mutex
	broken  bool
	logOnce sync.Once
	reply   []byte
}

func newLauncherRPC(fd int) *launcherRPC {
	if fd < 0 {
		return nil
	}
	return &launcherRPC{fd: fd, reply: make([]byte, ownerReplyBufSize)}
}

// request serializes every round trip so the reply always matches its request on SOCK_SEQPACKET.
func (rpc *launcherRPC) request(request []byte, attachedFd int) ([]byte, error) {
	if rpc == nil {
		return nil, fmt.Errorf("launcher RPC channel is unavailable")
	}
	rpc.mutex.Lock()
	defer rpc.mutex.Unlock()
	if rpc.broken {
		return nil, fmt.Errorf("launcher RPC channel is closed")
	}
	if err := sendRequest(rpc.fd, request, attachedFd); err != nil {
		return nil, err
	}
	for {
		n, _, _, _, err := syscall.Recvmsg(rpc.fd, rpc.reply, nil, 0)
		if err == syscall.EINTR {
			continue
		}
		if err != nil || n <= 0 {
			rpc.broken = true
			return nil, fmt.Errorf("launcher RPC read (err=%v n=%d)", err, n)
		}
		return append([]byte(nil), rpc.reply[:n]...), nil
	}
}

// socketOwnerQuery asks the launcher which app owns a connection. Wire format: request
// "<ipproto>\t<src>\t<dst>", reply "<uid>\t<package>", "-1\t" = unknown.
func socketOwnerQuery(rpc *launcherRPC) ownerQuery {
	if rpc == nil {
		return nil
	}

	report := func(err error) {
		rpc.logOnce.Do(func() {
			fmt.Fprintf(os.Stderr, "socket-owner RPC failed (err=%v); per-app rules fall back to /proc/net\n", err)
		})
	}

	return func(protocol int, source, target string) (int, string) {
		request := strconv.AppendInt(make([]byte, 0, 128), int64(protocol), 10)
		request = append(request, '\t')
		request = append(request, source...)
		request = append(request, '\t')
		request = append(request, target...)

		reply, err := rpc.request(request, -1)
		if err != nil {
			report(err)
			return -1, ""
		}
		return decodeSocketOwner(reply)
	}
}

// protect asks VpnService to exempt one core socket from the TUN before it connects.
func (rpc *launcherRPC) protect(fd int) error {
	reply, err := rpc.request([]byte(protectRequest), fd)
	if err != nil {
		return err
	}
	if string(reply) != protectReply {
		return fmt.Errorf("VpnService rejected socket")
	}
	return nil
}

func sendRequest(fd int, request []byte, attachedFd int) error {
	var oob []byte
	if attachedFd >= 0 {
		oob = syscall.UnixRights(attachedFd)
	}
	for {
		if err := syscall.Sendmsg(fd, request, oob, nil, 0); err != syscall.EINTR {
			return err
		}
	}
}

func decodeSocketOwner(reply []byte) (int, string) {
	uidPart, pkg, found := bytes.Cut(reply, []byte{'\t'})
	if !found {
		return -1, ""
	}
	uid, err := strconv.Atoi(string(uidPart))
	if err != nil {
		uid = -1
	}
	return uid, string(pkg)
}

func parseRightsFd(oob []byte) int {
	if len(oob) == 0 {
		return -1
	}
	messages, err := syscall.ParseSocketControlMessage(oob)
	if err != nil {
		return -1
	}
	for i := range messages {
		if fds, err := syscall.ParseUnixRights(&messages[i]); err == nil && len(fds) > 0 {
			return fds[0]
		}
	}
	return -1
}

// closeFd keeps the int-shaped fd arithmetic off callers: syscall.Close takes a Handle on Windows,
// where the launcher channel does not exist at all.
func closeFd(fd int) error {
	return syscall.Close(fd)
}

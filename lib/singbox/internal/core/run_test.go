package core

import (
	"io"
	"os"
	"path/filepath"
	"strings"
	"testing"
)

// The Android tester (ProfileConfigTester.kt) shells out with --test --home --config and parses the
// failure line for a "mihomo:" prefix plus the "test failed:" marker. These tests pin that contract
// so a refactor cannot silently turn a config error into an unreadable message.

func captureStderr(t *testing.T, run func()) string {
	t.Helper()
	reader, writer, err := os.Pipe()
	if err != nil {
		t.Fatalf("pipe: %v", err)
	}
	original := os.Stderr
	os.Stderr = writer
	collected := make(chan string, 1)
	go func() {
		data, _ := io.ReadAll(reader)
		collected <- string(data)
	}()
	run()
	os.Stderr = original
	_ = writer.Close()
	return <-collected
}

func withWorkdir(t *testing.T, dir string) {
	t.Helper()
	previous, err := os.Getwd()
	if err != nil {
		t.Fatalf("getwd: %v", err)
	}
	t.Cleanup(func() { _ = os.Chdir(previous) })
	if err = os.Chdir(dir); err != nil {
		t.Fatalf("chdir: %v", err)
	}
}

func TestRunTestReportsFailureInTheTesterFormat(t *testing.T) {
	dir := t.TempDir()
	withWorkdir(t, dir)
	profile := filepath.Join(dir, "profile.yaml")
	// An unsupported proxy type is an actionable, user-visible failure.
	if err := os.WriteFile(profile, []byte("proxies:\n  - { name: n, type: snell, server: s.example.com, port: 1 }\n"), 0o600); err != nil {
		t.Fatalf("write: %v", err)
	}
	var code int
	stderr := captureStderr(t, func() {
		code = runTest(Options{Home: dir, ConfigPath: profile})
	})
	if code == 0 {
		t.Fatal("a rejected profile must report a non-zero exit code")
	}
	if !strings.Contains(stderr, "mihomo:") {
		t.Fatalf("the tester matches on the mihomo: prefix, got: %q", stderr)
	}
	if !strings.Contains(stderr, "test failed:") {
		t.Fatalf("the tester cuts the message at test failed:, got: %q", stderr)
	}
	if !strings.Contains(stderr, "snell") {
		t.Fatalf("the cause must reach the user, got: %q", stderr)
	}
}

func TestRunTestAcceptsAValidProfile(t *testing.T) {
	dir := t.TempDir()
	withWorkdir(t, dir)
	profile := filepath.Join(dir, "profile.yaml")
	body := "proxies:\n  - { name: n, type: trojan, server: n.example.com, port: 443, password: p }\n" +
		"proxy-groups:\n  - { name: g, type: select, proxies: [n, DIRECT] }\n" +
		"rules:\n  - MATCH,g\n"
	if err := os.WriteFile(profile, []byte(body), 0o600); err != nil {
		t.Fatalf("write: %v", err)
	}
	var code int
	stderr := captureStderr(t, func() {
		code = runTest(Options{Home: dir, ConfigPath: profile})
	})
	if code != 0 {
		t.Fatalf("a valid profile must pass, got code %d: %s", code, stderr)
	}
}

func TestRunTestWithoutFlagsFailsFast(t *testing.T) {
	dir := t.TempDir()
	if code := runTest(Options{Home: dir}); code == 0 {
		t.Fatal("--test without --config must fail")
	}
	if code := runTest(Options{ConfigPath: "x"}); code == 0 {
		t.Fatal("--test without --home must fail")
	}
}

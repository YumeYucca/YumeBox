#!/usr/bin/env bash
# Builds the YumeBox sing-box core as a c-shared library for Android.
#
# This is the build half of the P0 spike: it proves the sing-box module compiles with cgo and the
# Android linker before any Kotlin changes land. The output keeps the launcher contract
# (SingboxMain, socketpair config + TUN fd), so a packed .so can be dropped behind the existing
# loader once the Kotlin side starts feeding JSON.
#
# Requires: Go >= 1.24, ANDROID_NDK_HOME (or --ndk), curl for nothing else.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
MODULE="$ROOT/lib/singbox"
OUT_DIR="${OUT_DIR:-$ROOT/build/native/singbox}"
ANDROID_API="${ANDROID_API:-26}"
# sing-box gates features behind build tags; without these the config decoder rejects the very
# inbounds/outbounds the app emits (clash_api, gvisor stack, QUIC transports, uTLS, WireGuard).
TAGS="${TAGS:-with_clash_api,with_gvisor,with_quic,with_utls,with_wireguard,with_acme}"

if [[ -z "${ANDROID_NDK_HOME:-}" ]]; then
  echo "ANDROID_NDK_HOME is not set" >&2
  exit 1
fi

HOST_TAG="linux-x86_64"
if [[ "$(uname -s)" == "Darwin" ]]; then
  HOST_TAG="darwin-x86_64"
fi
TOOLCHAIN="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/$HOST_TAG"

build_abi() {
  local abi="$1" goarch="$2" triple="$3"
  local cc="$TOOLCHAIN/bin/${triple}${ANDROID_API}-clang"
  if [[ ! -x "$cc" ]]; then
    echo "missing compiler: $cc" >&2
    exit 1
  fi
  local out="$OUT_DIR/$abi/libsingboxcore.so"
  mkdir -p "$(dirname "$out")"
  echo "[singbox] building $abi -> $out"
  (
    cd "$MODULE"
    CGO_ENABLED=1 \
    GOOS=android \
    GOARCH="$goarch" \
    CC="$cc" \
    go build \
      -buildmode=c-shared \
      -trimpath \
      -tags "$TAGS" \
      -ldflags "-s -w -extldflags=-Wl,--pack-dyn-relocs=android" \
      -o "$out" \
      .
  )
}

case "${1:-all}" in
  arm64) build_abi arm64-v8a arm64 aarch64-linux-android ;;
  arm)   build_abi armeabi-v7a arm armv7a-linux-androideabi ;;
  x86_64) build_abi x86_64 x86_64 x86_64-linux-android ;;
  all)
    build_abi arm64-v8a arm64 aarch64-linux-android
    ;;
  *)
    echo "usage: $0 [arm64|arm|x86_64|all]" >&2
    exit 2
    ;;
esac

echo "[singbox] done: $OUT_DIR"

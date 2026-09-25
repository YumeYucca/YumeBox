"""Command-line orchestration for native builds."""

from __future__ import annotations

import argparse
from pathlib import Path
import shutil

from .cmake_builder import compat_builder, loader_builder, shell_builder
from .command import command_exists
from .config import NdkTools, ProjectConfig, SystemDetector
from .geo import ResourceDownloader
from .go_core import GoCoreBuilder
from .rust_builder import RustBuilder


MESSAGE = r"""
__   __                             ____
\ \ / /  _   _   _ __ ___     ___  | __ )    ___   __  __
 \ V /  | | | | | '_ ` _ \   / _ \ |  _ \   / _ \  \ \/ /
  | |   | |_| | | | | | | | |  __/ | |_) | | (_) |  >  <
  |_|    \__,_| |_| |_| |_|  \___| |____/   \___/  /_/\_\
""".strip()


def parser() -> argparse.ArgumentParser:
    result = argparse.ArgumentParser(description="YumeBox Native Build Tool")
    result.add_argument("--go", action="store_true", help="Build the mihomo shared core and PIE shell")
    result.add_argument("--coreexe", action="store_true", help="Compatibility alias of --go")
    result.add_argument("--shell", action="store_true", help="Build only the mihomo PIE shells")
    result.add_argument("--rust", action="store_true", help="Build Rust config compiler")
    result.add_argument("--loader", action="store_true", help="Build the C/liblzma native payload extractor")
    result.add_argument("--compat", action="store_true", help="Build the out-of-process core bridge")
    result.add_argument("--geo", action="store_true", help="Download and compress Geo databases")
    result.add_argument("--clean", action="store_true", help="Clean build outputs")
    result.add_argument("--all", action="store_true", help="Build everything")
    return result


def clean_build_outputs(root: Path) -> None:
    print("[Clean] Removing build outputs...")
    for relative in ("build/native", "build/generated"):
        shutil.rmtree(root / relative, ignore_errors=True)
    for name in ("geoip.metadb.xz", "geosite.dat.xz", "ASN.mmdb.xz"):
        (root / "app/assets" / name).unlink(missing_ok=True)
    for abi in ("arm64-v8a",):
        for name in ("libmihomo.so", "libpreview.so", "libmihomocore.so", "liboverride.so", "libloader.so", "libcompat.so", "core-version.properties"):
            (root / "jniLibs" / abi / name).unlink(missing_ok=True)
    (root / "build/generated/core-version.properties").unlink(missing_ok=True)
    print("[Clean] Done")


def main(argv: list[str] | None = None) -> int:
    args = parser().parse_args(argv)
    root = Path(__file__).resolve().parents[2]
    if args.clean:
        clean_build_outputs(root)
        return 0

    print(MESSAGE)
    print("=== YumeBox Native Build Tool ===")
    print(f"OS: {SystemDetector.os_name()}, Host: {SystemDetector.host_tag()}")
    config = ProjectConfig(root)
    explicit = any((args.go, args.coreexe, args.shell, args.rust, args.loader, args.compat, args.geo, args.all))
    build_go = not explicit or args.all or args.go or args.coreexe
    build_shell = build_go or args.all or args.shell
    build_rust = not explicit or args.all or args.rust
    build_loader = not explicit or args.all or args.loader
    build_compat = not explicit or args.all or args.compat
    download_geo = not explicit or args.all or args.geo
    needs_ndk = build_go or build_shell or build_rust or build_loader or build_compat
    ndk_tools = NdkTools(config) if needs_ndk else None
    if ndk_tools:
        print(f"NDK: {ndk_tools.ndk_dir}")
    print(f"Go: {'OK' if command_exists('go') else 'NOT FOUND'}")
    print(f"Rust: {'OK' if command_exists('cargo') else 'NOT FOUND'}")
    print("XZ: Python standard library lzma")

    if build_go:
        GoCoreBuilder(config, ndk_tools).build_all()
    if build_shell:
        shell_builder(config, ndk_tools).build_all()
    if build_rust:
        RustBuilder(config, ndk_tools).build_all()
    if build_loader:
        loader_builder(config, ndk_tools).build_all()
    if build_compat:
        compat_builder(config, ndk_tools).build_all()
    if download_geo:
        ResourceDownloader(config).download_geo_files()
    print("=== Build Complete ===")
    return 0

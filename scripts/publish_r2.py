#!/usr/bin/env python3
"""Verify and stage an already-built YumeBox update manifest for R2 upload."""

from __future__ import annotations

import argparse
import hashlib
import json
import shlex
import sys
from pathlib import Path

CHANNELS = ("stable", "nightly")


def fail(message: str) -> None:
    print(f"error: {message}", file=sys.stderr)
    raise SystemExit(1)


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def main() -> int:
    parser = argparse.ArgumentParser(description="Stage a YumeBox update for R2")
    parser.add_argument("--channel", required=True, choices=CHANNELS)
    parser.add_argument("--meta-dir", required=True)
    parser.add_argument("--apk-dir", required=True)
    parser.add_argument("--prefix", default="releases")
    parser.add_argument("--output-dir", default="r2")
    parser.add_argument("--existing-latest", default="")
    args = parser.parse_args()

    meta_files = sorted(Path(args.meta_dir).glob("*.json"))
    if len(meta_files) != 1:
        fail(f"expected exactly one manifest in {args.meta_dir}, found {len(meta_files)}")
    manifest_path = meta_files[0].resolve()
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))

    if int(manifest.get("schemaVersion", 0)) != 1:
        fail(f"unsupported manifest schemaVersion: {manifest.get('schemaVersion')!r}")
    if manifest.get("channel") != args.channel:
        fail(f"manifest channel {manifest.get('channel')!r} != {args.channel!r}")

    version_code = int(manifest["versionCode"])
    version_name = str(manifest.get("versionName") or f"v{version_code}")
    commit_sha = str(manifest.get("commitSha") or "")
    packages = manifest.get("packages") or []
    if len(packages) != 1:
        fail(f"expected exactly one external package, found {len(packages)}")
    package = packages[0]
    if package.get("geoBundled") is not False:
        fail("update manifest must contain the external package")
    if package.get("abi") != "arm64-v8a":
        fail(f"unsupported package ABI: {package.get('abi')!r}")
    download_url = str(package.get("downloadUrl") or "")
    if not download_url.startswith("https://"):
        fail("manifest downloadUrl must be an https URL")

    file_name = str(package["fileName"])
    apk_path = (Path(args.apk_dir) / file_name).resolve()
    if not apk_path.is_file():
        fail(f"APK not found: {apk_path}")
    digest = sha256_file(apk_path)
    expected_digest = str(package.get("sha256") or "").lower()
    if expected_digest and digest.lower() != expected_digest:
        fail(f"sha256 mismatch for {file_name}: manifest={expected_digest} actual={digest}")

    prefix = args.prefix.strip("/")
    apk_key = f"{prefix}/{args.channel}/YumeBox-external.apk"
    manifest_key = f"{prefix}/{args.channel}/latest.json"

    publish_latest = True
    existing_path = Path(args.existing_latest) if args.existing_latest else None
    if existing_path and existing_path.is_file():
        try:
            existing = json.loads(existing_path.read_text(encoding="utf-8"))
            existing_code = int(existing.get("versionCode", -1))
            existing_sha = str(existing.get("commitSha") or "")
        except (ValueError, TypeError, json.JSONDecodeError):
            existing_code = -1
            existing_sha = ""
        # Different branches can produce the same commit count; let the most recent
        # build win in that case, but keep re-runs of the same commit idempotent.
        publish_latest = version_code > existing_code or (
            version_code == existing_code and commit_sha != existing_sha
        )

    output_dir = Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)
    values = {
        "APK_PATH": apk_path,
        "APK_KEY": apk_key,
        "MANIFEST_PATH": manifest_path,
        "MANIFEST_KEY": manifest_key,
        "VERSION_CODE": version_code,
        "VERSION_NAME": version_name,
        "PUBLISH_LATEST": str(publish_latest),
    }
    (output_dir / "publish.env").write_text(
        "".join(f"{key}={shlex.quote(str(value))}\n" for key, value in values.items()),
        encoding="utf-8",
    )

    print(json.dumps({
        "channel": args.channel,
        "versionName": version_name,
        "versionCode": version_code,
        "commitSha": commit_sha,
        "apkKey": apk_key,
        "manifestKey": manifest_key,
        "publishLatest": publish_latest,
    }, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

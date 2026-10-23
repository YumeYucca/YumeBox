"""Project configuration and Android toolchain discovery."""

from __future__ import annotations

import os
from pathlib import Path
import platform
import shutil

from .command import command_exists

SUPPORTED_ANDROID_ABI = "arm64-v8a"


class ProjectConfig:
    def __init__(self, project_root: Path):
        self.project_root = project_root
        self.properties: dict[str, str] = {}
        for name in ("kernel.properties", "product.properties", "local.properties"):
            self._load_properties(project_root / name)

    def _load_properties(self, path: Path) -> None:
        if not path.is_file():
            return
        for raw_line in path.read_text(encoding="utf-8").splitlines():
            line = raw_line.strip()
            if not line or line.startswith(("#", "!")):
                continue
            separator = next((index for index, char in enumerate(line) if char in "=:\t"), -1)
            if separator < 0:
                self.properties[line] = ""
                continue
            key = line[:separator].strip()
            value = self._unescape_property_value(line[separator + 1 :].strip())
            self.properties[key] = value

    @staticmethod
    def _unescape_property_value(value: str) -> str:
        """Decode the escapes used by Java/Gradle properties files."""
        decoded: list[str] = []
        index = 0
        escapes = {"t": "\t", "n": "\n", "r": "\r", "f": "\f"}
        while index < len(value):
            if value[index] != "\\":
                decoded.append(value[index])
                index += 1
                continue

            index += 1
            if index >= len(value):
                decoded.append("\\")
                break
            escaped = value[index]
            if escaped == "u" and index + 4 < len(value):
                codepoint = value[index + 1 : index + 5]
                try:
                    decoded.append(chr(int(codepoint, 16)))
                    index += 5
                    continue
                except ValueError:
                    pass
            decoded.append(escapes.get(escaped, escaped))
            index += 1
        return "".join(decoded)

    def get_string(self, key: str, default: str = "") -> str:
        env_key = key.replace(".", "_").upper()
        return os.environ.get(env_key, self.properties.get(key, default))

    def get_int(self, key: str, default: int) -> int:
        try:
            return int(self.get_string(key, str(default)))
        except ValueError:
            return default

    def get_bool(self, key: str, default: bool) -> bool:
        value = self.get_string(key, str(default).lower()).lower()
        return {"true": True, "false": False}.get(value, default)

    def get_csv(self, key: str, default: str = "") -> list[str]:
        return [item.strip() for item in self.get_string(key, default).split(",") if item.strip()]


class SystemDetector:
    @staticmethod
    def os_name() -> str:
        system = platform.system().lower()
        if "windows" in system:
            return "windows"
        if "darwin" in system or "mac" in system:
            return "darwin"
        if "linux" in system:
            return "linux"
        return "unknown"

    @classmethod
    def host_tag(cls) -> str:
        system = cls.os_name()
        if system == "windows":
            return "windows-x86_64"
        if system == "darwin":
            machine = platform.machine().lower()
            return "darwin-arm64" if "aarch64" in machine or "arm64" in machine else "darwin-x86_64"
        return "linux-x86_64"

    @staticmethod
    def check_command_exists(command: str) -> bool:
        return command_exists(command)


class NdkTools:
    def __init__(self, config: ProjectConfig):
        self.config = config
        self.sdk_dir = self._find_sdk_dir()
        self.ndk_dir = self._find_ndk_dir()

    def _find_sdk_dir(self) -> Path:
        configured = self.config.get_string("sdk.dir")
        path = configured or os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
        if not path:
            raise RuntimeError("Android SDK not found. Please configure sdk.dir or ANDROID_HOME.")
        sdk_dir = Path(path).expanduser().resolve()
        if not sdk_dir.is_dir():
            raise RuntimeError(f"Android SDK not found: {sdk_dir}")
        return sdk_dir

    def _find_ndk_dir(self) -> Path:
        configured = self.config.get_string("ndk.dir")
        version = self.config.get_string("android.ndkVersion")
        ndk_dir = Path(configured) if configured else self.sdk_dir / "ndk" / version
        ndk_dir = ndk_dir.expanduser().resolve()
        if not ndk_dir.is_dir():
            raise RuntimeError(f"NDK not found: {ndk_dir}")
        source = ndk_dir / "source.properties"
        actual = ""
        if source.is_file():
            for line in source.read_text(encoding="utf-8").splitlines():
                key, _, value = line.partition("=")
                if key.strip() == "Pkg.Revision":
                    actual = value.strip()
                    break
        if version and actual != version:
            raise RuntimeError(f"NDK version mismatch: requested {version}, found {actual or 'unknown'} at {ndk_dir}")
        return ndk_dir

    def _tool(self, relative_path: str) -> Path:
        path = self.ndk_dir / "toolchains" / "llvm" / "prebuilt" / SystemDetector.host_tag() / "bin" / relative_path
        if not path.is_file():
            raise RuntimeError(f"NDK tool not found: {path}")
        return path

    def get_clang_path(self, abi: str) -> Path:
        if abi != SUPPORTED_ANDROID_ABI:
            raise ValueError(f"Unsupported ABI: {abi}")
        extension = ".cmd" if SystemDetector.os_name() == "windows" else ""
        return self._tool(f"aarch64-linux-android{self.get_min_android_api()}-clang{extension}")

    def get_strip_path(self) -> Path:
        extension = ".exe" if SystemDetector.os_name() == "windows" else ""
        return self._tool(f"llvm-strip{extension}")

    def get_min_android_api(self) -> int:
        return max(self.config.get_int("android.minSdk", 24), 24)

    def _cmake_tool(self, name: str) -> Path:
        extension = ".exe" if SystemDetector.os_name() == "windows" else ""
        cmake_root = self.sdk_dir / "cmake"
        if cmake_root.is_dir():
            preferred = cmake_root / "3.22.1" / "bin" / f"{name}{extension}"
            if preferred.is_file():
                return preferred
            versions = [path for path in cmake_root.iterdir() if path.is_dir()]
            compatible = [path for path in versions if path.name.startswith("3.")]
            candidates = sorted(
                (path / "bin" / f"{name}{extension}" for path in (compatible or versions)),
                key=lambda path: path.parent.parent.name,
                reverse=True,
            )
            for candidate in candidates:
                if candidate.is_file():
                    return candidate
        host_tool = shutil.which(name)
        if host_tool:
            return Path(host_tool)
        raise RuntimeError(f"{name} executable not found under {cmake_root} or PATH")

    def get_cmake_path(self) -> Path:
        return self._cmake_tool("cmake")

    def get_ninja_path(self) -> Path:
        return self._cmake_tool("ninja")


def configured_abis(config: ProjectConfig) -> list[str]:
    abis = config.get_csv("abi.app.list", SUPPORTED_ANDROID_ABI)
    if abis != [SUPPORTED_ANDROID_ABI]:
        raise RuntimeError(f"Only {SUPPORTED_ANDROID_ABI} is supported; configured ABIs: {', '.join(abis)}")
    return abis

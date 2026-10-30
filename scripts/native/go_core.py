"""Go shared-core builder, for either the mihomo or the sing-box engine.

Which engine a build produces is a one-line switch: ``golang.core`` in kernel.properties.
Both land in the same file name so the loader, the kernel index and KernelManager stay untouched;
the loader resolves either entry symbol.
"""

from __future__ import annotations

from .command import command_failure, execute_command
from .config import NdkTools, ProjectConfig, configured_abis
from .version import CoreVersionStamp, resolve_core_version_stamp, write_core_version_stamp


class GoCoreBuilder:
    ABI_TO_GO_ARCH = {"arm64-v8a": "arm64"}
    OUTPUT_LIBRARY_NAME = "libmihomocore.so"
    ANDROID_PACKED_RELOCATIONS_LDFLAG = "-extldflags=-Wl,--pack-dyn-relocs=android"

    def __init__(self, config: ProjectConfig, ndk_tools: NdkTools):
        self.config = config
        self.ndk_tools = ndk_tools
        self.root = config.project_root
        self.output_dir = self.root / "build/native/go-core"
        self.app_jni_root = self.root / "jniLibs"
        self.build_flags = config.get_csv("golang.buildFlags", "-trimpath")
        self.core_kind = (config.get_string("golang.core", "mihomo") or "mihomo").strip().lower()
        self.mihomo_dir = self.root / "lib/mihomo/mihomo"
        self.kernel_patch_dir = self.root / ".github/patches/mihomo"
        if self.core_kind == "singbox":
            # The sing-box module lives in its own directory and is built as the module root, so
            # the package path is "." rather than a named sub-package.
            self.source_dir = self.root / "lib/singbox"
            self.package_name = "."
            self.build_tags = config.get_csv(
                "golang.singboxBuildTags",
                "with_clash_api,with_gvisor,with_quic,with_utls,with_wireguard",
            )
            self.kernel_patch_dir = self.root / ".github/patches/singbox"
        else:
            self.source_dir = self.root / "lib/native/go"
            self.build_tags = config.get_csv("golang.buildTags", "cmfa")
            self.package_name = config.get_string("golang.packageName", "cfa/native")
        self.core_version_stamp: CoreVersionStamp | None = None

    def build_all(self) -> None:
        if not self.source_dir.exists():
            raise RuntimeError(f"[GoCore] Source directory not found: {self.source_dir}")
        self.apply_kernel_patches()
        abis = configured_abis(self.config)
        if self.core_kind == "singbox":
            # No version stamp: sing-box has no constant package to inject into, and the stamp
            # files drive the mihomo-shaped release metadata.
            print("[GoCore] Engine: sing-box")
        else:
            self.core_version_stamp = resolve_core_version_stamp(self.config, self.root)
            write_core_version_stamp(self.core_version_stamp, abis, self.root)
        print(f"[GoCore] Building shared core ({self.OUTPUT_LIBRARY_NAME}) for ABIs: {', '.join(abis)}")
        for abi in abis:
            self.build_for_abi(abi)

    def apply_kernel_patches(self) -> None:
        if not self.kernel_patch_dir.is_dir() or not (self.mihomo_dir / ".git").exists():
            return
        patches = sorted(self.kernel_patch_dir.glob("*.patch"))
        if not patches:
            return
        print(f"[GoCore] Applying {len(patches)} mihomo kernel patch(es)")
        for patch in patches:
            already_applied = execute_command(
                ["git", "apply", "--reverse", "--check", str(patch)],
                working_dir=self.mihomo_dir,
                print_stdout=False,
                print_stderr=False,
                stderr_is_error=False,
            ).success
            if already_applied:
                print(f"[GoCore]   already applied: {patch.name}")
                continue
            result = execute_command(
                ["git", "apply", str(patch)],
                working_dir=self.mihomo_dir,
                stdout_prefix="[patch]",
                stderr_prefix="[patch]",
                stderr_is_error=False,
            )
            if not result.success:
                raise RuntimeError(f"Failed to apply kernel patch {patch.name}: {command_failure(result)}")
            print(f"[GoCore]   applied: {patch.name}")

    def build_for_abi(self, abi: str) -> None:
        arch = self.ABI_TO_GO_ARCH.get(abi)
        if not arch:
            raise RuntimeError(f"[GoCore] Unsupported ABI: {abi}")
        print(f"[building] Building for {abi} (Go shared core, arch: {arch})...")
        output_file = self.output_dir / abi / self.OUTPUT_LIBRARY_NAME
        output_file.parent.mkdir(parents=True, exist_ok=True)
        flags = self.merge_core_ldflags(self.build_flags, self.core_version_stamp)
        command = ["go", "build", "-buildmode", "c-shared", *flags]
        if self.build_tags:
            command.extend(["-tags", ",".join(self.build_tags)])
        command.extend(["-o", str(output_file), self.package_name])
        result = execute_command(
            command,
            working_dir=self.source_dir,
            environment={
                "CGO_ENABLED": "1",
                "GOOS": "android",
                "GOARCH": arch,
                "CC": str(self.ndk_tools.get_clang_path(abi)),
                "GOCACHE": str(self.root / "build/go-cache"),
            },
            stdout_prefix=f"[building][{abi}]",
            stderr_prefix=f"[building][{abi}]",
            stderr_is_error=False,
        )
        if not result.success or not output_file.is_file():
            raise RuntimeError(f"[GoCore] Failed to build {abi}: {command_failure(result)}")
        destination = self.app_jni_root / abi / self.OUTPUT_LIBRARY_NAME
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_bytes(output_file.read_bytes())
        print(f"[GoCore] Copied to {destination}")

    @classmethod
    def merge_core_ldflags(cls, base_flags: list[str], stamp: CoreVersionStamp | None) -> list[str]:
        additions = [cls.ANDROID_PACKED_RELOCATIONS_LDFLAG]
        if stamp and stamp.commit != "unknown":
            additions.extend([
                f"-X github.com/metacubex/mihomo/constant.Version={stamp.display_version.lower()}",
                f"-X github.com/metacubex/mihomo/constant.BuildTime={stamp.build_time}",
            ])
        inject = " ".join(additions)
        flags = list(base_flags)
        try:
            index = flags.index("-ldflags")
        except ValueError:
            flags.extend(["-ldflags", f"-s -w {inject}"])
        else:
            if index + 1 < len(flags):
                flags[index + 1] = f"{flags[index + 1].strip()} {inject}"
            else:
                flags.insert(index + 1, f"-s -w {inject}")
        return flags




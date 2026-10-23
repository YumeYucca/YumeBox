# Toolchain packaging

The supported entry point is the `packaging` JVM application. It uses the pinned
Kotlin Toolchain 0.13.0 wrapper, Kotlin 2.4.21, JDK25 and Android SDK37/build-tools
37.0.0. Hand-maintained Kotlin sources remain in shallow `src/` directories.

## Build

From the repository root on Windows (quote dotted `-P` arguments in PowerShell):

```powershell
.\kotlin.bat run -m packaging -- --build-type=debug
.\kotlin.bat run -m packaging -- --build-type=release '-Pgeo.bundle=false'
.\kotlin.bat run -m packaging -- --build-type=release '-Pgeo.bundle=true'
.\kotlin.bat run -m packaging -- --build-type=release --compile-only
```

On Linux/macOS replace `.\kotlin.bat` with `sh kotlin`. Set `ANDROID_HOME` to an
SDK with accepted licenses. GitHub Actions uses `macos-26` for every job and
derives the Android NDK version from `kernel.properties`. Native inputs remain in `jniLibs/arm64-v8a`; build
them with `scripts/native-build.py` as before. Builtin APKs additionally require
the three XZ geo files in `build/generated/assets/geo`.

Product/version inputs live in `product.properties`; native configuration lives
in `kernel.properties`. Existing explicit `-Pbuild.*`, `-Pcore.*`, `-Papk.output.*`,
`-Pgeo.bundle` and update-channel parameters are accepted by the Kotlin CLI
application, not by Gradle. Unknown parameters fail.

Release signing requires ignored `signing.properties` with `keystore.path`,
`keystore.password`, `key.alias`, `key.password`. No secrets enter build requests
or generated sources. Debug builds can use the standard debug keystore. An
unsigned artifact requires explicit `--allow-unsigned`; CI does not pass it.

## Pipeline and compatibility

A project lock protects request generation, compilation and publication. The
pipeline generates BuildInfo, compiles with Toolchain, rewrites compiled Manifest
metadata through AAPT2/protobuf, injects native/geo/license inputs, builds the
loader DEX, packs payloads, aligns, signs and validates the final APK before
publishing it into `output_apk/<variant>`. A JSON sidecar records its identity,
version and SHA-256. Dependency-license collection fails rather than emit empty
or unrecognized metadata. The license text catalog preserves the previous
AboutLibraries-generated definitions; the component list comes from the actual
selected runtime graph, not a frozen Gradle dependency snapshot.

The Android engine inside Toolchain is still Gradle/AGP. `ToolchainBackend` owns
the narrow 0.13.0 compatibility adapter for desugaring, scoped dependency
exclusions and resource-only library AARs. `resource-symbols` supplies compile-only
R headers; real R classes are linked by the Android backend. No business source
tree is copied. Shim/resource changes select a new content-hashed build root,
while the private Gradle distribution cache is reused. Update this adapter before
changing the wrapper version. Do not bypass packaging with a bare app build.

No test directories or fixtures are maintained. Production artifact checks
include payload sizes/SHA-256, bootstrap libraries, Manifest identity/version,
16KB alignment, APK signatures and signer certificate identity.

## API publishing

```powershell
.\kotlin.bat run -m packaging -- --prepare-api '-Papi.version=0.1.0'
.\kotlin.bat run -m packaging -- --publish-api '-Papi.version=0.1.0'
```

The first command uses Toolchain native Maven publishing into
`build/api-publication` without remote upload. The second uses `GITHUB_ACTOR` and
`GITHUB_TOKEN`. Publishing version/repository overrides are restored under the
project lock, and temporary credential files are removed. Java8, sources/POM and
the empty JavaDoc artifact contract are retained.

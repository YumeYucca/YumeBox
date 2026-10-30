# Builds the P0 spike APK without Gradle/AGP: aapt2 + javac + d8 + zipalign + apksigner.
# Kept out of the main build on purpose 闁?this app is a probe, not a product.
#
#   pwsh spike/vpnhost/build.ps1
#
# Requires: JDK 21 and an Android SDK with build-tools 34.0.0 + platforms/android-34.
param(
    [string]$SdkRoot = "",
    [string]$JdkHome = "",
    [string]$ToolsRoot = "",
    [string]$Abi = "arm64"
)

$ErrorActionPreference = "Continue"
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$root = (Resolve-Path "$here/../..").Path

# The toolchain lives beside the checkout rather than inside it, so a plain `git status` in the
# repo stays clean. Fall back to a repo-local tools dir when someone vendors it instead.
if (-not $ToolsRoot) {
    $sibling = Join-Path (Split-Path -Parent $root) "tools"
    $ToolsRoot = if (Test-Path $sibling) { $sibling } else { Join-Path $root "tools" }
}
if (-not $SdkRoot) { $SdkRoot = Join-Path $ToolsRoot "sdk" }
if (-not $JdkHome) {
    $JdkHome = (Get-ChildItem $ToolsRoot -Directory -Filter "jdk-21*" | Select-Object -First 1).FullName
}

$java = Join-Path $JdkHome "bin/java.exe"
$buildTools = Join-Path $SdkRoot "build-tools/34.0.0"
$androidJar = Join-Path $SdkRoot "platforms/android-34/android.jar"
$workspace = Split-Path -Parent $root
$scratch = Join-Path $workspace "build"
$work = Join-Path $scratch "spike"
$abiDir = @{ arm64 = "arm64-v8a"; arm = "armeabi-v7a"; x86_64 = "x86_64" }[$Abi]
$out = Join-Path $work "app-debug.apk"

foreach ($required in @($java, $androidJar, (Join-Path $buildTools "aapt2.exe"))) {
    if (-not (Test-Path $required)) { throw "missing tool: $required" }
}

Remove-Item -Recurse -Force $work -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path "$work/classes", "$work/dex", "$work/stage/lib/$abiDir" | Out-Null

Write-Host "[spike] building core library ($Abi)"
$ndk = Join-Path $ToolsRoot "android-ndk-r27c"
$ndkPrefix = Join-Path $ndk "toolchains/llvm/prebuilt/windows-x86_64"
$cc = Join-Path $ndkPrefix "bin/aarch64-linux-android26-clang.cmd"
if (-not (Test-Path $cc)) { throw "missing NDK compiler: $cc" }

$env:PATH = "$ToolsRoot/go/bin;$env:PATH"
$env:GOPROXY = "https://goproxy.cn,direct"
$env:GOSUMDB = "off"
$env:GOFLAGS = "-mod=mod"
$env:GOMODCACHE = "$scratch/gomodcache"
$env:GOCACHE = "$scratch/gocache"
$env:GOTMPDIR = "$scratch/gotmp"
$env:CGO_ENABLED = "1"
$env:GOOS = "android"
$env:GOARCH = "arm64"
$env:CC = $cc
New-Item -ItemType Directory -Force -Path "$work/core" | Out-Null
Push-Location "$root/lib/singbox"
& go build -buildmode=c-shared -trimpath -tags "with_clash_api with_gvisor with_quic with_utls with_wireguard" `
    -ldflags "-s -w" -o "$work/core/libyumecore.so" .
if ($LASTEXITCODE -ne 0) { throw "core build failed" }
Pop-Location
Copy-Item "$work/core/libyumecore.so" "$work/stage/lib/$abiDir/libyumecore.so"

Write-Host "[spike] building JNI shim"
& $cc -shared -fPIC -O2 -o "$work/stage/lib/$abiDir/libcorebridge.so" "$here/native/corebridge.c" `
    -I "$ndkPrefix/sysroot/usr/include"
if ($LASTEXITCODE -ne 0) { throw "corebridge build failed" }

Write-Host "[spike] linking resources"
& "$buildTools/aapt2.exe" link -o "$work/base.apk" -I $androidJar --manifest "$here/AndroidManifest.xml"
if ($LASTEXITCODE -ne 0) { throw "aapt2 link failed" }

Write-Host "[spike] compiling java"
$sources = Get-ChildItem -Recurse -Filter *.java "$here/java" | ForEach-Object { $_.FullName }
& "$JdkHome/bin/javac.exe" -source 8 -target 8 -nowarn -bootclasspath $androidJar -classpath $androidJar -d "$work/classes" $sources
if ($LASTEXITCODE -ne 0) { throw "javac failed" }

Write-Host "[spike] dexing"
$classes = Get-ChildItem -Recurse -Filter *.class "$work/classes" | ForEach-Object { $_.FullName }
# build-tools 34's bundled d8 crashes on javac 21 output (NPE on anonymous-class attributes), so
# prefer a standalone R8 jar when one is present and fall back to the SDK copy otherwise.
$r8Jar = Join-Path $ToolsRoot "r8.jar"
$env:JAVA_HOME = $JdkHome
if (Test-Path $r8Jar) {
    & "$JdkHome/bin/java.exe" -cp $r8Jar com.android.tools.r8.D8 --lib $androidJar --min-api 26 --output "$work/dex" $classes
} else {
    & "$buildTools/d8.bat" --lib $androidJar --min-api 26 --output "$work/dex" $classes
}
if ($LASTEXITCODE -ne 0) { throw "d8 failed" }

Write-Host "[spike] packaging"
Copy-Item "$work/base.apk" "$work/unsigned.apk"
Push-Location $work
& "$JdkHome/bin/jar.exe" uf unsigned.apk -C dex classes.dex
& "$JdkHome/bin/jar.exe" uf unsigned.apk -C stage "lib"
Pop-Location
& "$buildTools/zipalign.exe" -f 4 "$work/unsigned.apk" "$work/aligned.apk"
if ($LASTEXITCODE -ne 0) { throw "zipalign failed" }

$keystore = Join-Path $scratch "spike.keystore"
if (-not (Test-Path $keystore)) {
    & "$JdkHome/bin/keytool.exe" -genkeypair -keystore $keystore -storepass android -keypass android `
        -alias spike -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=YumeBox Spike" | Out-Null
}

Write-Host "[spike] signing"
& "$buildTools/apksigner.bat" sign --ks $keystore --ks-pass pass:android --key-pass pass:android `
    --out $out "$work/aligned.apk"
if ($LASTEXITCODE -ne 0) { throw "apksigner failed" }

Write-Host "[spike] done: $out"
Get-Item $out | Select-Object @{n = "MB"; e = { [math]::Round($_.Length / 1MB, 1) } }, FullName | Format-Table -AutoSize




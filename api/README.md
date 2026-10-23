# YumeBox API

`api` is a Kotlin/JVM client for the YumeBox controller REST API.

## Dependency

```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/YumeYucca/YumeBox")
        credentials {
            username = providers.gradleProperty("gpr.user").orNull
            password = providers.gradleProperty("gpr.key").orNull
        }
    }
}

dependencies {
    implementation("io.github.yumeyucca.yumebox:api:0.1.0-SNAPSHOT")
}
```

## Usage

```kotlin
val client = ApiClient(
    ApiConfig(
        endpoint = "http://127.0.0.1:9090",
        secret = "controller-secret",
    )
)

val mode = client.tunnelMode()
val proxies = client.proxies()
client.selectProxy(group = "Proxy", name = "Hong Kong 01")
val delay = client.proxyDelay("Hong Kong 01")
client.close()
```

The convenience methods cover the common controller endpoints. Use `request` or `requestText` for
newer controller endpoints before a dedicated helper is added.

## Publishing

```bash
sh kotlin run -m packaging -- --prepare-api -Papi.version=0.1.0
sh kotlin run -m packaging -- --publish-api -Papi.version=0.1.0
```

The prepare command writes a local Maven repository under `build/api-publication`.
GitHub Packages publishing uses `GITHUB_ACTOR` and `GITHUB_TOKEN`; temporary credential
files are removed after publishing. Override the repository with
`-Papi.githubRepository=owner/repository`. On Windows use `.\kotlin.bat` and quote
dotted `-P` arguments in PowerShell.

For an external project, create a GitHub personal access token with `read:packages` and place the
GitHub username and token in `~/.gradle/gradle.properties`:

```properties
gpr.user=GITHUB_USERNAME
gpr.key=GITHUB_TOKEN_WITH_READ_PACKAGES
```

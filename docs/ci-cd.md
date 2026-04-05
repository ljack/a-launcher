# CI/CD Setup

## GitHub Actions Workflows

### Build & Test (`build.yml`)
- **Triggers**: Push to `master`, Pull Requests
- **Steps**: Build debug APK, run unit tests, run lint
- **Artifacts**: `app-debug.apk`, test results, lint report

### Release (`release.yml`)
- **Triggers**: Tag push matching `v*` (e.g., `v1.0`, `v0.1-alpha`)
- **Steps**: Run tests, build APK, create GitHub Release with changelog
- **Pre-release**: Tags containing `-` are marked as pre-release (e.g., `v1.0-beta`)

## Creating a Release

```bash
git tag v1.0
git push origin v1.0
```

The workflow will:
1. Run tests
2. Build the APK (debug if no signing key, release if configured)
3. Generate changelog from commits since last tag
4. Create a GitHub Release with the APK attached

## Setting Up Signed Release Builds

To enable signed release APKs:

### 1. Generate a keystore
```bash
keytool -genkey -v -keystore release.keystore -alias alauncher \
  -keyalg RSA -keysize 2048 -validity 10000
```

### 2. Base64 encode it
```bash
base64 -i release.keystore | pbcopy
```

### 3. Add GitHub Secrets
Go to repo → Settings → Secrets → Actions, add:

| Secret | Value |
|---|---|
| `KEYSTORE_BASE64` | Base64 encoded keystore |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias (e.g., `alauncher`) |
| `KEY_PASSWORD` | Key password |

### 4. Add signing config to `app/build.gradle.kts`

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("release.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(...)
        }
    }
}
```

Until signing is configured, releases use debug APK.

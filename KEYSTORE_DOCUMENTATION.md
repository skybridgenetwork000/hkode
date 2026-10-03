# HKode Permanent Keystore Documentation

This document contains the official and permanent keystore details used for signing the **HKode** Android application. This configuration ensures that all future updates retain signature compatibility across devices.

---

## 1. Keystore Parameters

| Parameter | Value |
|---|---|
| **Keystore File** | `Hkode.jks` (also backed up in `app/keystore/Hkode.jks`) |
| **Keystore Format** | Java KeyStore (`JKS`) |
| **Keystore Password** | `Hkode1234h` |
| **Key Alias** | `hkode` (or `HKode`) |
| **Key Password** | `Hkode1234h` |
| **Key Size** | `2048` bit RSA |
| **Signature Algorithm** | `SHA1withRSA` |
| **Validity** | 99 Years (Valid until `September 8, 2125`) |

---

## 2. Certificate Distinguished Name (DN) Details

- **Common Name (CN)**: `HKode`
- **Organizational Unit (OU)**: `HKode/H3NRICAN3`
- **Organization (O)**: `H3NRICAN3`
- **City / Locality (L)**: `owerri`
- **State / Province (ST)**: `imo`
- **Country Code (C)**: `NG`

---

## 3. Certificate Fingerprints

- **SHA-1 Fingerprint**:
  `28:01:5A:3F:56:F1:42:8D:3C:1E:10:85:90:20:4D:EB:02:07:53:36`

- **SHA-256 Fingerprint**:
  `3D:41:36:0E:37:B1:5D:88:BD:48:BF:E8:38:BB:0C:9E:9C:7B:31:00:27:79:62:FB:12:0E:33:E7:C1:88:5F:A6`

---

## 4. Gradle Integration

The signing configuration is permanently defined in `app/build.gradle`:

```groovy
android {
    signingConfigs {
        hkode {
            storeFile file("${rootProject.rootDir}/Hkode.jks")
            storePassword "Hkode1234h"
            keyAlias "hkode"
            keyPassword "Hkode1234h"
            v1SigningEnabled true
            v2SigningEnabled true
        }
    }

    buildTypes {
        release {
            signingConfig signingConfigs.hkode
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
        unsigned {
            initWith release
            signingConfig null
            matchingFallbacks = ['release']
        }
        debug {
            signingConfig signingConfigs.hkode
        }
    }
}
```

---

## 5. Building the APKs

- **Signed Release APK (`Hkode.apk`)**:
  ```bash
  gradle :app:assembleRelease
  ```
  Generates `Hkode.apk` signed with `Hkode.jks`.

- **Unsigned Release APK (`Hkode_unsign.apk`)**:
  ```bash
  gradle :app:assembleUnsigned
  ```
  Generates `Hkode_unsign.apk` (pure unsigned binary).

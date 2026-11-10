plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Engineered by: Aries Thorne & Todd Steinhauer for Wolf Reactive Systems (WRS)
// Special thanks to the NTXMESH community, Meshcore Community, and Pat Steinhauer-Scates for their guidance and support.

android {
    namespace = "com.wolfreactive.cairn"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.wolfreactive.cairn.station"
        minSdk = 26
        targetSdk = 35
        versionCode = 120
        versionName = "1.4.1"
    }

    // Release signing material lives OUTSIDE git. Copy android/keystore.properties.example to
    // android/keystore.properties (gitignored) and fill it in, or supply the same four values as
    // CAIRN_STORE_FILE / CAIRN_STORE_PASSWORD / CAIRN_KEY_ALIAS / CAIRN_KEY_PASSWORD environment variables (CI secrets).
    val ksProps = java.util.Properties().apply {
        val pf = rootProject.file("keystore.properties")
        if (pf.isFile) pf.inputStream().use { load(it) }
    }
    fun ks(prop: String, env: String): String? = (ksProps.getProperty(prop) ?: System.getenv(env))?.takeIf { it.isNotBlank() }
    val ksFile = ks("storeFile", "CAIRN_STORE_FILE")
    val haveSigning = ksFile != null && ks("storePassword", "CAIRN_STORE_PASSWORD") != null &&
        ks("keyAlias", "CAIRN_KEY_ALIAS") != null && ks("keyPassword", "CAIRN_KEY_PASSWORD") != null

    signingConfigs {
        if (haveSigning) {
            create("release") {
                storeFile = rootProject.file(ksFile!!)
                storePassword = ks("storePassword", "CAIRN_STORE_PASSWORD")
                keyAlias = ks("keyAlias", "CAIRN_KEY_ALIAS")
                keyPassword = ks("keyPassword", "CAIRN_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Fail closed: no signing material means no release APK (never fall back to a shared key).
            if (haveSigning) signingConfig = signingConfigs.getByName("release")
            else gradle.taskGraph.whenReady {
                if (allTasks.any { it.name.contains("Release", ignoreCase = true) && it.name.startsWith("assemble") || it.name.startsWith("bundle") && it.name.contains("Release") })
                    throw GradleException("Release signing is not configured: see android/keystore.properties.example")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    lint { abortOnError = false }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.webkit:webkit:1.11.0")
    implementation("com.github.mik3y:usb-serial-for-android:3.8.1")
    testImplementation("junit:junit:4.13.2")
}

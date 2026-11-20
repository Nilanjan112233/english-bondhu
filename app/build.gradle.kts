import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.englishbondhu.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.englishbondhu.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 8
        versionName = "0.4.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        // Secrets never live in source control. Lookup order: -P Gradle property,
        // environment variable, then the git-ignored local.properties file.
        val localProperties = Properties().apply {
            val file = rootProject.file("local.properties")
            if (file.isFile) file.inputStream().use(::load)
        }
        fun config(name: String, default: String): String =
            providers.gradleProperty(name)
                .orElse(providers.environmentVariable(name))
                .orElse(localProperties.getProperty(name) ?: default)
                .get()
                .trim()
        fun quoted(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

        buildConfigField("String", "AI_BASE_URL", quoted(config("AI_BASE_URL", "http://10.0.2.2:8787")))
        buildConfigField("String", "GEMINI_API_KEY", quoted(config("GEMINI_API_KEY", "")))
        buildConfigField("String", "GEMINI_TEXT_MODEL", "\"gemini-3.1-flash-lite\"")
        buildConfigField("String", "GEMINI_LIVE_MODEL", "\"gemini-3.1-flash-live-preview\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.04.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    testImplementation("junit:junit:4.13.2")
    // Android's org.json is a stub in local JVM tests; use the real implementation there.
    testImplementation("org.json:json:20240303")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

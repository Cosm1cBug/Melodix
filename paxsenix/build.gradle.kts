plugins {
    id("com.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.melodix.paxsenix"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        // No desugaring: this module uses no java.time/stream APIs and minSdk 26
        // provides them natively. Enabling it here would force the whole app to
        // enable core library desugaring (AAR metadata requirement), which the
        // app intentionally keeps disabled.
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.client.encoding)
    implementation(libs.timber)
    testImplementation(libs.junit)

    implementation(project(":betterlyrics"))
}

import org.jetbrains.kotlin.gradle.plugin.ide.kotlinExtrasSerialization

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    id("com.google.protobuf") version "0.9.4" // versión reciente
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.26.1"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") {
                    option("lite") // Usa clases más ligeras
                }
            }
        }
    }
}

android {
    namespace = "com.shary.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.shary.app"
        minSdk = 26
        //noinspection EditedTargetSdkVersion
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/NOTICE.md",
                "META-INF/LICENSE.md",
                "META-INF/LICENSE",
                "META-INF/NOTICE"
            )
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    // Extra implementations
    // Jetpack Compose
    implementation(libs.ui) // o tu versión Compose actual
    implementation(libs.androidx.lifecycle.viewmodel.compose) // 👈 ESTA ES LA CLAVE
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.navigation.runtime.ktx) // or latest
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.foundation.layout.android)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.androidx.datastore.v100)
    implementation(libs.androidx.datastore.core)
    implementation(libs.protobuf.javalite) // Ligero para Android
    implementation(libs.security.crypto)
    implementation(libs.androidx.fragment.ktx)
    // JavaMail (Jakarta Mail) compatible with Android
    implementation(libs.android.mail)
    implementation(libs.android.activation)
    // Ktor for HTTP requests
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp) // Use OkHttp engine on Android
    implementation(libs.ktor.client.serialization)
    implementation(libs.androidx.foundation.android)
    implementation(libs.androidx.foundation.android) // (Optional) JSON parsing
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.appcompat) // or latest stable
    // AppCompact
    implementation(libs.material3)
    //
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
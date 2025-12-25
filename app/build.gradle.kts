plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.protobuf)
    id("org.jetbrains.kotlin.kapt") /**

“plugin is already on the classpath with an unknown version”, because the Kotlin plugin
already brings kapt. So you cannot apply kapt with a version alias — it must be applied
without version. Use plain id, no alias. And in libs.versions.toml, remove this block:

kotlin-kapt = { id = "org.jetbrains.kotlin.kapt", version.ref = "kotlin" }

Why is that?

- kotlin {} plugins (android, jvm, multiplatform, kapt) are all shipped together.

- Only org.jetbrains.kotlin.android and org.jetbrains.kotlin.plugin.* (compose, serialization)
need version alignment.

- kapt is always applied using just id("org.jetbrains.kotlin.kapt").

 **/
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.google.services)
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") { option("lite") }
            }
        }
    }
}

android {
    namespace = "com.shary.app"
    compileSdk = 36

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
        debug {
            buildConfigField(
                "String",
                "FIREBASE_BASE_URL",
                "\"https://us-central1-shary-21b61.cloudfunctions.net\""
            )
        }
        release {
            buildConfigField(
                "String",
                "FIREBASE_BASE_URL",
                "\"https://us-central1-shary-21b61.cloudfunctions.net\""
            )
            signingConfig = signingConfigs.getByName("debug")
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
        buildConfig = true
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
    // Firebase
    /**
    En libs.versions.toml están bien definidas las librerías:

    firebase-auth-ktx = { group = "com.google.firebase", name = "firebase-auth-ktx" }
    firebase-firestore-ktx = { group = "com.google.firebase", name = "firebase-firestore-ktx" }

    pero en app/build.gradle.kts existe:

    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)

    Eso no corresponde con las claves del TOML.
    Gradle interpreta libs.firebase.auth.ktx como un “bundle” pero no existe, por eso queda vacío (:).

    Solución: En app/build.gradle.kts, se deben usar los nombres tal cual están en el TOML:

    implementation(libs.firebase.auth.ktx)       // ❌ incorrecto
    implementation(libs.firebase.firestore.ktx) // ❌ incorrecto

     */

    implementation(platform(libs.firebase.bom))
    implementation(libs.google.firebase.messaging)
    implementation(libs.google.firebase.auth)
    implementation(libs.google.firebase.firestore)
    implementation(libs.google.firebase.analytics)
    // FIX: Changed from implementation(libs.firebase.functions.ktx) to correct camelCase for TOML flat key
    //implementation(libs.firebaseFunctionsKtx)


    // Core AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.fragment.ktx)

    // Jetpack Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.ui.text.google.fonts)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.navigation.runtime.ktx)

    // Hilt & DI
    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Data & Storage
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.datastore.core)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.protobuf.javalite)

    // Networking
    // Direct OkHttp dependency is required for direct use of okhttp3.Request
    implementation(libs.okhttp) // Use the latest stable version
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.serialization)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.play.services)

    // Other
    implementation(libs.bcprov.jdk15to18)
    //implementation(libs.android.mail)
    //implementation(libs.android.activation)
    implementation(libs.material)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    //id("com.android.application")
    id("com.google.gms.google-services")
}

android {

    // =========================
    // Leer local.properties (por ejemplo OPENAI_API_KEY)
    // =========================
    val localProps = Properties().apply {
        val f = rootProject.file("local.properties")
        if (f.exists()) {
            f.inputStream().use { load(it) }
        }
    }

    // 1) Prioridad: gradle.properties / -P
    // 2) Fallback: local.properties
    val openaiApiKey: String =
        (project.findProperty("OPENAI_API_KEY") as String?)
            ?: (localProps.getProperty("OPENAI_API_KEY") ?: "")

    val openaiModel: String =
        (project.findProperty("OPENAI_MODEL") as String?)
            ?: (localProps.getProperty("OPENAI_MODEL") ?: "gpt-4.1-mini")

    namespace = "com.example.proyecto_iot"
    compileSdk = 36

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.example.proyecto_iot"

        // ✅ BuildConfig.OPENAI_API_KEY / BuildConfig.OPENAI_MODEL
        buildConfigField("String", "OPENAI_API_KEY", "\"$openaiApiKey\"")
        buildConfigField("String", "OPENAI_MODEL", "\"$openaiModel\"")

        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/INDEX.LIST",
                "META-INF/io.netty.versions.properties",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt"
            )
        }
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
}

dependencies {
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("com.google.android.material:material:1.12.0")
    implementation(platform("com.google.firebase:firebase-bom:34.4.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.facebook.android:facebook-login:latest.release")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.fragment:fragment:1.6.2")

    implementation("com.google.android.gms:play-services-maps:18.1.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")

    implementation("com.google.firebase:firebase-firestore")

    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    implementation("com.google.firebase:firebase-firestore:24.7.1")
    implementation("com.google.firebase:firebase-firestore:24.10.0")

    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")

    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("com.squareup.picasso:picasso:2.71828")
    implementation("com.itextpdf:itext7-core:7.1.15")
    implementation("com.github.deano2390:MaterialShowcaseView:1.3.7")
}

import com.android.build.api.dsl.ApkSigningConfig
import java.io.FileInputStream
import java.util.Properties
//import org.ajoberstar.grgit.Grgit

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("org.ajoberstar.grgit") version "5.3.3"
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.21"
}

//var gitVersionName = Grgit.describe()
//var gitVersionCode = if(gitVersionName != null && gitVersionName.isInteger()) gitVersionName.toInteger() else 1

//println("Version Name: $gitVersionName")
//println("Version Code: $gitVersionCode")

val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("/opt/key.properties")
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.futo.music"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.futo.music"
        minSdk = 29
        targetSdk = 36
        versionCode = 15
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }


    signingConfigs {
        create("release") {
            keyAlias = keystoreProperties["keyAlias"] as String?
            keyPassword = keystoreProperties["keyPassword"] as String?
            storeFile = if(keystoreProperties["storeFile"] as String? != null) file(keystoreProperties["storeFile"] as String) as File? else null
            storePassword = keystoreProperties["storePassword"] as String?
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs["release"] as ApkSigningConfig?
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
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.documentfile)
    implementation(libs.material)
    implementation(libs.androidx.media3.session)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation("androidx.media3:media3-exoplayer:1.10.0-rc02")
    implementation("androidx.media3:media3-ui:1.10.0-rc02")
    implementation("androidx.media3:media3-session:1.10.0-rc02")

    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    //Database
    implementation("androidx.room:room-runtime:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")

    //Images
    annotationProcessor("com.github.bumptech.glide:compiler:5.0.5")
    implementation("com.github.bumptech.glide:glide:5.0.5")
    implementation ("jp.wasabeef:glide-transformations:4.3.0")
    implementation("androidx.palette:palette:1.0.0")

    //Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    //Async
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    //HTTP
    implementation("com.squareup.okhttp3:okhttp:5.3.0")

    implementation("androidx.core:core-splashscreen:1.0.0")
}
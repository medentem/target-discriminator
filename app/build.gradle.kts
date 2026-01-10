plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    id("androidx.navigation.safeargs.kotlin")
}

android {
    namespace = "com.targetdiscriminator"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.targetdiscriminator"
        minSdk = 24
        targetSdk = 35
        versionCode = 7
        versionName = "1.1.0"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildFeatures {
        viewBinding = true
    }
}

// ============================================================================
// MEDIA SYNC TASKS - Sync media from submodule to assets directory
// ============================================================================

tasks.register("syncMediaAssets", Exec::class) {
    group = "build"
    description = "Sync media assets from submodule to assets directory"
    workingDir = rootProject.projectDir
    
    // Use bash script (works on Unix/Mac, use PowerShell on Windows)
    if (System.getProperty("os.name").lowercase().contains("windows")) {
        commandLine("powershell", "-ExecutionPolicy", "Bypass", "-File", "scripts/sync-media.ps1")
    } else {
        commandLine("bash", "scripts/sync-media.sh")
    }
    
    // Only run if media submodule exists
    doFirst {
        val mediaDir = file("${rootProject.projectDir}/media")
        if (!mediaDir.exists()) {
            throw GradleException(
                "Media submodule not found at ${mediaDir.absolutePath}.\n" +
                "Please run: git submodule update --init --recursive"
            )
        }
    }
    
    // Show output
    isIgnoreExitValue = false
    standardOutput = System.out
    errorOutput = System.err
}

// Optional: Task to update submodule to latest
tasks.register("updateMediaSubmodule", Exec::class) {
    group = "build"
    description = "Update media submodule to latest commit from remote"
    workingDir = rootProject.projectDir
    commandLine("git", "submodule", "update", "--remote", "media")
}

// Make preBuild depend on syncMediaAssets
// This ensures media is synced before every build
tasks.named("preBuild") {
    dependsOn("syncMediaAssets")
}

// ============================================================================
// END MEDIA SYNC TASKS
// ============================================================================

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.activity:activity-ktx:1.8.2")
    // Material 3
    implementation("com.google.android.material:material:1.11.0")
    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    // Navigation Component
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.6")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.6")
    // Lifecycle & ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    // ExoPlayer for video playback
    implementation("androidx.media3:media3-exoplayer:1.2.0")
    implementation("androidx.media3:media3-ui:1.2.0")
    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}


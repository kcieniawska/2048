plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.a2048"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.example.a2048"
        minSdk = 21
        targetSdk = 33
        versionCode = 1
        versionName = "1.3"

        // Dodano konfigurację dla testów instrumentowanych
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    // === DODANA SEKCJA build.gradle ===
    buildTypes {
        release {
            // Włączenie zmniejszania kodu, zaciemniania i optymalizacji dla wersji wydania
            isMinifyEnabled = false
            // Warto ustawić true, jeśli używasz ProGuard lub R8,
            // ale dla prostego projektu zostawiamy false dla łatwiejszej kompilacji.
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // Wersja debug domyślnie ma isMinifyEnabled = false
            isMinifyEnabled = false
        }
    }
    // =================================
}

dependencies {
    // --- Zależności aplikacji ---
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")

    // Zaktualizowano wersję Gson na szeroko stosowaną, stabilną wersję 2.10.1
    implementation("com.google.code.gson:gson:2.10.1")

    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // --- Zależności testowe ---
    // 1. Zależność dla testów lokalnych (JUnit dla testów unitarnych)
    testImplementation("junit:junit:4.13.2")

    // 2. Zależności dla testów instrumentowanych (androidTest)
    // Nowoczesny runner testów instrumentowanych
    androidTestImplementation("androidx.test.ext:junit:1.1.5")

    // Standardowa zależność dla testów UI
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

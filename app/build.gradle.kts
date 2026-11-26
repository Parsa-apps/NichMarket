plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.parsaapps.nichmarket"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.parsaapps.nichmarket"
        minSdk = 24
        targetSdk = 35
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        versionCode = 2
        versionName = "1.1.0"

        // ---- Myket store configuration ----
        val marketApplicationId = "ir.mservices.market"
        val marketBindAddress = "ir.mservices.market.InAppBillingService.BIND"
        manifestPlaceholders["marketApplicationId"] = marketApplicationId
        manifestPlaceholders["marketBindAddress"] = marketBindAddress
        manifestPlaceholders["marketPermission"] = "$marketApplicationId.BILLING"

        // Public verification key from Myket; never store the private signing key in source.
        val myketPublicKey = providers.environmentVariable("MYKET_PUBLIC_KEY").orNull.orEmpty().trim()
        require(myketPublicKey.isEmpty() || myketPublicKey.matches(Regex("[A-Za-z0-9+/=]+"))) {
            "MYKET_PUBLIC_KEY must be a single-line Base64 RSA public key"
        }
        buildConfigField("String", "IAB_PUBLIC_KEY", "\"$myketPublicKey\"")
    }

    // ---- امضای Release از طریق متغیرهای محیطی (در GitHub Actions یا خط فرمان) ----
    // اگر این متغیرها تنظیم نشده باشند، خروجی Release بدون امضا ساخته می‌شود.
    val signingKeystore: String? = System.getenv("SIGNING_KEYSTORE_PATH")

    signingConfigs {
        create("release") {
            if (signingKeystore != null) {
                storeFile = file(signingKeystore)
                storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (signingKeystore != null) {
                signingConfig = signingConfigs.getByName("release")
            }
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
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)

    // Myket In-App Billing SDK
    implementation(libs.myket.billing)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    debugImplementation(libs.androidx.ui.tooling)
}

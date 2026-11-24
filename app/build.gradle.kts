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
        versionCode = 1
        versionName = "1.0.0"

        // ---- Myket store configuration ----
        val marketApplicationId = "ir.mservices.market"
        val marketBindAddress = "ir.mservices.market.InAppBillingService.BIND"
        manifestPlaceholders["marketApplicationId"] = marketApplicationId
        manifestPlaceholders["marketBindAddress"] = marketBindAddress
        manifestPlaceholders["marketPermission"] = "$marketApplicationId.BILLING"

        // TODO: کلید عمومی (RSA Base64) را از پنل توسعه‌دهندگان مایکت بگیرید و اینجا قرار دهید
        buildConfigField("String", "IAB_PUBLIC_KEY", "\"REPLACE_WITH_YOUR_MYKET_PUBLIC_KEY\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)

    // Myket In-App Billing SDK
    implementation(libs.myket.billing)

    debugImplementation(libs.androidx.ui.tooling)
}

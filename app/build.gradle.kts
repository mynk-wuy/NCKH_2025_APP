@file:Suppress("UNUSED_EXPRESSION")

plugins {
    alias(libs.plugins.android.application)
}
configurations.all {
    exclude(group = "com.google.guava", module = "listenablefuture")
}

android {
    namespace = "com.example.mqtt"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.mqtt"
        minSdk = 25
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
    buildFeatures {
        viewBinding = true
    }
    packagingOptions {
        exclude ("META-INF/io.netty.versions.properties")
        exclude ("META-INF/INDEX.LIST")
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    // Google Play Services Maps: Hiển thị bản đồ Google Maps
    implementation ("com.google.android.gms:play-services-maps:18.2.0")
    // Google Play Services Location: Truy cập vị trí thiết bị
    implementation ("com.google.android.gms:play-services-location:21.0.1")
    // AndroidX Core KTX: Cung cấp tiện ích mở rộng Kotlin cho AndroidX
    implementation ("androidx.core:core-ktx:1.7.0")
    // Guava: Thư viện tiện ích của Google cho xử lý dữ liệu, bộ sưu tập
    implementation ("com.google.guava:guava:23.0")
    // Retrofit: Gửi và nhận yêu cầu HTTP cho API RESTful
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    // Retrofit Gson Converter: Chuyển đổi JSON thành đối tượng Java
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")

    // MPAndroidChart (thư viện vẽ biểu đồ)
    implementation ("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // AndroidX Navigation Fragment: Quản lý điều hướng giữa các Fragment
    implementation ("androidx.navigation:navigation-fragment:2.5.3")
    // AndroidX Navigation UI: Liên kết Navigation với giao diện
    implementation ("androidx.navigation:navigation-ui:2.5.3")
    // HiveMQ MQTT Client: Kết nối và giao tiếp qua giao thức MQTT
    implementation ("com.hivemq:hivemq-mqtt-client:1.3.0")
    // iText 7 Core: Tạo và xử lý tài liệu PDF
    implementation ("com.itextpdf:itext7-core:7.2.5")
}
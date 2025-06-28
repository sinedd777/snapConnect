import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 34
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        localProperties.keys.forEach { key ->
            val keyStr = key.toString()
            if (!keyStr.contains(".")) {
                buildConfigField("String", keyStr, "\"${localProperties.getProperty(keyStr)}\"")
            }
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

val googleServicesJson = """
{
  "project_info": {
    "project_number": "${localProperties.getProperty("FIREBASE_MESSAGING_SENDER_ID")}",
    "project_id": "${localProperties.getProperty("FIREBASE_PROJECT_ID")}",
    "storage_bucket": "${localProperties.getProperty("FIREBASE_PROJECT_ID")}.firebasestorage.app"
  },
  "client": [
    {
      "client_info": {
        "mobilesdk_app_id": "${localProperties.getProperty("FIREBASE_APP_ID")}",
        "android_client_info": {
          "package_name": "com.example.myapplication"
        }
      },
      "oauth_client": [
        {
          "client_id": "${localProperties.getProperty("GOOGLE_WEB_CLIENT_ID")}",
          "client_type": 3
        }
      ],
      "api_key": [
        {
          "current_key": "${localProperties.getProperty("FIREBASE_API_KEY")}"
        }
      ],
      "services": {
        "appinvite_service": {
          "other_platform_oauth_client": [
            {
              "client_id": "${localProperties.getProperty("GOOGLE_WEB_CLIENT_ID")}",
              "client_type": 3
            }
          ]
        }
      }
    }
  ],
  "configuration_version": "1"
}
"""

File(project.projectDir, "google-services.json").writeText(googleServicesJson)

dependencies {
    implementation("com.composables:core:1.36.1")
    implementation("ai.deepar.ar:DeepAR:5.6.19")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.storage.ktx)
    implementation(libs.google.id)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.accompanist.permissions)
    implementation(libs.androidx.concurrent.futures.ktx)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.guava)
    implementation(libs.coil.compose)
    implementation(libs.play.services.location)
    
    // OpenStreetMap dependencies
    implementation("org.osmdroid:osmdroid-android:6.1.16")
    implementation("org.osmdroid:osmdroid-mapsforge:6.1.16")
    implementation("org.osmdroid:osmdroid-geopackage:6.1.16") {
        exclude(group = "com.j256.ormlite", module = "ormlite-core")
    }
    implementation("org.osmdroid:osmdroid-wms:6.1.16")
    implementation("androidx.preference:preference-ktx:1.2.1")
    
    // GeoFire for location-based queries
    implementation("com.firebase:geofire-android-common:3.2.0")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    
    // AR dependencies
    implementation("com.google.ar:core:1.40.0")
    implementation("io.github.sceneview:arsceneview:0.10.0")
}
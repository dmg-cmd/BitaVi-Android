plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.hilt)
}

kotlin {
  jvmToolchain(17)
}

android {
  namespace = "com.aistudio.localizador"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.aistudio.localizador.vqtspk"
    minSdk = 24
    targetSdk = 35
    versionCode = 3
    versionName = "3.2.3"

    testInstrumentationRunner = "com.aistudio.localizador.HiltTestRunner"

    externalNativeBuild {
      cmake {
        cppFlags("-Wl,-z,max-page-size=16384")
      }
    }
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig_manual") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      // Usar la firma por defecto de Android para evitar errores de archivo no encontrado
    }
  }
  compileOptions {
    isCoreLibraryDesugaringEnabled = true
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions {
    unitTests {
      isIncludeAndroidResources = true
      // Worker de tests dedicado: heap controlado y ejecución secuencial
      // para evitar OutOfMemoryError con Robolectric + MockK en equipos con poca RAM libre.
      // forkEvery=50 recicla el worker y libera ClassLoaders acumulados (ByteBuddy/Robolectric).
      all {
        it.maxHeapSize = "3072m"
        it.maxParallelForks = 1
        it.forkEvery = 50
        it.jvmArgs(
          "-XX:+HeapDumpOnOutOfMemoryError",
          "-XX:MaxMetaspaceSize=512m"
        )
      }
    }
  }

  packaging {
    jniLibs {
      useLegacyPackaging = false
    }
  }
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  
  // Eliminamos Gemini SDK y Ktor para usar Retrofit directo (más estable)

  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.play.services.location)
  // implementation(libs.maps.compose)
  implementation(libs.osmdroid.android)
  implementation(libs.androidx.preference.ktx)
  implementation(libs.retrofit)
  implementation(libs.androidx.biometric)
  implementation(libs.androidx.security.crypto)
  implementation(libs.androidx.appcompat)
  implementation(libs.material)
  implementation(libs.sqlcipher.android)
  
  // Librería para leer coordenadas de las fotos
  implementation("androidx.exifinterface:exifinterface:1.3.7")

  // Librería para gráficos de altitud (Canvas puro, muy ligera)
  implementation("com.github.philjay:mpandroidchart:v3.1.0")

  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)
  kspAndroidTest(libs.hilt.compiler)
  implementation(libs.androidx.hilt.navigation.compose)
  testImplementation(libs.mockk)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  androidTestImplementation(libs.hilt.android.testing)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
  coreLibraryDesugaring(libs.desugar.jdk.libs)
}

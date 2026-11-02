// OpenCV 5.0.0 Android SDK (16 KB page-size build) packaged as a local library module.
// Only the Java wrappers used by Camera Tools plus the arm64-v8a / armeabi-v7a native libraries are included.
plugins {
  alias(libs.plugins.android.library)
}

android {
  namespace = "org.opencv"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    minSdk = 24
    consumerProguardFiles("consumer-rules.pro")
    ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }

  // libopencv_java5.so links against libc++_shared.so. The SDK ships a tiny CMake target whose only job is to make
  // Gradle package it. If you drop libc++_shared.so from your NDK into src/main/jniLibs/<abi>/ the CMake step is skipped
  // and no NDK/CMake install is needed on the build machine.
  val stlProvided = file("src/main/jniLibs/arm64-v8a/libc++_shared.so").exists() &&
    file("src/main/jniLibs/armeabi-v7a/libc++_shared.so").exists()
  if (!stlProvided) {
    externalNativeBuild { cmake { path = file("libcxx_helper/CMakeLists.txt") } }
    defaultConfig {
      externalNativeBuild {
        cmake {
          arguments("-DANDROID_STL=c++_shared")
          targets("opencv_jni_shared")
        }
      }
    }
  }

  packaging { jniLibs { keepDebugSymbols += "**/*.so" } }
}

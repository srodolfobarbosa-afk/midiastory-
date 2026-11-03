import java.io.File
import java.util.Properties
import org.gradle.api.GradleException
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val uploadPropertiesFile = rootProject.file("keystore.properties")
val uploadProperties = Properties()
if (uploadPropertiesFile.isFile) {
    uploadPropertiesFile.inputStream().use { stream -> uploadProperties.load(stream) }
}

fun resolveSigningValue(propKey: String, envKey: String): String? {
    return uploadProperties.getProperty(propKey)?.takeIf { it.isNotBlank() }
        ?: (project.findProperty(propKey) as? String)?.takeIf { it.isNotBlank() }
        ?: System.getenv(envKey)?.takeIf { it.isNotBlank() }
}

val resolvedStorePath = resolveSigningValue("storeFile", "SWEEP_STORE_FILE")
val resolvedStorePassword = resolveSigningValue("storePassword", "SWEEP_STORE_PASSWORD")
val resolvedKeyAlias = resolveSigningValue("keyAlias", "SWEEP_KEY_ALIAS")
val resolvedKeyPassword = resolveSigningValue("keyPassword", "SWEEP_KEY_PASSWORD")

fun locateStoreFile(path: String?): File? {
    if (path.isNullOrBlank()) return null
    val candidates = listOf(
        project.file(path),
        rootProject.file(path),
        File(path),
        File(rootProject.projectDir.parentFile, path),
    )
    return candidates.firstOrNull { it.isFile }
}

val uploadStoreFile: File? = locateStoreFile(resolvedStorePath)
val uploadSigningReady = uploadStoreFile != null &&
    !resolvedStorePassword.isNullOrBlank() &&
    !resolvedKeyAlias.isNullOrBlank() &&
    !resolvedKeyPassword.isNullOrBlank()

android {
    namespace = "com.rdf112.sweep"
    compileSdk = 36

    defaultConfig {
        // Atualização do app existente na Google Play: preserva com.rdf112.sweep
        applicationId = "com.rdf112.sweep"
        minSdk = 29
        targetSdk = 36
        versionCode = 14
        versionName = "2.1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    signingConfigs {
        if (uploadSigningReady) {
            create("upload") {
                storeFile = requireNotNull(uploadStoreFile)
                storePassword = resolvedStorePassword
                keyAlias = resolvedKeyAlias
                keyPassword = resolvedKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            // Ativa R8 para eliminar o aviso de tabela de símbolos de desobfuscação na Google Play Console
            isMinifyEnabled = true
            isShrinkResources = false
            if (uploadSigningReady) {
                signingConfig = signingConfigs.getByName("upload")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":domain"))
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("com.google.android.material:material:1.13.0")
    implementation("com.android.billingclient:billing-ktx:8.0.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:core-ktx:1.6.1")
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val localCatalog = providers.gradleProperty("catalogDir").orNull?.let { path ->
    rootProject.file(path).also { directory ->
        require(directory.resolve("catalog.json").isFile) {
            "catalogDir must point to an exported catalogue directory"
        }
    }
}

android {
    namespace = "app.championguides"
    compileSdk = 37

    defaultConfig {
        applicationId = "app.championguides"
        minSdk = 28
        targetSdk = 36
        versionCode = 2
        versionName = "0.2.0"
        if (localCatalog != null) {
            applicationIdSuffix = ".local"
            versionNameSuffix = "-local"
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

if (localCatalog != null) {
    android.sourceSets.getByName("main").assets.setSrcDirs(listOf(localCatalog))
    androidComponents.beforeVariants(androidComponents.selector().withBuildType("release")) { variant ->
        variant.enable = false
    }
    tasks.withType<Test>().configureEach {
        inputs.dir(localCatalog)
        systemProperty("catalog.assetsDir", localCatalog.absolutePath)
    }
}

ksp {
    arg("room.schemaLocation", layout.projectDirectory.dir("schemas").asFile.path)
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.json)
}

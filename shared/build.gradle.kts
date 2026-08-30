import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.kotlinSerialization)
}

compose.resources {
    // AndroidCareNotificationScheduler lives in :androidApp (it needs that module's own R class
    // and MainActivity — see its class doc) but still resolves shared notification copy from here,
    // so the generated Res accessor needs to be visible outside this module.
    publicResClass = true
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    
    android {
       namespace = "org.mlanau.project.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
    
       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
       withDeviceTestBuilder {
           sourceSetTreeName = "test"
       }.configure {
           instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
       }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.koin.android)
            implementation(libs.sqldelight.android.driver)
            implementation(libs.kotlinx.datetime)
            implementation(libs.androidx.activity.compose)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.ui.backhandler)
            implementation(libs.compose.components.resources)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.compose.icons.extended)
            implementation(libs.kotlinx.datetime)
            
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.no.arg)
            implementation(libs.multiplatform.settings.coroutines)

            implementation(libs.sqldelight.coroutines.extensions)

            // Coil (compose-only: every image is local, there's no network image loading)
            implementation(libs.coil.compose)

            implementation(libs.peekaboo.ui)
            implementation(libs.peekaboo.image.picker)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
            implementation(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

dependencies {
}

sqldelight {
    databases {
        create("PlantDb") {
            packageName.set("org.mlanau.project.plant.infrastructure.persistence")
            verifyMigrations.set(true)
            // Must live inside the sqldelight source folder (not build/): VerifyMigrationTask looks
            // for the versioned .db snapshot among the .sq/.sqm source folders, not in build outputs.
            // This snapshot is meant to be checked into version control alongside the migrations.
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
            // Pre-release policy: the app isn't published yet, so the schema lives entirely in
            // PlantDb.sq and there are no .sqm migrations. Edit PlantDb.sq directly and clear the
            // local database (reinstall / adb shell pm clear) after a schema change instead of
            // writing a migration. Once the app ships, switch back to adding a numbered .sqm (with
            // its checked-in schema snapshot) for every schema change, since real installs will need
            // to upgrade in place.
        }
    }
}

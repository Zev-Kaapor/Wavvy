// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

// Redirect build files to a folder outside OneDrive
allprojects {
    layout.buildDirectory.set(file("C:/temp-android-builds/${rootProject.name}/${project.name}"))
}

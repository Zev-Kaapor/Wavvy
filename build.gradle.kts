// Top-level build file
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

allprojects {
    layout.buildDirectory.set(file("C:/temp-android-builds/${rootProject.name}/${project.name}"))
}

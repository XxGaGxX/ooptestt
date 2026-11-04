plugins {
    java
    application
}

val jme3Version = "3.8.0-stable"

repositories {
    mavenCentral()
}

dependencies {
    // Core di jMonkeyEngine
    implementation("org.jmonkeyengine:jme3-core:$jme3Version")
    implementation("org.jmonkeyengine:jme3-desktop:$jme3Version")
    implementation("org.jmonkeyengine:jme3-lwjgl3:$jme3Version")
    implementation("org.jmonkeyengine:jme3-testdata:$jme3Version")
    // Fisica Bullet (sostituisce jme3-bullet e jme3-bullet-native)
    implementation("com.github.stephengold:Minie:7.7.0")

    implementation("org.jmonkeyengine:jme3-plugins:$jme3Version")
}

java { toolchain { languageVersion.set(JavaLanguageVersion.of(25)) } }
application { mainClass.set("org.example.Main") }
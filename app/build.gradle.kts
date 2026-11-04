plugins {
    java
    application
}

repositories { mavenCentral() }

val jme = "3.8.0-stable"
dependencies {
    implementation("org.jmonkeyengine:jme3-core:$jme")
    implementation("org.jmonkeyengine:jme3-desktop:$jme")
    implementation("org.jmonkeyengine:jme3-lwjgl3:$jme")
    implementation("org.jmonkeyengine:jme3-plugins:$jme") // GltfLoader (.glb)

    testImplementation("junit:junit:4.13.2")
}

java { toolchain { languageVersion.set(JavaLanguageVersion.of(25)) } }
application { mainClass.set("org.example.Main") }
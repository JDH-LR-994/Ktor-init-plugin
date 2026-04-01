plugins {
    id("java")
    kotlin("jvm") version "2.2.20"
    id("org.jetbrains.intellij.platform") version "2.13.1"
}

group = "com.likhodievskii"
version = "1.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}


dependencies {
    implementation("com.squareup.okhttp3:okhttp:5.3.2")
    implementation("tools.jackson.core:jackson-databind:3.1.0")
    implementation("tools.jackson.module:jackson-module-kotlin:3.1.0")
    implementation(kotlin("stdlib-jdk8"))

    intellijPlatform {
        local("/home/andrey/Editors/openIDE-253.28294.334.2") // Путь до IDE (использовалось при разработке)
    }
}

intellijPlatform {
    instrumentCode = false
    pluginConfiguration {
        id = "com.likhodievskii.ktor-init"
        name = "Ktor init"
        version = project.version.toString()
        changeNotes = """
            <ul>
              <li>1.0 - релиз первой версии плагина</li>
            </ul>
        """.trimIndent()
    }
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
}
kotlin {
    jvmToolchain(21)
}
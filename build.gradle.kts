plugins {
    java
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

group = "top.mrxiaom"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://maven.fastmirror.net/repositories/minecraft")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    // Minecraft
    val mcVersion = "1.19.4"
    compileOnly("org.spigotmc:spigot-api:$mcVersion-R0.1-SNAPSHOT")

    // Dependency Plugins
    compileOnly("me.clip:placeholderapi:2.11.5")

    // Shadow Dependency
    implementation("org.jetbrains:annotations:19.0.0")
}

val targetJavaVersion = 8
java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
    }
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        val p = "top.mrxiaom.minebbsreward.utils"
        relocate("org.intellij.lang.annotations", "$p.annotations.intellij")
        relocate("org.jetbrains.annotations", "$p.annotations.jetbrains")
    }
    build {
        dependsOn(shadowJar)
    }
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible) {
            options.release.set(targetJavaVersion)
        }
    }
    processResources {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        from(sourceSets.main.get().resources.srcDirs) {
            expand(mapOf(
                "version" to version,
            ))
            include("plugin.yml")
        }
    }
}

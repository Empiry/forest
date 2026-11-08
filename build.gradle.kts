plugins {
    alias(libs.plugins.jvm)
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        url = uri("https://repo.aikar.co/content/groups/aikar/")
    }

    // ignite in local repo for rn
    mavenLocal()
}

dependencies {
    compileOnly(libs.ignite)
    compileOnly(libs.hackscommon)
    // paper api
    compileOnly("io.papermc.paper:paper-api:26.3.build.+")
    // adventure for Bukkit
    compileOnly("net.kyori:adventure-platform-bukkit:4.4.1")
    // commands
    compileOnly("co.aikar:acf-paper:0.5.1-SNAPSHOT")
    // lin alg
    compileOnly("org.joml:joml:1.10.8")
}

// Java 25
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

plugins {
    java
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

repositories{
    mavenCentral()

    // Placeholder API
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")

    // Dynmap & QuickShop
    maven("https://repo.codemc.io/repository/maven-public/")

    // ProtocolLib
    maven("https://repo.dmulloy2.net/repository/public/")

    // QuickShop - dependencies require this
    maven("https://repo.onarandombox.com/content/groups/public")
    maven("https://repo.minebench.de/")

    // ChestShop
    maven("https://repo.minebench.de/chestshop-repo")

    // DiscordSRV - dependencies require this
    maven("https://m2.dv8tion.net/releases")
    maven("https://nexus.scarsz.me/content/groups/public/")

    // Vault, BlueMap
    maven("https://jitpack.io")
}

dependencies{
    compileOnly("org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT")
    compileOnly("org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT")
    compileOnly("org.spigotmc:spigot-1.17.1-R0.1-SNAPSHOT-remapped")
    compileOnly("org.spigotmc:spigot-1.16.5-R0.1-SNAPSHOT")
    compileOnly("com.comphenix.protocol:ProtocolLib:5.0.0")
    compileOnly("org.maxgamer:QuickShop:5.1.2.2-SNAPSHOT")
    compileOnly("com.acrobot.chestshop:chestshop:3.12")
    compileOnly("org.dynmap:dynmap:3.4-beta-2-spigot")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
    compileOnly("com.discordsrv:discordsrv:1.25.1")
    compileOnly("me.clip:placeholderapi:2.11.2")
    compileOnly("net.luckperms:api:5.4")
    compileOnly("com.github.BlueMap-Minecraft:BlueMapAPI:v2.6.0")

    implementation("org.xerial:sqlite-jdbc:3.40.1.0")
    implementation(project(":api"))
}

tasks {
    shadowJar {
        archiveBaseName.set("Konquest")
        archiveClassifier.set("")
        destinationDirectory.set(file("$rootDir/build/libs"))

        dependencies {
            include(project(":api"))
        }
    }

    build {
        dependsOn(shadowJar)
    }

    processResources {
        filter<org.apache.tools.ant.filters.ReplaceTokens>("tokens" to mapOf("version" to project.version))
    }
}

java{
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
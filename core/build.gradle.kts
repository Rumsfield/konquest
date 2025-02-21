plugins {
    java
    id("com.github.johnrengelman.shadow") version "7.1.2"
    `maven-publish`
}

repositories{
    mavenCentral()

    // WorldEdit & WorldGuard
    maven("https://maven.enginehub.org/repo/")

    // Placeholder API
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")

    // Dynmap
    maven("https://repo.mikeprimm.com/")

    // QuickShop
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

    // EssentialsX
    maven("https://repo.essentialsx.net/releases/")
    maven("https://papermc.io/repo/repository/maven-public/")
}

dependencies{
    // Spigot
    compileOnly("org.spigotmc:spigot-api:1.21.4-R0.1-SNAPSHOT") // Primary API
    compileOnly("org.spigotmc:spigot-1.17.1-R0.1-SNAPSHOT-remapped") // for nms packets, local lib
    compileOnly("org.spigotmc:spigot-1.16.5-R0.1-SNAPSHOT") // for nms packets, local lib

    // Integrated third-party plugins
    compileOnly("com.comphenix.protocol:ProtocolLib:5.0.0")
    compileOnly("com.github.jikoo.OpenInv:openinvapi:4.3.1")
    compileOnly("net.kyori:adventure-api:4.17.0")
    compileOnly("com.ghostchu:quickshop-common:6.2.0.6")
    compileOnly("com.ghostchu:quickshop-api:6.2.0.6")
    compileOnly("com.acrobot.chestshop:chestshop:3.12")
    compileOnly("us.dynmap:DynmapCoreAPI:3.6")
    compileOnly("us.dynmap:dynmap-api:3.6")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
    compileOnly("com.discordsrv:discordsrv:1.29.0")
    compileOnly("me.clip:placeholderapi:2.11.2")
    compileOnly("net.luckperms:api:5.4")
    compileOnly("com.github.BlueMap-Minecraft:BlueMapAPI:v2.6.0")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.2.15")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.5")
    compileOnly("net.essentialsx:EssentialsX:2.20.1")
    compileOnly("xyz.jpenilla:squaremap-api:1.3.4")

    implementation("org.apache.commons:commons-lang3:3.14.0")
    implementation("org.xerial:sqlite-jdbc:3.41.2.2")
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

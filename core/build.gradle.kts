plugins {
    java
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

repositories{
    mavenCentral()
    //Placeholder API
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")

    //Dynmap
    maven("https://repo.codemc.io/repository/maven-public/")

    // QuickShop - dependencies require this
    maven("https://repo.onarandombox.com/content/groups/public")
    maven("https://repo.minebench.de/")

    //DiscordSRV - dependencies require this
    maven("https://m2.dv8tion.net/releases")
    maven("https://nexus.scarsz.me/content/groups/public/")

    //Vault
    maven("https://jitpack.io")
}

dependencies{
    compileOnly("org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT")
    compileOnly("org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT")
    compileOnly("org.spigotmc:spigot-1.17.1-R0.1-SNAPSHOT-remapped")
    compileOnly("org.spigotmc:spigot-1.16.5-R0.1-SNAPSHOT")
    compileOnly("com.comphenix.protocol:ProtocolLib:4.7.0")
    compileOnly("org.maxgamer:QuickShop:5.1.1.2-SNAPSHOT")
    compileOnly("org.dynmap:dynmap:3.4-beta-2-spigot")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
    compileOnly("com.discordsrv:discordsrv:1.25.1")
    compileOnly("me.clip:placeholderapi:2.11.2")
    compileOnly("net.luckperms:api:5.4")

    implementation("org.xerial:sqlite-jdbc:3.40.0.0")
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
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

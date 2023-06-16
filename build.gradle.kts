allprojects {
    group = "com.github.rumsfield.konquest"
    version = "0.11.2"
}

subprojects {
    repositories {
        mavenCentral()
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        flatDir {
            dirs("$rootDir/lib")
        }
    }
}

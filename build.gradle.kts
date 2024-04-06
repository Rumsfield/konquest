allprojects {
    group = "com.github.rumsfield.konquest"
    version = "1.4.3"
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

allprojects {
    group = "com.github.rumsfield.konquest"
    version = "1.5.0"
}

subprojects {
    repositories {
        mavenCentral()
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://oss.sonatype.org/content/repositories/central")
        flatDir {
            dirs("$rootDir/lib")
        }
    }
}

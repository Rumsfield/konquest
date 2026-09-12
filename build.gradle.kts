allprojects {
    group = "com.github.rumsfield.konquest"
    version = "1.10.1"
}

subprojects {
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://oss.sonatype.org/content/repositories/central")
        flatDir {
            dirs("$rootDir/lib")
        }
    }
}

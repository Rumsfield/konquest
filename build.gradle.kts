allprojects {
    group = "com.github.rumsfield.konquest"
    version = "1.2.0"
}

subprojects {
    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    repositories {
        mavenCentral()
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        flatDir {
            dirs("$rootDir/lib")
        }
    }
}

plugins {
    java
    id("com.github.johnrengelman.shadow") version "7.1.2"
    `maven-publish`
}

dependencies{
    compileOnly("org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT")
}

tasks {
    shadowJar {
        archiveBaseName.set(rootProject.name+"-"+project.name)
        archiveClassifier.set("")
        destinationDirectory.set(file("$rootDir/build/libs"))
    }

    register<Javadoc>("generateJavadoc"){
        source = sourceSets.main.get().allJava
        classpath += project.configurations.getByName("compileClasspath").asFileTree
        title = "Konquest ${project.version} Documentation"
        options.overview("overview.html")
        setDestinationDir(file("$rootDir/docs"))
    }
}

java{
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = rootProject.name+"-"+project.name
            version = project.version.toString()
            from(components["java"])
        }
    }
}

plugins {
    java
    `maven-publish`
}

dependencies{
    compileOnly("org.spigotmc:spigot-api:1.21.1-R0.1-SNAPSHOT") // Primary API
}

tasks {

    jar{
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

    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            groupId = project.group.toString()
            artifactId = rootProject.name+"-"+project.name
            version = project.version.toString()
        }
    }
    repositories{
        mavenLocal()
    }
}

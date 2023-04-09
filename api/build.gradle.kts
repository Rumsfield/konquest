plugins {
    java
    id("com.github.johnrengelman.shadow") version "7.1.2"
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
        title = rootProject.name+" "+project.version+" Documentation"
        options.overview("overview.html")
        setDestinationDir(file("$rootDir/doc"))
    }
}

java{
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
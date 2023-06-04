# Konquest
Konquest is a plugin for land claiming and territory control.

## For Users
All details can be found on the [wiki](https://github.com/Rumsfield/konquest-doc/wiki).

## For Developers
Here are a few helpful links.
* [API Javadoc](https://rumsfield.github.io/konquest-doc/)
* [Contributing Guide](./CONTRIBUTING.md)

### Project Layout
This project uses the IntelliJ IDEA with Gradle. There are the following sub-projects:
* [`api`](./api) - Public interfaces for API use
* [`core`](./core) - All plugin source code

The [`doc`](./doc) folder contains auto-generated Javadoc files.

### Building
Konquest uses Gradle for most dependencies, building and generating Javadoc.
* Use Java 8 JDK or newer
* Requires Git

Use these commands to build from source:
```
git clone https://github.com/Rumsfield/konquest.git
cd Konquest/
./gradlew shadowJar
```
Build outputs will be placed in the `build/libs/` folder.

To generate Javadoc, use this command:
```
./gradlew generateJavadoc
```
The new Javadoc will overwrite the contents of the `doc/` folder.

To clean the project and remove stale build files, use this command:
```
./gradlew clean
```

## License
Konquest is licensed under the GNU GPLv3 license. See [`LICENSE.txt`](./LICENSE.txt) for more info.
# Oraculum

This is the main application for the Oraculum project.

## Running the Application

This application uses DuckDB for high-speed data processing, which requires access to native libraries. When running the application with a modern JDK (17+), you must provide a specific JVM flag to grant the necessary permissions.

### JVM Arguments

To avoid warnings and ensure future compatibility, run the application with the following JVM argument:

```shell
--enable-native-access=ALL-UNNAMED
```

### Example with Docker

If you are running the application from a JAR file inside a Docker container, you would add the flag to your `java` command in the `Dockerfile`:

```dockerfile
# Example Dockerfile entrypoint
ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-jar", "/app.jar"]
```

### Example with Maven

To run the application directly using the Spring Boot Maven plugin, you can pass the argument like this:

```shell
mvn spring-boot:run -Dspring-boot.run.jvmArguments="--enable-native-access=ALL-UNNAMED"
```

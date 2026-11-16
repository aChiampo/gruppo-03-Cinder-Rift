# syntax=docker/dockerfile:1

# Compile with the Java 21 JDK on Alpine.
FROM eclipse-temurin:21-jdk-alpine AS build

# Keep the Maven project and its build output in one directory.
WORKDIR /workspace

# Use the repository's Maven Wrapper configuration and project descriptor.
COPY .mvn/ .mvn/
COPY pom.xml ./

# Make the wrapper executable inside Linux. Git enforces LF for mvnw.
COPY --chmod=0755 mvnw ./

# Include the application and test sources for Maven packaging.
COPY src/ src/

# Reuse wrapper and dependency downloads across builds. Run tests separately.
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    ./mvnw -B -ntp -DskipTests package

# Run the packaged application with a Java 21 JRE on Alpine.
FROM eclipse-temurin:21-jre-alpine

# Install curl for health checks and assign fixed IDs to the runtime account.
RUN apk add --no-cache curl \
    && addgroup -g 10001 spring \
    && adduser -D -H -u 10001 -G spring spring

# Use a fixed working directory for the packaged application.
WORKDIR /app

# Select the executable JAR by its current Maven artifact ID and version.
# Keep the JAR owned by root so the runtime account cannot modify it.
COPY --from=build /workspace/target/app-0.0.1-SNAPSHOT.jar /app/app.jar

# Run Java and the health probe with the unprivileged account.
USER 10001:10001

# Declare the container port configured in application.properties.
EXPOSE 8081

# Provide a health probe for direct Docker runs. Compose supplies its own check.
HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=10 \
    CMD ["curl", "--fail", "--silent", "--show-error", "http://localhost:8081/actuator/health"]

# Launch Java directly so it receives Docker's stop signal.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY pom.xml ./
COPY --chmod=0755 mvnw ./
COPY src/ src/

# Run the JUnit suite separately; keep dependency downloads between builds.
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    ./mvnw -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl \
    && addgroup -g 10001 spring \
    && adduser -D -H -u 10001 -G spring spring
WORKDIR /app

# The POM's exec classifier distinguishes this from the non-executable library JAR.
COPY --from=build /workspace/target/game_engine-0.0.1-SNAPSHOT-exec.jar /app/app.jar

ENV SERVER_PORT=8083 \
    GAME_ENGINE_CLI_ENABLED=false
USER 10001:10001
EXPOSE 8083
HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=10 \
    CMD ["curl", "--fail", "--silent", "--show-error", "http://localhost:8083/actuator/health"]
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

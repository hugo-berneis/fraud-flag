# Build stage: has the full JDK + Gradle wrapper, produces the bootJar.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY gradlew gradlew.bat ./
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts ./
RUN ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

COPY src ./src
RUN ./gradlew --no-daemon bootJar

# Runtime stage: just a JRE and the jar, no build tooling, no source.
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S fraudflag && adduser -S fraudflag -G fraudflag
COPY --from=build /app/build/libs/*.jar app.jar
RUN chown fraudflag:fraudflag app.jar
USER fraudflag

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

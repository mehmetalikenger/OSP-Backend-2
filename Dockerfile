# syntax=docker/dockerfile:1

# ---- Build stage: compile + package the fat jar with JDK 25 ----
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# The Maven wrapper is "only-script" type — it downloads Maven at runtime,
# which needs curl (to fetch) and unzip (to extract). Not present in the base image.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl unzip \
    && rm -rf /var/lib/apt/lists/*

# Copy the wrapper + pom first so this layer is cached unless they change.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

# Now the sources, then build (tests skipped — run them in CI, not the image build).
COPY src/ src/
RUN ./mvnw -B -DskipTests clean package

# ---- Run stage: slim JRE image with just the jar ----
FROM eclipse-temurin:25-jre AS run
WORKDIR /app

# Spring Boot repackages a single executable jar; *.jar matches it (not the .original).
COPY --from=build /app/target/*.jar app.jar

# Documentation only — Spring binds to ${PORT:8080}, and Railway injects PORT.
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

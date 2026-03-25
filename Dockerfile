# ─────────────────────────────────────────────
# Stage 1: Build the app with Maven
# ─────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS builder

# Set working directory inside the container
WORKDIR /app

# Copy pom.xml first so Maven can download dependencies (Docker cache layer)
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Copy the rest of the source code
COPY src ./src

# Build the project, skip tests for faster Docker builds
RUN mvn -B -q clean package -DskipTests

# ─────────────────────────────────────────────
# Stage 2: Run the app with a slim Java image
# ─────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

# Set working directory
WORKDIR /app

# Copy the built JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose the application port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

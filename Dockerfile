# ---- Stage 1: Build ----
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and pom first (layer caching)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Download dependencies (cached unless pom.xml changes)
RUN ./mvnw dependency:go-offline --batch-mode

# Copy source code and build the JAR
COPY src/ src/
RUN ./mvnw clean package -DskipTests --batch-mode

# ---- Stage 2: Run ----
FROM eclipse-temurin:21-jre-alpine AS runner

WORKDIR /app

# Security: run as non-root user
RUN addgroup -S spring && adduser -S spring -G spring

# Create uploads directory (for song/cover file storage)
RUN mkdir -p /app/uploads/songs /app/uploads/covers && \
    chown -R spring:spring /app/uploads

USER spring:spring

# Copy the built JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Default port for the backend
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/swagger-ui.html || exit 1

# JVM flags optimized for containers
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]

# ──────────────────────────────────────────────────────────────────────────────
#  Stage 1 — Build (Maven + JDK 21)
# ──────────────────────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache dependencies first (layer caching optimization)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# ──────────────────────────────────────────────────────────────────────────────
#  Stage 2 — Run (JRE 21 only — minimal attack surface)
# ──────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre
WORKDIR /app

# Security: run as non-root user
RUN groupadd -r appuser && useradd -r -g appuser appuser

COPY --from=build /app/target/*.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

USER appuser
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

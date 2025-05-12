# -------- Stage 1: Build --------
FROM maven:3.8.6-openjdk-17 AS builder

# Arbeitsverzeichnis im Container
WORKDIR /workspace

# Nur pom.xml kopieren, um Abhängigkeiten zwischenzu­cachen
COPY pom.xml .

# Abhängigkeiten herunterladen
RUN mvn dependency:go-offline -B

# Quellcode kopieren und bauen
COPY src ./src
RUN mvn clean package -DskipTests -B

# -------- Stage 2: Runtime --------
FROM eclipse-temurin:17-jre-jammy

# Arbeitsverzeichnis für die App
WORKDIR /app

# Nur das fertige Jar aus dem Builder holen
COPY --from=builder /workspace/target/*.jar app.jar

# Port, auf dem der Spring‐Boot‐Embedded‐Tomcat hört
EXPOSE 8080

# Default‐Kommando
ENTRYPOINT ["java", "-jar", "app.jar"]
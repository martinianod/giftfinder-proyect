# ===== RUNTIME =====
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the pre-built JAR from local build
COPY build/libs/*-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]

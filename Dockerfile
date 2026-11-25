# Stage 1: Build Spring Boot JAR with Maven + JDK 17
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Lightweight JRE 17 runtime tuned for Render 512MB RAM
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/UCOP.jar app.jar
EXPOSE 8088
ENTRYPOINT ["java", "-Xms128m", "-Xmx384m", "-Dfile.encoding=UTF-8", "-Dspring.profiles.active=render", "-jar", "app.jar"]

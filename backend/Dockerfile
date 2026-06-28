# --- Build stage: compile with the Maven Wrapper (no global Maven needed) ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Cache dependencies first.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

# Build the application.
COPY src ./src
RUN ./mvnw -B -q clean package -DskipTests

# --- Runtime stage: slim JRE ------------------------------------------------
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

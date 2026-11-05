# ==========================================
# STAGE 1: Build the React Dashboard & Java App
# ==========================================
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy everything (pom.xml, src, dashboard)
COPY . .

# Maven will automatically run the frontend-maven-plugin,
# build the React app, put it in /static, and then compile Java!
RUN mvn clean package -DskipTests

# ==========================================
# STAGE 2: The Tiny Runtime Image
# ==========================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy ONLY the compiled JAR from the build stage
COPY --from=build /app/target/*.jar gateway.jar

# Expose the Gateway port
EXPOSE 8080

# Run the JAR
ENTRYPOINT ["java", "-jar", "gateway.jar"]
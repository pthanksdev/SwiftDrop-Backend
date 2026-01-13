# Use official Maven image to build
FROM maven:3.9-eclipse-temurin-17 AS build

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Use lightweight JRE for runtime
FROM eclipse-temurin:17-jre

# Set working directory
WORKDIR /app

# Copy the JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port (Render will override this with $PORT)
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "app.jar"]
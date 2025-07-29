# Use OpenJDK 21 slim image and install Maven
FROM openjdk:21-jdk-slim AS build

# Install Maven
RUN apt-get update && \
    apt-get install -y maven && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /build

# Copy pom.xml first to leverage Docker layer caching
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application and create JAR
RUN mvn clean package -DskipTests

# Use JRE image for runtime
FROM openjdk:21-jre-slim

# Set working directory
WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /build/target/jobportal-1.0-SNAPSHOT.jar jobportal.jar

# Expose port 8080
EXPOSE 8080

# Set JVM options for containerized environment
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Run the application using the JAR
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar jobportal.jar"]
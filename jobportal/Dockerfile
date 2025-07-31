FROM openjdk:24-jdk-slim AS build

# Install Maven
RUN apt-get update && \
    apt-get install -y maven && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy Maven files
COPY pom.xml .
COPY src ./src

# Build the JAR
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:24-jre-slim

# Copy the JAR from build stage
COPY --from=build /app/target/jobportal-*.jar jobportal.jar

# Run the application
ENTRYPOINT ["java", "-jar", "jobportal.jar"]
FROM openjdk:21-jdk-slim

# Copy the built jar file from target directory into the image
COPY target/bookmanagement-1.0-SNAPSHOT.jar bookapp.jar

# Expose default Spring Boot port
EXPOSE 8080

# Run the jar
ENTRYPOINT ["java", "-jar", "bookapp.jar"]

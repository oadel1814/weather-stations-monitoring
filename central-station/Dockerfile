# Use the same Java version you developed with
FROM amazoncorretto:23

# Set the working directory inside the container
WORKDIR /app

# Copy the Fat Jar we just built
COPY target/*-jar-with-dependencies.jar app.jar

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
FROM amazoncorretto:23
WORKDIR /app
COPY target/*-jar-with-dependencies.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
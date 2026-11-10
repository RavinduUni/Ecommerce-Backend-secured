FROM amazoncorretto:21-alpine-jdk
COPY target/ECommerceApp.jar ECommerceApp.jar
EXPOSE  8080
ENTRYPOINT ["java", "-jar", "job-app.jar"]
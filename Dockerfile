FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
ARG SERVICE_NAME
COPY ${SERVICE_NAME}/target/*[!-original].jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
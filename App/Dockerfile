FROM gradle:8.7-jdk21-alpine AS build
WORKDIR /home/gradle/src

COPY --chown=gradle:gradle . .

RUN gradle clean build --no-daemon -x test

FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY --from=build /home/gradle/src/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
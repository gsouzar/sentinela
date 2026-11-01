FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src src
RUN mvn -B package -DskipTests
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S sentinela && adduser -S sentinela -G sentinela
WORKDIR /app
COPY --from=build /src/target/sentinela-api-1.0.0.jar app.jar
USER sentinela
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]

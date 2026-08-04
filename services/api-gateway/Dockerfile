FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
COPY services/common/pom.xml services/common/pom.xml
COPY services/api-gateway/pom.xml services/api-gateway/pom.xml
RUN mvn -q dependency:go-offline -B -pl services/api-gateway -am

COPY . .
RUN mvn -q package -pl services/api-gateway -am -DskipTests

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S medikit && adduser -S medikit -G medikit
WORKDIR /app
COPY --from=build /app/services/api-gateway/target/*.jar app.jar
USER medikit
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]

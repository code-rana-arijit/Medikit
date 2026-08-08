FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
COPY services/common/pom.xml services/common/pom.xml
COPY services/loyalty-service/pom.xml services/loyalty-service/pom.xml
RUN mvn -q dependency:go-offline -B -pl services/loyalty-service -am

COPY . .
RUN mvn -q package -pl services/loyalty-service -am -DskipTests

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S medikit && adduser -S medikit -G medikit
WORKDIR /app
COPY --from=build /app/services/loyalty-service/target/*.jar app.jar
USER medikit
EXPOSE 8112
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]

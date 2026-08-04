FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY services/common/pom.xml services/common/pom.xml
COPY services/prescription-service/pom.xml services/prescription-service/pom.xml
RUN mvn -q dependency:go-offline -B
COPY . .
RUN mvn -q package -pl services/prescription-service -am -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/services/prescription-service/target/*.jar app.jar
EXPOSE 8109
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]

FROM maven:3.9.6-eclipse-temurin-8 AS build
WORKDIR /workspace
COPY . .
RUN mvn -B -pl order-service -am -DskipTests clean package

FROM eclipse-temurin:8-jre-jammy
WORKDIR /app
COPY --from=build /workspace/order-service/target/order-service-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]

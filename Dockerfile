
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY . .
RUN chmod +x ./mvnw && ./mvnw -B clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build /workspace/target/ExpensesTracker-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 10000

ENTRYPOINT ["sh", "-c", "exec java -Dserver.port=${PORT:-10000} -jar app.jar"]

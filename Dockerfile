
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY . .
RUN mkdir -p /root/.m2 && \
    printf '%s\n' \
      '<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0">' \
      '  <mirrors>' \
      '    <mirror>' \
      '      <id>central-alternate</id>' \
      '      <mirrorOf>central</mirrorOf>' \
      '      <url>https://repo1.maven.org/maven2</url>' \
      '    </mirror>' \
      '  </mirrors>' \
      '</settings>' \
      > /root/.m2/settings.xml && \
    mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build /workspace/target/ExpensesTracker-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 10000

ENTRYPOINT ["sh", "-c", "exec java -Dserver.port=${PORT:-10000} -jar app.jar"]

FROM maven:3.9.9-eclipse-temurin-11 AS build
WORKDIR /build

COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
COPY sql ./sql
COPY docs ./docs
COPY README.md .
RUN mvn -q -Dmaven.test.skip=true package

FROM eclipse-temurin:11-jre
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends chromium fonts-noto-cjk \
    && rm -rf /var/lib/apt/lists/*

ENV MCP_BROWSER_PATH=/usr/bin/chromium

COPY --from=build /build/target/AI-LiuYao-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

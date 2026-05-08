# syntax=docker/dockerfile:1.7
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY common-lib/pom.xml common-lib/
COPY config-server/pom.xml config-server/
COPY auth-service/pom.xml auth-service/
COPY api-gateway/pom.xml api-gateway/
COPY link-service/pom.xml link-service/
COPY analytics-service/pom.xml analytics-service/
COPY webhook-service/pom.xml webhook-service/
RUN chmod +x mvnw && ./mvnw -B -pl config-server -am dependency:go-offline -DskipTests

COPY config-server/src config-server/src
RUN ./mvnw -B -pl config-server -am package -DskipTests \
    && cp config-server/target/config-server-*.jar /workspace/app.jar

FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S app && adduser -S -G app app
WORKDIR /app
USER app
COPY --from=build /workspace/app.jar /app/app.jar
EXPOSE 8888
ENTRYPOINT ["java","-jar","/app/app.jar"]

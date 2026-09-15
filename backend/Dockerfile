FROM maven:3.9.11-eclipse-temurin-21-alpine AS build
WORKDIR /workspace
COPY pom.xml ./
COPY src ./src
RUN mvn -B -ntp clean package -DskipTests

FROM eclipse-temurin:25-jre-alpine
RUN apk upgrade --no-cache \
    && apk add --no-cache wget \
    && addgroup -S redclubes \
    && adduser -S redclubes -G redclubes
WORKDIR /app
COPY --from=build --chown=redclubes:redclubes /workspace/target/backend-*.jar app.jar
USER redclubes:redclubes
EXPOSE 8080
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/urandom", "-jar", "/app/app.jar"]

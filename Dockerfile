FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY backend/pom.xml backend/pom.xml
COPY backend/config backend/config
COPY backend/src backend/src
COPY frontend frontend
RUN mvn -q -f backend/pom.xml package -DskipTests

FROM eclipse-temurin:21-jre
RUN useradd --system --no-create-home screening
WORKDIR /app
COPY --from=build /src/backend/target/screening-loop-backend-0.1.0-SNAPSHOT.jar app.jar
USER screening
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]

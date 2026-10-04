FROM eclipse-temurin:21-jdk-noble AS build

WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN chmod +x mvnw
RUN ./mvnw -B -ntp dependency:go-offline

COPY src/ src/

RUN ./mvnw -B -ntp -DskipTests package


FROM eclipse-temurin:21-jre-noble AS runtime

WORKDIR /app

RUN groupadd --system spring \
    && useradd --system --gid spring spring

COPY --from=build --chown=spring:spring \
    /workspace/target/*.jar app.jar

USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
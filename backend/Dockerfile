# Etapa 1: compilar
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q clean package -DskipTests

# Etapa 2: ejecutar
FROM eclipse-temurin:21-jre
WORKDIR /app
# Usuario sin privilegios (no se ejecuta como root)
RUN useradd --system --uid 1001 appuser
COPY --from=build /app/target/*.jar app.jar
USER appuser
# En Docker siempre corre el perfil de producción
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]

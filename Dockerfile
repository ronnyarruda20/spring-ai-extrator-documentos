# 1. Frontend Angular: gera os arquivos estáticos dentro do backend
FROM node:22-alpine AS frontend
WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npx ng build

# 2. Backend Spring Boot: empacota o jar já com o frontend
FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /app/backend
COPY backend/.mvn .mvn
COPY backend/pom.xml .
RUN mvn -B -q dependency:go-offline
COPY backend/src src
COPY --from=frontend /app/backend/src/main/resources/static src/main/resources/static
RUN mvn -B -q -DskipTests package

# 3. Imagem final, só com o JRE
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 app
COPY --from=backend /app/backend/target/spring-ai-extrator-documentos-*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

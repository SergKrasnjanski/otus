# ── Stage 1: Загрузка зависимостей ────────────────────────────────────────────
# Копируем только pom.xml → этот слой кэшируется до следующего изменения pom.xml
FROM maven:3.9-eclipse-temurin-21-alpine AS deps
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B --no-transfer-progress

# ── Stage 2: Компиляция и распаковка JAR ──────────────────────────────────────
FROM deps AS builder
COPY src ./src
RUN mvn clean package -DskipTests -B --no-transfer-progress && \
    mkdir -p target/extracted && \
    cd target/extracted && \
    jar -xf ../jwt-api-0.0.1-SNAPSHOT.jar

# ── Stage 3: Runtime (distroless, non-root) ───────────────────────────────────
FROM gcr.io/distroless/java21-debian12:nonroot AS runtime
WORKDIR /app

# Копируем слои от наиболее стабильных к наиболее изменяемым:
# зависимости меняются редко → хорошо кэшируются
COPY --from=builder /build/target/extracted/BOOT-INF/lib/     BOOT-INF/lib/
COPY --from=builder /build/target/extracted/org/              org/
COPY --from=builder /build/target/extracted/META-INF/         META-INF/
# код приложения меняется часто → идёт последним
COPY --from=builder /build/target/extracted/BOOT-INF/classes/ BOOT-INF/classes/

EXPOSE 8080

# distroless:nonroot уже запускается от пользователя uid=65532 (nonroot)
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]

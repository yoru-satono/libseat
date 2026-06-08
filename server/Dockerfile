# ── Stage 1: build ──────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build

# 先只复制 pom.xml，利用层缓存避免每次重新下载依赖
COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn package -DskipTests -q

# ── Stage 2: run ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jre
WORKDIR /app

# 非 root 用户运行
RUN addgroup --system libseat && adduser --system --ingroup libseat libseat
USER libseat

COPY --from=build /build/target/libseat-server-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

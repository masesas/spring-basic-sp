# ============================================================================
#  Stage 1 — build jar
#
#  pom.xml disalin duluan dan dependency diunduh di layer terpisah, supaya
#  perubahan pada src/ tidak memaksa seluruh dependency diunduh ulang.
# ============================================================================
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

COPY pom.xml ./
RUN mvn -B -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests clean package

# ============================================================================
#  Stage 2 — runtime
#
#  JRE saja, tanpa Maven dan tanpa source code. Berjalan sebagai user tanpa
#  hak istimewa dengan UID tetap 1001 — UID-nya harus tetap karena direktori
#  volume di host dimiliki UID yang sama.
# ============================================================================
FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S -g 1001 app \
 && adduser  -S -u 1001 -G app app

WORKDIR /app

COPY --from=build /build/target/*.jar app.jar

RUN mkdir -p /app/resources/images \
 && chown -R app:app /app

USER app

EXPOSE 8080

# Endpoint /api/rolemap ada di whitelist SecurityConfig, jadi membalas 200 tanpa
# token. Balasan 200 membuktikan JVM hidup, port terbuka, dan konteks Spring
# selesai dimuat — aplikasi yang gagal me-resolve satu placeholder saja tidak
# akan pernah sampai ke titik ini.
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/api/rolemap || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

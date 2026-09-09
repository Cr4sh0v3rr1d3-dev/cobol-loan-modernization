# syntax=docker/dockerfile:1

# ---------------------------------------------------------------------------
# Stage 1: cobol-builder
#
# El paquete gnucobol4 de Debian/Ubuntu (snapshot "4.0-early" de 2020) NO
# tiene soporte JSON en absoluto: falla con "compiler is not configured to
# support JSON". SIMLOAN.cbl usa JSON GENERATE, así que hace falta compilar
# GnuCOBOL 3.2 desde fuente con libcjson-dev disponible en el sistema
# (./configure detecta cjson automático via --with-json=check por default).
# Verificado empíricamente antes de escribir este Dockerfile — no cambiar de
# versión sin volver a confirmar que JSON GENERATE compila.
# ---------------------------------------------------------------------------
FROM debian:bookworm-slim AS cobol-builder

RUN apt-get update && apt-get install -y --no-install-recommends \
        build-essential libtool autoconf automake pkg-config \
        flex bison gettext texinfo \
        libgmp-dev libdb-dev libncurses-dev libcjson-dev ca-certificates curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /src

# La descarga desde SourceForge es lenta (~50KB/s medido, tarball ~5.5MB,
# ~100-120s). --max-time 280 da margen; si falla por timeout de red, subir
# el timeout, no cambiar de versión sin re-verificar JSON GENERATE.
RUN curl -sSL --max-time 280 -o gnucobol-3.2.tar.gz \
        "https://sourceforge.net/projects/gnucobol/files/gnucobol/3.2/gnucobol-3.2.tar.gz/download" \
    && tar xzf gnucobol-3.2.tar.gz

WORKDIR /src/gnucobol-3.2
RUN ./configure --prefix=/usr/local --with-json=cjson \
    && make -j"$(nproc)" \
    && make install \
    && ldconfig

WORKDIR /build
COPY cobol/ .
RUN cobc -x -free -o simloan SIMLOAN.cbl

# Deja constancia de las shared libs reales que necesita el binario en
# runtime — usado como referencia al armar el stage final (ver más abajo).
RUN ldd simloan > /build/simloan.ldd || true

# ---------------------------------------------------------------------------
# Stage 2: java-builder
#
# Compila la API Spring Boot con el wrapper Maven del proyecto. Los tests
# de la API corren en CI (ver .github/workflows/ci.yml), no en el build de
# imagen, de ahí -DskipTests.
#
# Flyway (convención estándar de Spring Boot) busca migraciones en
# classpath:db/migration. Las migraciones reales viven en db/migrations/
# (con S) en la raíz del repo, así que se copian al classpath ANTES de
# empaquetar, para que la rama de api/ no tenga que duplicarlas ni haga
# falta una config custom de spring.flyway.locations.
# ---------------------------------------------------------------------------
FROM eclipse-temurin:23-jdk AS java-builder

WORKDIR /workspace

COPY api/ ./api/
COPY db/migrations/ ./db/migrations/

RUN mkdir -p api/src/main/resources/db/migration \
    && cp db/migrations/*.sql api/src/main/resources/db/migration/

WORKDIR /workspace/api
RUN chmod +x ./mvnw \
    && ./mvnw -q -B clean package -DskipTests

# ---------------------------------------------------------------------------
# Stage 3: runtime final
#
# Liviano: sin compiladores ni build tools. Copia el binario COBOL nativo,
# las shared libs de /usr/local/lib que produjo `make install` en el
# builder (libcob.so y afines — el paquete apt libcob4 es de la versión
# vieja sin JSON, sería inconsistente usarlo), las libs de sistema chicas
# que faltan en la imagen slim, y el jar de Spring Boot.
#
# Paquetes de sistema confirmados corriendo `ldd` sobre el binario real
# dentro del builder (no asumido de memoria): libcjson.so.1 (libcjson1),
# libncursesw.so.6 + libtinfo.so.6 (libncursesw6, que trae libtinfo6 como
# dependencia — el binario linkea la variante wide-char, no libncurses6
# a secas), libdb-5.3.so (libdb5.3t64 — Ubuntu 24.04/noble renombró el
# paquete por la transición a 64-bit time_t, confirmado con apt-cache
# search, no es el mismo nombre que en Debian bookworm), libgmp.so.10
# (libgmp10).
# ---------------------------------------------------------------------------
FROM eclipse-temurin:23-jre-noble AS runtime

RUN apt-get update && apt-get install -y --no-install-recommends \
        libcjson1 libncursesw6 libtinfo6 libdb5.3t64 libgmp10 \
    && rm -rf /var/lib/apt/lists/*

# Libs propias de GnuCOBOL compilado desde fuente (libcob.so, etc.)
COPY --from=cobol-builder /usr/local/lib /usr/local/lib
RUN ldconfig

RUN mkdir -p /app/bin
COPY --from=cobol-builder /build/simloan /app/bin/simloan
COPY --from=java-builder /workspace/api/target/*.jar /app/app.jar

# Coincide con simulador.cobol.executable-path en application.properties
# y con spring.flyway.locations: el jar ya tiene las migraciones en el
# classpath (ver stage java-builder), el default filesystem:../db/migrations
# de application.properties no existe en esta imagen.
ENV SIMULADOR_COBOL_EXECUTABLE_PATH=/app/bin/simloan
ENV FLYWAY_LOCATIONS=classpath:db/migration

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# cobol-loan-modernization

Simulador de préstamos con sistema de amortización francés. Demuestra un patrón real de modernización COBOL: el cálculo transaccional queda en un motor COBOL puro, y todo lo demás (API REST, persistencia, lookup de datos) vive en una capa Java moderna alrededor.

## Arquitectura

![Diagrama de secuencia — flujo real de POST /api/v1/prestamos/simular](docs/prestamo-simulacion-sequence.svg)

Versión interactiva (pan/zoom, tema claro/oscuro, trace): [`docs/prestamo-simulacion-sequence.html`](docs/prestamo-simulacion-sequence.html). Fuente editable: [`docs/prestamo-simulacion.sequence.json`](docs/prestamo-simulacion.sequence.json).

- **`cobol/`** — `SIMLOAN.cbl` + copybooks. Recibe `monto,plazo,tasa` por stdin (línea de texto, no JSON — ver más abajo), calcula la cuota por sistema francés, devuelve JSON plano por stdout. Sin acceso a datos: la tasa siempre llega resuelta desde afuera.
- **`api/`** — Spring Boot (Maven). Único endpoint `POST /api/v1/prestamos/simular`. Resuelve la tasa vigente en Postgres, invoca el proceso COBOL vía `ProcessBuilder`, mapea la respuesta. `BigDecimal` en todo el borde monetario.
- **`db/migrations/`** — Flyway. Tabla `tasas_bancarias` seedeada con tasas reales de bancos argentinos (fuente citada por fila, snapshot 2026-09-02).
- **`docker/Dockerfile`** — build multi-stage: compila GnuCOBOL desde fuente, compila la API, arma un runtime liviano sin compiladores.

## Por qué las decisiones técnicas quedaron así

**Java + BigDecimal, no Python/Node.** Java es el estándar de modernización COBOL en banca, y `BigDecimal` mapea exacto contra los campos `COMP-3` de COBOL — nunca hay redondeo de punto flotante en una cifra monetaria.

**Puente COBOL↔Java por texto plano + JSON, no JSON bidireccional.** La arquitectura original preveía `JSON GENERATE`/`JSON PARSE` en ambas direcciones. Verificado en la práctica (compilando GnuCOBOL 3.2 desde fuente y leyendo el propio código del compilador) que **`JSON PARSE` no está implementado en ningún release real de GnuCOBOL** — es un `CB_PENDING` hardcodeado en el parser. Como el contrato de entrada son solo 3 campos numéricos, la solución fue una línea delimitada por comas parseada con `UNSTRING` + `FUNCTION NUMVAL` (funciones COBOL estándar, sin dependencias). La salida sí es JSON real vía `JSON GENERATE`, que funciona sin problemas.

**GnuCOBOL compilado desde fuente, no el paquete `gnucobol4` de Debian/Ubuntu.** Ese paquete es un snapshot "4.0-early" de 2020 sin soporte JSON en absoluto. El Dockerfile compila GnuCOBOL 3.2 con `libcjson-dev` presente, que sí tiene `JSON GENERATE` funcional.

**No FFM/JNI/JNA para el puente.** Se evaluó y se descartó por riesgo de desalineación de memoria entre la JVM y el proceso nativo (alineación de `COMP-3`, `cob_init()`, `LD_LIBRARY_PATH`). El costo de un proceso por request es aceptable para este volumen; el riesgo de un bug de memoria no lo es para un proyecto que tiene que quedar sin bugs.

**Lookup de tasa en Java, no en COBOL.** COBOL queda como motor de cálculo puro, sin acceso a datos — es el patrón real de modernización: sacarle a COBOL toda responsabilidad que no sea el cálculo transaccional.

Historial completo de decisiones (incluyendo las que llevaron a estas correcciones) en `CLAUDE.md`, sección 6. Investigación original con fuentes en `docs/investigacion-cobol-modernizacion-rest.md`.

## Correr el proyecto

```bash
cp .env.example .env   # completar con credenciales locales
docker compose up --build
```

```bash
curl -X POST http://localhost:8080/api/v1/prestamos/simular \
  -H "Content-Type: application/json" \
  -d '{"banco":"Banco Macro","producto":"Plan Sueldo","montoSolicitado":100000.00,"plazoMeses":12}'
```

## Tests

```bash
cd api && ./mvnw test
```

Incluye un test de integración con Testcontainers (`PrestamoSimulacionEndToEndTest`) que compila la imagen Docker real (COBOL + Spring Boot) y la levanta contra un Postgres real — es el criterio de "listo" del proyecto, no los unit tests sueltos.

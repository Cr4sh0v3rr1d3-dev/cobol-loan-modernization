# Modernización COBOL → API REST — Investigación técnica completa

**Proyecto de portfolio:** simulador de préstamos COBOL expuesto como servicio REST.
**Objetivo:** dejar esto listo para continuar la implementación en Claude Code Desktop y subirlo a un repo privado de GitHub sin exponer secretos.

---

## 1. Decisión de arquitectura: por qué Java y no Python/Node

Tenías razón en cuestionar Python acá. La aclaración técnica es esta: el módulo `decimal.Decimal` de Python en sí *no* tiene errores de redondeo (es aritmética decimal exacta, igual que COMP-3), el riesgo real es que es fácil mezclar `float` por descuido en algún punto del pipeline (JSON, ORM, alguna librería) y perder precisión silenciosamente. Pero más allá de ese tecnicismo, tu instinto de buscar "el lenguaje que realmente se usa en bancos para esto" es el correcto para un proyecto de portfolio que quiere transmitir seriedad.

**Java es el estándar de facto de la industria para modernización COBOL en banca**, y por una razón muy concreta y verificable:

- `BigDecimal` en Java representa exactamente el mismo modelo numérico que `COMP-3` (packed decimal) de COBOL: valores decimales exactos en base 10, sin la aproximación binaria de `float`/`double`. Un blog especializado en migraciones COBOL→Java para banca lo resume así: *"COBOL `PIC 9` clauses and `COMP-3` packed-decimal fields represent exact base-10 values"* y advierte que convertir a `double`/`float` "constitutes a production defect rather than an acceptable shortcut" ([mecanik.dev](https://mecanik.dev/en/posts/cobol-to-java-migration-a-uk-enterprise-guide/)).
- El ecosistema Java (Spring/Jakarta EE) es el que concentra las herramientas de migración bancaria — hay incluso soluciones comerciales de migración COBOL→Java asistida por IA apuntadas específicamente a bancos, como la que lanzó CLPS Incorporation en 2026 ([Yahoo Finance](https://finance.yahoo.com/news/clps-incorporation-announces-ai-driven-123400302.html), [StockTitan](https://www.stocktitan.net/news/CLPS/clps-incorporation-announces-ai-driven-cobol-to-java-migration-dgl6cxoqkies.html)).
- Disponibilidad de talento y continuidad a largo plazo: los bancos priorizan Java precisamente porque hay pool de desarrolladores para mantenerlo por décadas, algo que también es un buen punto para justificar la decisión en una entrevista.

**Conclusión para el portfolio:** stack en **Java 22+ con Spring Boot**, usando `BigDecimal` en el borde de la API para toda cifra monetaria, y mapeo explícito y testeado contra los valores COMP-3 que devuelve el programa COBOL. Esto es defendible frente a cualquier entrevistador técnico de banca.

---

## 2. Cómo invocar GnuCOBOL desde Java: dos enfoques, uno recomendado

Investigué dos caminos posibles. Para un proyecto que "tiene que estar perfecto, sin errores", recomiendo el segundo por sobre el primero, aunque documento ambos porque el primero es más impresionante técnicamente si querés mostrar profundidad.

### Opción A (avanzada, más riesgo): Java FFM API + jextract

Java 22+ estabilizó el **Foreign Function & Memory API (FFM)**, el reemplazo moderno de JNI/JNA para llamar código nativo sin escribir C intermedio. Detalles concretos de un caso real documentado ([the-main-thread.com](https://www.the-main-thread.com/p/cobol-java-ffm-modernization-macbook)):

- Hay que llamar `cob_init()` antes de invocar cualquier módulo COBOL compilado.
- Los buffers de memoria se crean con `Arena.ofConfined()` / `Arena.allocate()`, replicando el layout exacto de cada `PIC` clause (ej. `PIC 9(9)V99` necesita 12 bytes: 11 dígitos + terminador).
- El punto decimal es **implícito**: hay que insertarlo manualmente al leer el resultado, porque COBOL no lo guarda como carácter.
- `jextract` puede generar los bindings Java automáticamente a partir de un header C, evitando el boilerplate típico de JNI.
- **Pitfall documentado:** `SymbolLookup` de FFM resuelve símbolos a través del linker del OS directamente, *no* de la JVM — `-Djava.library.path` no alcanza, hace falta `LD_LIBRARY_PATH` (Linux) o el equivalente del sistema para que encuentre tanto tu `.so` compilado como el runtime de GnuCOBOL.

Este enfoque es real y usado, pero el riesgo de bugs sutiles de layout de memoria (padding, alineación, signos en COMP-3) es alto — exactamente el tipo de "error silencioso" que no querés en un proyecto que vas a mostrar como pulido.

### Opción B (recomendada para este portfolio): frontera JSON nativa de COBOL + proceso/socket

GnuCOBOL soporta `JSON GENERATE` y `JSON PARSE` de forma nativa desde el lenguaje (funcionalidad incorporada en el desarrollo hacia GnuCOBOL 3.1, usando la librería `cJSON` internamente — confirmado en el propio tracker de parches del proyecto, [SourceForge #45](https://sourceforge.net/p/gnucobol/patches/45/)). Esto significa que **el propio programa COBOL puede recibir y devolver JSON en texto plano**, sin que Java tenga que entender el layout binario de memoria de la COMMAREA.

Arquitectura resultante:

```
Cliente HTTP
     │  POST /api/v1/prestamos/simular  {json}
     ▼
Spring Boot (Java 22+)
     │  valida con Bean Validation, convierte a BigDecimal
     │  ProcessBuilder → invoca el ejecutable COBOL compilado
     │  (o: socket persistente si se quiere evitar el costo de spawnear proceso)
     ▼
Ejecutable COBOL (compilado con GnuCOBOL, cobc -x)
     │  JSON PARSE del stdin/request → WS-LOAN-REQUEST
     │  lógica de negocio (igual que tenías planteado)
     │  JSON GENERATE de WS-LOAN-RESPONSE → stdout/response
     ▼
Spring Boot parsea el JSON de vuelta, remapea a BigDecimal, aplica ROUND igual
que hace COBOL con COMP-3, devuelve 200/4xx al cliente
```

Ventajas concretas para este proyecto:
- Cero riesgo de desalineación de memoria entre JVM y proceso nativo — el contrato es texto (JSON), no bytes.
- El copybook (`LOANREQ.cpy`/`LOANRES.cpy`) sigue siendo el "contrato de datos" documentado, tal como ya lo tenías diseñado; simplemente el transporte hacia Java es JSON en vez de COMMAREA binaria.
- Mucho más fácil de testear de punta a punta y de explicar en una entrevista: "expuse el copybook como contrato JSON usando `JSON GENERATE`/`JSON PARSE` nativos de GnuCOBOL, y Java orquesta el proceso" es una frase perfectamente defendible.
- Si más adelante querés mostrar la Opción A como "modo avanzado" en el mismo repo (una rama o un módulo alternativo), queda como plus, no como base frágil.

**Nota de precisión a verificar en Claude Code Desktop:** confirmá con `cobc -v` la versión exacta de GnuCOBOL que se instala en tu imagen Docker y corré un test que verifique `JSON GENERATE`/`JSON PARSE` end-to-end antes de construir el resto — el soporte JSON nativo depende de que la build de GnuCOBOL se haya compilado con la librería `cJSON` disponible.

---

## 3. GnuCOBOL en Docker desde el día uno

Confirmaste que querés Docker desde el arranque, que es la decisión correcta para reproducibilidad (evita el clásico "funciona en mi máquina" al clonar el repo).

Patrón recomendado — **multi-stage build**:

```dockerfile
# ---- Stage 1: compilar el programa COBOL ----
FROM debian:bookworm-slim AS cobol-builder
RUN apt-get update && apt-get install -y gnucobol4 build-essential libcjson-dev \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /build
COPY cobol/ .
RUN cobc -x -free -o simloan SIMLOAN.cbl   # ejecutable standalone, no .so

# ---- Stage 2: build de la app Java ----
FROM eclipse-temurin:22-jdk AS java-builder
WORKDIR /app
COPY . .
RUN ./mvnw -q clean package -DskipTests=false

# ---- Stage 3: runtime final, liviano ----
FROM eclipse-temurin:22-jre-jammy
RUN apt-get update && apt-get install -y libcob4 libcjson1 \
    && rm -rf /var/lib/apt/lists/*
COPY --from=cobol-builder /build/simloan /app/bin/simloan
COPY --from=java-builder /app/target/*.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

La razón de usar multi-stage no es solo tamaño de imagen: es que **el runtime final no tiene ni el compilador COBOL ni las herramientas de build de Java**, así que la superficie de ataque y el tamaño quedan mínimos — algo que también es un buen punto a mencionar si documentás decisiones de arquitectura en el README.

Cosas a verificar en Claude Code Desktop (el ecosistema de imágenes GnuCOBOL cambia seguido y no hay una imagen oficial única mantenida por el proyecto):
- Qué paquete Debian/Alpine trae la versión de GnuCOBOL que soporta `JSON GENERATE` (en Debian bookworm es `gnucobol4`; confirmar con `apt-cache policy gnucobol4` al momento de construir).
- Si usás Alpine en vez de Debian, GnuCOBOL depende de glibc en algunas builds — vale la pena probar primero con Debian slim para evitar dolores de cabeza con musl.

---

## 4. Manejo de secretos y `.env` — cómo blindar el repo privado

Esto es probablemente lo más importante para que "no explote" cuando lo subas, incluso siendo privado (un repo privado igual puede tener colaboradores, forks accidentales, o pasar a público más adelante).

**Dato clave que hay que tener claro:** el **push protection nativo de GitHub que bloquea secretos automáticamente es gratis solo para repos públicos**. Para repos **privados**, bloquear secretos al momento del push requiere **GitHub Advanced Security (GHAS)**, que es un producto pago a nivel organización/enterprise ([docs.github.com](https://docs.github.com/en/code-security/concepts/secret-security/push-protection)). Como plan individual con un repo privado, **no vas a tener ese cinturón de seguridad automático de GitHub** — así que la defensa tiene que estar en tu propio flujo, no en la plataforma.

Capas de defensa recomendadas (en orden, todas gratis):

**Capa 1 — Nunca lo escribas en el repo:**
- `.env` real solo en tu máquina/Claude Code Desktop, nunca en git.
- `.env.example` (o `.env.sample`) commiteado, con placeholders del tipo `DB_PASSWORD=changeme`, documentando qué variables existen sin sus valores reales.
- `.gitignore` con `.env`, `.env.*`, `*.pem`, `*.key` desde el primer commit del repo — antes de escribir una sola línea de código.

**Capa 2 — Hook local con Gitleaks (bloquea antes de que llegue a GitHub):**
- Instalar [gitleaks](https://github.com/gitleaks/gitleaks) y configurarlo vía `pre-commit`:
  - `.pre-commit-config.yaml` en la raíz con el hook de gitleaks.
  - Correr `pre-commit install` una vez por clon (documentarlo en el README como parte del setup).
  - Un `.gitleaks.toml` con `[allowlist]` para no marcar falsos positivos en fixtures de test (ej. API keys de ejemplo claramente marcadas como `EXAMPLE_KEY_DO_NOT_USE`).
- Nota realista: `git commit --no-verify` saltea el hook local, así que esta capa sola no es suficiente ([decryptiondigest.com](https://www.decryptiondigest.com/blog/prevent-developers-pushing-secrets-git-pre-commit-gitleaks-guide)).

**Capa 3 — CI como red de contención (server-side, no se puede bypassear con `--no-verify`):**
- GitHub Action con `gitleaks/gitleaks-action@v2` corriendo en cada PR, con `fetch-depth: 0` para escanear el historial completo, no solo el diff.
- Marcarlo como *required status check* en la protección de rama de `main` — así ni vos mismo podés mergear sin que pase.

**Capa 4 — Si alguna vez se filtra algo (aunque sea en privado):**
- Rotar/revocar la credencial inmediatamente, no esperar a "limpiar" el historial primero — la rotación es lo que realmente neutraliza el riesgo.
- Recién después, reescribir historia (`git filter-repo` o BFG Repo-Cleaner) si hace falta sacarlo del historial.

Para este proyecto en particular, las variables sensibles reales que vas a necesitar (aunque no haya DB2 real): credenciales de una base de datos local para las tasas (si la sumás), y cualquier token si conectás algo a GitHub Actions. Documentar esto en el `.env.example` desde el día uno evita el error clásico de "lo pongo hardcodeado ahora y lo saco después".

---

## 5. Testing — qué probar y con qué

Para que quede "sin errores" de verdad, no solo libre de secretos:

- **Nivel COBOL:** casos de borde del propio cálculo — monto/plazo cero o negativo, tasa cero, overflow en `COMPUTE` (ya lo tenías contemplado con `ON SIZE ERROR`), y comparar manualmente contra una planilla de amortización francesa calculada aparte para al menos 3-4 escenarios conocidos.
- **Nivel contrato JSON:** un test que verifique que `JSON GENERATE`/`JSON PARSE` de GnuCOBOL efectivamente producen/consumen el shape exacto que espera Java — este es el punto de falla más probable de toda la arquitectura, ponerle foco.
- **Nivel Java/API:** JUnit 5 + `BigDecimal` con comparaciones exactas (`compareTo`, nunca `equals` a secas por el tema de `scale`), y **Testcontainers** para levantar el contenedor completo (COBOL + Java) en el pipeline de CI y pegarle con requests HTTP reales de punta a punta — esto es lo que un evaluador técnico va a valorar más que tests unitarios sueltos, porque prueba la integración real.
- **Nivel seguridad:** el propio gitleaks scan en CI cuenta como test de seguridad automatizado, vale la pena nombrarlo así en el README.

---

## 6. Estructura de repo sugerida

```
cobol-loan-modernization/
├── .github/workflows/ci.yml        # build + tests + gitleaks
├── .gitleaks.toml
├── .pre-commit-config.yaml
├── .gitignore
├── .env.example
├── cobol/
│   ├── SIMLOAN.cbl
│   ├── LOANREQ.cpy
│   └── LOANRES.cpy
├── api/                             # proyecto Spring Boot
│   ├── src/main/java/...
│   ├── src/test/java/...
│   └── pom.xml (o build.gradle)
├── docker/
│   └── Dockerfile                   # multi-stage descripto arriba
├── docker-compose.yml
└── README.md                        # arquitectura, decisiones, cómo correr
```

---

## 7. Checklist para retomar en Claude Code Desktop

1. Confirmar versión de GnuCOBOL disponible (imagen Debian bookworm + `gnucobol4`) y correr un `JSON GENERATE`/`JSON PARSE` mínimo de prueba antes de escribir el resto de la lógica.
2. Escribir `SIMLOAN.cbl` con el parseo JSON nativo en vez de `LINKAGE SECTION` pura (mantener los `.cpy` como documentación del contrato de datos igual).
3. Armar el Dockerfile multi-stage y confirmar que el ejecutable COBOL corre standalone dentro del contenedor final.
4. Proyecto Spring Boot: endpoint `POST /api/v1/prestamos/simular`, DTO con `BigDecimal`, invocación al proceso COBOL vía `ProcessBuilder`, mapeo de respuesta.
5. Tests: unitarios Java, contrato JSON, y de integración con Testcontainers.
6. Antes del primer `git init`: `.gitignore`, `.env.example`, `.pre-commit-config.yaml` con gitleaks.
7. GitHub Actions: build + test + gitleaks scan como *required check*.
8. Crear el repo como **privado** en GitHub, activar branch protection en `main` con el CI como required check (push protection automático no está disponible en privado sin GHAS, así que esta capa CI es tu red real).
9. README con el diagrama de arquitectura y la justificación de por qué Java/BigDecimal en vez de un lenguaje con floats — es tu mejor argumento de portfolio.

---

## 8. Persistencia real: tasas bancarias en vez de valor hardcodeado

El diseño original tenía `RES-TASA-ANUAL` hardcodeado dentro del `.cbl` (comentario `* Consulta a tabla de scoring/tasas en DB2`). Se reemplaza por persistencia real:

**Dónde vive:** Postgres, agregado como servicio en `docker-compose.yml`, con migraciones versionadas por Flyway (`db/migrations/`). Tabla `tasas_bancarias(banco, producto, tna, cft, fecha_vigencia, fuente)`.

**Quién la consulta:** Java, no COBOL. Spring Boot resuelve la tasa por banco/producto contra Postgres *antes* de invocar el proceso COBOL, y la manda como parte del JSON de entrada (`REQ-TASA-ANUAL` pasa a ser un campo de entrada del copybook, no algo que COBOL calcule o tenga hardcodeado). Esto mantiene a COBOL como motor de cálculo puro — sin acceso a datos — que es el patrón real que siguen los proyectos de modernización bancaria: sacarle a COBOL toda responsabilidad que no sea el cálculo transaccional en sí.

**Datos semilla — tasas reales de bancos argentinos, snapshot 2026-09-02** (fuente: [iproup](https://www.iproup.com/finanzas/71138-prestamos-personales-cuanto-cuesta-pedir-banco-cobra-menos-tasa-total-septiembre-2026)):

| Banco | Producto | TNA | CFT (TEA) |
|---|---|---|---|
| Banco Galicia | Éminent | 39% | 58.9% |
| Banco Macro | Jubilados y Selecta | 55% | 91.1% |
| Banco Nación | Jubilados con descuento | 56% | 93.3% |
| Banco Galicia | Personal Plus | 57% | 95.8% |
| Banco Macro | Plan Sueldo | 61% | 104.7% |
| Banco Nación | Empleados públicos c/haberes | 64% | 111.8% |
| Banco Ciudad | Plan Sueldo | 70% | 126.7% |
| Banco Santander | Personal | 79% | 150.9% |
| Banco Patagonia | Personal Online | 98% | 209.8% |
| Banco Hipotecario | Destino Libre | 110.4% | 255.2% |
| BBVA | Oferta exclusiva clientes | 129% | 320.1% |

**Por qué esto y no un feed en vivo:** las tasas de préstamos personales en Argentina cambian con frecuencia y no hay una API pública gratuita y estable para consumirlas en tiempo real sin scraping. Documentar el snapshot con `fecha_vigencia` y `fuente` explícitos en la propia tabla es la opción honesta — cualquiera que audite el repo ve exactamente de dónde salió cada número y cuándo. Si más adelante se quiere un job de refresco automático (scraping periódico del comparador BCRA), queda como mejora futura documentada en el Decision Log del `CLAUDE.md`, no como algo que haya que fingir que ya existe.

---

## Fuentes

- [COBOL to Java Migration - A UK Enterprise Guide 2026 (mecanik.dev)](https://mecanik.dev/en/posts/cobol-to-java-migration-a-uk-enterprise-guide/)
- [CLPS Incorporation Announces AI-Driven COBOL-to-Java Migration Solution (Yahoo Finance)](https://finance.yahoo.com/news/clps-incorporation-announces-ai-driven-123400302.html)
- [CLPS launches AI COBOL-to-Java bank migration tool (StockTitan)](https://www.stocktitan.net/news/CLPS/clps-incorporation-announces-ai-driven-cobol-to-java-migration-dgl6cxoqkies.html)
- [COBOL on Mac + Java FFM: What Modernization Misses (the-main-thread.com)](https://www.the-main-thread.com/p/cobol-java-ffm-modernization-macbook)
- [GnuCOBOL Patch #45 — JSON GENERATE (SourceForge)](https://sourceforge.net/p/gnucobol/patches/45/)
- [GnuCOBOL — GNU Project (sourceforge.io)](https://gnucobol.sourceforge.io/)
- [Prevent Secrets in Git 2026: Pre-Commit, Gitleaks, Push Protection (decryptiondigest.com)](https://www.decryptiondigest.com/blog/prevent-developers-pushing-secrets-git-pre-commit-gitleaks-guide)
- [Push protection - GitHub Docs](https://docs.github.com/en/code-security/concepts/secret-security/push-protection)
- [Improvements to secret scanning and public monitoring — GitHub Changelog, julio 2026](https://github.blog/changelog/2026-07-15-improvements-to-secret-scanning-and-public-monitoring/)
- [Préstamos con mora récord: qué tasa cobra cada banco en septiembre 2026 (iproup)](https://www.iproup.com/finanzas/71138-prestamos-personales-cuanto-cuesta-pedir-banco-cobra-menos-tasa-total-septiembre-2026)

# desktop-client

Cliente de escritorio JavaFX para `POST /api/v1/prestamos/simular`. Un solo form: banco/producto, monto, plazo, botón "Simular". Hace un HTTP request real contra el backend (`http://localhost:8080`) — no replica la fórmula del préstamo, no tiene lógica de negocio propia. Muestra la cuota, la tasa aplicada, el total de intereses (formateados en es-AR: `100.000,00`) y un gráfico de barras apiladas con la composición interés/amortización de cada cuota.

Módulo Maven independiente, Java 17 (ver "Por qué Java 17" más abajo). No depende de `api/` ni de ninguna otra parte del repo — solo habla HTTP.

## Requisitos

- JDK 17 (jlink/jpackage necesitan un JDK real de la misma línea que targetea el módulo; este repo no trae uno, hay que tener uno instalado).
- [JavaFX jmods](https://gluonhq.com/products/javafx/) para la misma versión que `pom.xml` (`javafx.version`, hoy 21.0.4) — **no son los mismos jars que trae Maven**, es un zip aparte (`openjfx-<version>_windows-x64_bin-jmods.zip`).
- Windows + [WiX Toolset v3](https://wixtoolset.org/) instalado (`winget install WiXToolset.WiXToolset`) — lo pide `jpackage --type msi` para generar el `.msi`. Sin WiX, `jpackage` falla directo con un error claro.

## Build y run en desarrollo

```bash
JAVA_HOME=/ruta/al/jdk-17 ./mvnw -q clean package
```

Genera `target/prestamos-simulador-desktop-1.0.0.jar` (jar modular, con `module-info.class`).

Para correr sin empaquetar (necesita los jars de JavaFX con clasificador de plataforma, que Maven ya bajó a `~/.m2`):

```bash
java --module-path "target/classes;<ruta-a-javafx-base-win.jar>;<ruta-a-javafx-controls-win.jar>;<ruta-a-javafx-graphics-win.jar>" \
     -m com.portfolio.prestamos.desktop/com.portfolio.prestamos.desktop.SimuladorDesktopApp
```

(`build.sh` en esta carpeta ya wrappea el `JAVA_HOME` correcto para este repo.)

## Generar el instalador `.msi`

Con el backend corriendo o no (el instalador no lo necesita, solo la app en runtime).

**1. Empaquetar el jar** (arriba) y copiarlo a un directorio de input propio (`jpackage` requiere que el jar esté en un directorio dedicado, no importa el path original):

```bash
mkdir -p target/input
cp target/prestamos-simulador-desktop-1.0.0.jar target/input/
```

**2. `jlink`** — runtime recortado con solo los módulos que hacen falta. **Importante: incluir `jdk.localedata`** — sin este módulo, `jlink` genera un runtime que ignora silenciosamente cualquier `Locale` que no sea la default del JDK (el formato `es-AR` de la app se degrada a formato US sin ningún error ni warning). Se puede acotar con `--include-locales` para no traer las ~700 locales completas:

```bash
jlink --module-path "target/prestamos-simulador-desktop-1.0.0.jar;<ruta-a-javafx-jmods>" \
      --add-modules com.portfolio.prestamos.desktop,jdk.localedata \
      --include-locales=es-AR,en \
      --output target/runtime \
      --strip-debug --no-header-files --no-man-pages \
      --launcher simulador=com.portfolio.prestamos.desktop/com.portfolio.prestamos.desktop.SimuladorDesktopApp
```

Runtime resultante: ~85 MB (vs. un JDK completo). `target/runtime/bin/simulador.bat` ya es un launcher ejecutable para probarlo antes de empaquetar el instalador.

**3. `jpackage`** — el `.msi` final:

```bash
jpackage --type msi \
  --name "Simulador de Prestamos" \
  --app-version 1.0.0 \
  --vendor cobol-loan-modernization \
  --input target/input \
  --main-jar prestamos-simulador-desktop-1.0.0.jar \
  --main-class com.portfolio.prestamos.desktop.SimuladorDesktopApp \
  --runtime-image target/runtime \
  --dest target/installer \
  --win-dir-chooser --win-menu --win-per-user-install
```

Genera `target/installer/Simulador de Prestamos-1.0.0.msi`.

**`--win-per-user-install` es intencional, no un default de `jpackage`**: sin este flag, el MSI instala a nivel de máquina y **requiere privilegios de administrador** (falla con error 1925 si el usuario no eleva). Con el flag, instala en `%LOCALAPPDATA%` sin pedir elevación — mejor default para un instalador de demo/portfolio.

## Verificación

Instalar en modo silencioso y confirmar que corre de verdad (no solo que el `.msi` se generó):

```powershell
msiexec /i "target\installer\Simulador de Prestamos-1.0.0.msi" /quiet /norestart
& "$env:LOCALAPPDATA\Simulador de Prestamos\Simulador de Prestamos.exe"
```

Con el backend real levantado (`docker compose up -d` en la raíz del repo), simular un préstamo real y confirmar que la cuota coincide con el valor ya verificado en el resto del proyecto (ver CLAUDE.md sección 6).

## Por qué Java 17, no Java 23 (como `api/`)

`jlink`/`jpackage` para generar un `.msi` **tienen que correr nativos en Windows** — no se pueden correr desde un contenedor Linux como se hizo con el resto del build de este proyecto, porque un `.msi` es un artefacto específico de plataforma. El host de desarrollo de este proyecto no tiene un JDK 23 real instalado (mismo hallazgo que ya está documentado para `api/`), así que `desktop-client` targetea Java 17 LTS — el JDK real disponible — en vez de forzar consistencia de versión con `api/`. Esto no es un problema: son dos procesos completamente independientes que solo hablan HTTP, no comparten JVM ni classpath.

## Por qué JavaFX + jpackage, no Electron

Es JDK nativo: `jlink`/`jpackage` vienen incluidos en el JDK, sin runtime adicional que empaquetar por separado (Electron embebe Chromium + Node completos). Consistente con que el resto del proyecto ya es Java — un cliente Electron hubiera sido la única pieza no-Java del repo sin necesidad real.

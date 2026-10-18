# Verificación de la fase 6 — PM1EC0100

Fecha: 2026-10-17.

## Cambios de alcance autorizados

Se agregó InicioActivity como cuarta pantalla, con el logo UTH proporcionado y bienvenida.
Se conservan Registro, Apuntes y Dictado. La interfaz usa verde UTH, tarjetas, campos
Material y una selección local de Material Icons con estados dinámicos y descripciones
accesibles. Los botones de grabación y reproducción cambian entre su acción y detener.
Se mantienen etiquetas breves junto a controles con iconos para facilitar su comprensión.

El audio permanece privado; SQLite guarda rutas relativas. La investigación sobre BLOB
y transcripción está en AUDIO_Y_TRANSCRIPCION.md. No se implementó transcripción de
un archivo grabado ni almacenamiento BLOB.

## Pruebas y verificaciones ejecutadas

| Verificación | Resultado real |
|---|---|
| Sintaxis de todos los XML de recursos | Correcta |
| Compilación de recursos nuevos con AAPT2 | Correcta |
| Enlace de recursos con AAPT2 usando como referencia recursos del APK anterior | Correcto; comprobación parcial, no genera un APK instalable actualizado |
| Compilación de fuentes Java con `javac --release 11`, SDK y dependencias locales | Correcta; incluye fuentes de pruebas instrumentadas |
| JUnit: fechas reales, formato y años bisiestos | Ejecutada, correcta |
| JUnit: campos obligatorios, tema y archivo inexistente/vacío | Ejecutada, correcta |
| JUnit: descripción vacía o válida | Ejecutada, correcta |
| JUnit: rutas relativas, rutas antiguas y rechazo de rutas fuera de audios | Ejecutada, correcta |
| JUnit: eliminación limitada al archivo privado solicitado | Ejecutada, correcta |
| Migración SQL extraída de DatabaseHelper y ejecutada con SQLite de Python | Correcta: preserva registros y rutas relativas existentes |

Resultado JUnit: **OK (5 tests)**. Las muestras de archivo usadas en estas pruebas
verifican existencia y tamaño, no validez acústica ni reproducción de AAC.

La comprobación de Java usa dependencias ya presentes en caché. No sustituye la
resolución exacta de dependencias, el empaquetado ni la ejecución de Gradle.

## Bloqueos del entorno

El wrapper no puede escribir su bloqueo en `~/.gradle`. Se intentó usar la distribución
local de Gradle con una carpeta temporal y modo offline; tampoco pudo iniciar porque
el entorno impide crear el socket del servicio FileLockContentionHandler
(`java.net.SocketException: Operation not permitted`).

ADB tampoco puede iniciar su servidor (`could not install smartsocket listener`).
Por lo tanto no se instaló ni ejecutó esta versión en un teléfono o emulador, no se
verificó visualmente en Android y no se generó un APK completo actualizado. El APK
que ya existía en `app/build/outputs/` pertenece a una compilación anterior.

## Pruebas preparadas para Android, todavía no ejecutadas

`ApunteDAOTest` utiliza una base separada en la caché, con nombre `pruebas_bitacora_clases.db`.
Sus dos pruebas cubren:

- CRUD, filtro parametrizado con una entrada que contiene SQL, actualización exclusiva
  de descripción, cierre/reapertura y persistencia, consulta por ID y uso de audio.
- Migración de la versión 1 a la 2 conservando tema y ruta relativa.

Las fuentes compilaron; los resultados de estas dos pruebas siguen pendientes.

## Lista obligatoria para probar en un teléfono

Todas las filas siguientes están **PENDIENTES DE EJECUCIÓN EN ANDROID**. Que una
validación tenga una prueba JUnit no significa que se haya probado su diálogo en pantalla.

| Escenario | Resultado esperado |
|---|---|
| Registrar apunte válido | Confirmación y formulario limpio |
| Guardar sin asignatura | Alerta y ningún registro nuevo |
| Guardar sin tema | Alerta y ningún registro nuevo |
| Guardar sin fecha | Alerta y ningún registro nuevo |
| Guardar sin descripción | Alerta y ningún registro nuevo |
| Guardar sin audio | Alerta y ningún registro nuevo |
| Introducir fecha inválida, como 2026-02-31 | Alerta de fecha real con AAAA-MM-DD |
| Introducir símbolos en tema | Alerta; aceptar acentos y ñ |
| Grabar y detener audio | Temporizador MM:SS e icono de detener; archivo disponible |
| Reproducir grabación antes de guardar | Sonido y cambio de icono; libera recursos al finalizar |
| Guardar en SQLite | El apunte aparece en el listado |
| Cerrar y volver a abrir | Los registros continúan disponibles |
| Comprobar persistencia | Textos y audio coinciden con los guardados |
| Listar registros | Fecha y tema visibles; estado vacío si no hay registros |
| Filtrar por asignatura y Todas | Solo registros de la asignatura o listado completo |
| Seleccionar apunte | Fondo verde y selección única |
| Reproducir audio seleccionado | Reproduce el archivo correcto; alerta si falta |
| Actualizar texto sin regrabar | Guarda cambios y conserva el audio |
| Eliminar apunte confirmando Sí | Elimina registro y audio si nadie más lo usa |
| Cancelar eliminación con No | Registro y audio permanecen |
| Dictar descripción | Abre el reconocedor en español y recupera texto |
| Editar texto reconocido | Permite corrección manual |
| Guardar descripción | Cambia solo descripción y persiste al reabrir |
| Denegar permiso de micrófono | Explicación con AlertDialog, formulario utilizable |
| Volver a solicitar permiso cuando corresponda | Explica el uso y solicita sin forzar configuraciones |
| Sin reconocedor de voz compatible | Mensaje y edición manual disponible |
| Cancelar dictado | Conserva texto previo |

Pruebas adicionales de los cambios recientes, también pendientes:

- Inicio muestra el logo completo, bienvenida y navegación correcta.
- Agregar asignatura, rechazar duplicados y conservarla tras cerrar la aplicación.
- Rotar Registro, Listado y Dictado sin perder textos, filtro ni selección.
- Interrumpir grabación al enviar la app al fondo y comprobar liberación del micrófono.
- Grabación demasiado corta: alerta y conservación del audio anterior.
- Reemplazar audio en edición y comprobar que el anterior se elimina solo tras guardar.
- Cancelar edición y comprobar conservación del audio original.
- Actualizar desde una instalación de versión de base 1 y escuchar los audios existentes.
- Probar pantalla pequeña, modo horizontal, teclado, tamaño de fuente grande y TalkBack.
- Probar iconos de grabación/reproducción, etiquetas y estados deshabilitados.

## Cómo continuar la verificación fuera del entorno restringido

En la terminal de Android Studio, desde la raíz del proyecto:

```bash
./gradlew assembleDebug testDebugUnitTest lintDebug
```

Con un teléfono conectado, desbloqueado y con depuración USB autorizada:

```bash
./gradlew connectedDebugAndroidTest
```

Después ejecutar la configuración `app` de Android Studio. El launcher es InicioActivity.
Registrar dispositivo, versión Android, resultado real y evidencia de cada prueba manual.
No marcar las pruebas pendientes como exitosas sin ejecutarlas.

## Revisión de la rúbrica por código

| Punto | Implementación identificada | Verificación funcional |
|---|---|---|
| Grabación + SQLite (2) | MainActivity, controlador y DAO | Pendiente Android |
| Modelo (1) | Apunte con seis atributos | Fuente compilada |
| Obligatorios (1) | Validación central y AlertDialog | JUnit parcial; UI pendiente |
| Expresiones regulares (1) | Tema y fecha en controlador | JUnit correcto |
| Listado (1) | ApuntesActivity y DAO | Pendiente Android |
| Eliminar (0.5) | Confirmación, DAO y limpieza de audio | Pendiente Android |
| Actualizar (0.5) | Registro en edición y DAO | Pendiente Android |
| Filtro parametrizado (2) | `asignatura = ?` | Fuente revisada; prueba Android pendiente |
| MediaPlayer (2) | Registro y Listado | Pendiente teléfono |
| RecognizerIntent (2) | DictadoActivity, es-HN | Pendiente teléfono |
| Permiso y explicación (2) | MainActivity y Manifest | Pendiente teléfono |
| Agregar asignaturas (extra) | Diálogo y SharedPreferences | Pendiente Android |

## Actualización posterior: instalación y diagnóstico de audio

Después de este informe, `./gradlew installDebug` sí completó la compilación y la
instalación en Pixel_4_XL (AVD, Android 17). También se compiló e instaló la versión
que configura MediaPlayer con USAGE_MEDIA/CONTENT_TYPE_SPEECH, asocia los botones de
volumen al flujo multimedia y comprueba la amplitud durante la grabación.

El usuario reportó que no escucha sus grabaciones. La causa todavía no está confirmada.
La app ahora distingue entre recepción de señal y espera de señal; una grabación de al
menos dos segundos con amplitud máxima cero muestra una alerta. Esta comprobación no
identifica palabras ni demuestra que el archivo sea audible de extremo a extremo.

La configuración AVD declara `hw.audioInput=yes`; eso no demuestra que el acceso al
micrófono del host esté habilitado en los controles extendidos ni autorizado por macOS.
La consulta directa mediante ADB sigue bloqueada. La grabación audible, la reproducción
y las pruebas instrumentadas continúan pendientes de confirmación real.

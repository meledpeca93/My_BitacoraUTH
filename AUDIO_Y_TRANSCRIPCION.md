# Audio BLOB y transcripción local — 2026-10-18

## Funciones implementadas

- Grabación con MediaRecorder en AAC/M4A, mono a 16 kHz para nuevas notas.
- Ruta relativa en el atributo original `audio` y archivo en `getFilesDir()/audios/`.
- Copia binaria en SQLite mediante `audio_fragmentos`, en bloques de hasta 256 KiB.
- Recuperación desde BLOB cuando falta el archivo; respaldo de notas anteriores al utilizarlas.
- Transcripción del archivo guardado con Vosk en español, sin conexión ni clave API.
- Dictado en vivo con RecognizerIntent, conservado para cumplir el examen.

## Cómo usarlo

1. Grabar una nota audible y guardar el apunte.
2. Abrir Mis apuntes y seleccionar el registro.
3. Tocar Dictar y después **Transcribir audio guardado**.
4. Confirmar el reemplazo del texto del campo y esperar el resultado.
5. Revisar/corregir el texto y tocar **Guardar Descripción**.

La transcripción no guarda automáticamente ni modifica tema, fecha o audio. Si no hay
palabras reconocidas, falla el proceso o se cancela, el texto anterior se conserva.
La pantalla permite cancelar el trabajo. Al girar o cerrar la pantalla, se cancela el
trabajo anterior y se conserva el texto ya escrito mediante el estado de la Activity.
Se puede volver a iniciar la transcripción al regresar.

El botón del micrófono sigue utilizando el reconocedor del sistema para dictado en vivo.
La disponibilidad de esa Activity externa no condiciona la transcripción local con Vosk.

## Flujo del archivo y BLOB

BLOB almacena bytes: no reconoce palabras. Se conserva `Apunte.audio` como String y la
columna TEXT original. La base de datos versión 3 agrega:

```sql
CREATE TABLE audio_fragmentos (
    apunte_id INTEGER NOT NULL REFERENCES apuntes(id) ON DELETE CASCADE,
    parte INTEGER NOT NULL,
    datos BLOB NOT NULL,
    PRIMARY KEY (apunte_id, parte)
);
```

AudioBlobDAO guarda por bloques, evitando un audio completo en una fila de CursorWindow.
ApunteDAO usa una transacción para guardar apunte y audio juntos. Recuperar concatena los
bloques ordenados en un archivo privado. El listado consulta solo metadatos, sin BLOB.
Eliminar el apunte elimina sus fragmentos por clave foránea. La limpieza del archivo
se hace después del éxito de la operación y de comprobar que no tiene otros usuarios.

Los audios anteriores se respaldan la primera vez que se usan. Si el archivo anterior
se perdió antes de existir una copia BLOB, no es posible recuperar ese contenido.
Desinstalar la aplicación elimina sus datos privados, incluidas la base y las grabaciones.

## Conversión y reconocimiento

- `audio/AudioPcm.java`: mezcla canales PCM de 16 bits a mono.
- `audio/DecodificadorAudio.java`: MediaExtractor y MediaCodec convierten AAC/M4A a PCM
  mono; se conserva la frecuencia real para informarla a Vosk. No se interpretan los bytes
  comprimidos de un M4A como si fueran muestras PCM.
- `audio/ModeloVoz.java`: copia el modelo del APK al directorio privado y marca la copia
  como lista solamente al completarse. No solicita almacenamiento público.
- `audio/TranscriptorAudio.java`: alimenta Vosk por bloques, une sus resultados JSON y
  elimina el PCM temporal al terminar, incluso si falla o se cancela.
- `DictadoActivity`: ejecuta ese trabajo en un ExecutorService y actualiza la interfaz
  en el hilo principal. El trabajo utiliza su propio controlador y lo cierra al terminar.

Vosk devuelve texto que puede contener errores y normalmente carece de puntuación.
Debe revisarse antes de guardar. No puede recuperar palabras de una grabación silenciosa.
La prueba del micrófono del emulador sigue pendiente de confirmación real.

## Modelo, dependencias y compilación

Dependencias fijadas siguiendo el ejemplo Android oficial:

- `com.alphacephei:vosk-android:0.3.75@aar`
- `net.java.dev.jna:jna:5.18.1@aar`
- Modelo `vosk-model-small-es-0.42`, licencia Apache 2.0.

`preBuild` prepara el modelo automáticamente. La primera compilación necesita Internet
para descargar dependencias y el ZIP de unos 39 MB. El modelo descomprimido ocupa unos
58 MB y se incluye en el APK; en su primer uso se copia también al almacenamiento privado.
El APK universal debug generado ocupa unos 84.6 MiB porque incluye varias arquitecturas.
La aplicación instalada no necesita Internet para esta transcripción ni envía el audio
a un servidor nuevo. El dictado externo del sistema puede tener sus propias condiciones.

Preparación independiente, si se necesita:

```bash
./gradlew prepararModeloVosk
```

Archivo descargado: `.gradle/vosk/modelo-es-0.42.zip`.
Assets generados: `app/src/main/assets/model-es/`, excluidos de Git para no versionar
el modelo binario. Una nueva máquina lo obtiene automáticamente al compilar.
SHA-256 del ZIP descargado y utilizado en esta verificación:
`09b239888f633ef2f0b4e09736e3d9936acfd810bc65d53fad45261762c6511f`.

Se verificó la presencia del modelo y bibliotecas ARM64 en el APK, la alineación del APK
para páginas de 16 KiB y la alineación PT_LOAD de las bibliotecas ARM64 Vosk y JNA.
Esto no sustituye ejecutarlas en un teléfono.

## Verificación real y pendientes

`./gradlew assembleDebug testDebugUnitTest lintDebug`: BUILD SUCCESSFUL.
Ocho pruebas unitarias pasaron; Lint no reporta errores y conserva 91 advertencias.
Las tres pruebas nuevas verifican conversión PCM: mono, mezcla estéreo y muestras inválidas.

Cinco pruebas instrumentadas están preparadas y compilaron: cuatro para SQLite/BLOB y
una que genera AAC silencioso, lo decodifica y comprueba el modelo nativo sin micrófono.
`connectedDebugAndroidTest` no pudo ejecutarlas: **No connected devices**.
No se ha comprobado la precisión de Vosk con voz real ni la recuperación BLOB en Android.

De parte del usuario solo falta encender un emulador o conectar un teléfono y probar una
grabación audible breve en español. La biblioteca y el modelo ya se descargaron; no se
necesita crear una cuenta, pagar un servicio ni entregar claves API.

## Fuentes oficiales

- Vosk Android: https://alphacephei.com/vosk/android
- Modelos y licencias: https://alphacephei.com/vosk/models
- Dependencias del ejemplo: https://github.com/alphacep/vosk-android-demo/blob/master/app/build.gradle
- API de Vosk: https://github.com/alphacep/vosk-api
- MediaCodec: https://developer.android.com/reference/android/media/MediaCodec

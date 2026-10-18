# Audio BLOB y transcripción — estado al 2026-10-18

## Implementado: archivos privados y copia binaria en SQLite

MediaRecorder genera AAC dentro de un archivo MPEG-4 con extensión `.m4a` en
`getFilesDir()/audios/`. El atributo original `String audio` y la columna TEXT
conservan una ruta relativa, por ejemplo `audios/nota_123.m4a`.

Desde la versión 3 de DatabaseHelper se añade:

```sql
CREATE TABLE audio_fragmentos (
    apunte_id INTEGER NOT NULL REFERENCES apuntes(id) ON DELETE CASCADE,
    parte INTEGER NOT NULL,
    datos BLOB NOT NULL,
    PRIMARY KEY (apunte_id, parte)
);
```

AudioBlobDAO lee el archivo en bloques de hasta 256 KiB y los guarda en SQLite.
ApunteDAO usa transacciones para que el apunte y sus bytes se guarden juntos.
Una recuperación concatena los bloques ordenados en un archivo temporal y lo mueve
a su ruta privada al finalizar. No se carga todo el audio en memoria ni en una sola
fila del Cursor. Las consultas del listado solo leen los metadatos de apuntes.

Se mantienen los archivos porque MediaPlayer reproduce desde el sistema de archivos.
Si falta uno y hay BLOB, el DAO puede reconstruirlo antes de reproducir o editar.
Los audios anteriores se respaldan en BLOB la primera vez que se usan con esta versión.
Si ya se perdió el archivo anterior antes de contar con respaldo BLOB, no se puede recuperar.
No se necesitan permisos de almacenamiento. Desinstalar la app elimina sus datos privados.

## Qué falta para transcribir el audio guardado

BLOB es almacenamiento: no reconoce palabras. Se conserva el dictado en vivo mediante
RecognizerIntent, como exige el examen. Todavía no hay un motor que transcriba el archivo.

### Recomendación: Vosk local

Vosk tiene una biblioteca Android y un modelo pequeño en español,
`vosk-model-small-es-0.42`, listado con tamaño de 39 MB y licencia Apache 2.0.
Permitiría procesar las grabaciones sin una API de pago y sin enviar el audio a un servidor.
Hay que añadir esa biblioteca y el modelo, lo cual amplía la restricción original de
usar solo APIs nativas. Se solicitó al usuario confirmar esa elección.

Flujo propuesto después de esa elección:

1. Recuperar desde BLOB el archivo privado del apunte cuando sea necesario.
2. Decodificar AAC/M4A a PCM con MediaExtractor y MediaCodec de Android.
3. Ejecutar Vosk fuera del hilo de la interfaz y liberar sus recursos al terminar.
4. Mostrar el resultado editable en DictadoActivity, sin sobrescribir automáticamente la descripción.
5. Guardar solo cuando el usuario confirme, mediante actualizarDescripcion().

Debe probarse con una grabación audible real. Ni BLOB ni un motor de reconocimiento
pueden recuperar palabras de un archivo que se grabó en silencio.

### Alternativa: reconocimiento nativo con fuente de audio

Desde API 33, RecognizerIntent.EXTRA_AUDIO_SOURCE permite entregar un
ParcelFileDescriptor con audio y especificar frecuencia, codificación y canales.
No está garantizado en todos los reconocedores: la documentación advierte que, si
no soporta la fuente, puede abrir el micrófono. El proyecto admite desde API 24.
Por eso esta alternativa no garantiza transcripción de archivos en todos los teléfonos.
Un M4A no debe entregarse como si sus bytes comprimidos fueran PCM.

## De parte del usuario

- Elegir Vosk local o aceptar la compatibilidad limitada de la alternativa nativa.
- Encender un emulador o conectar un teléfono con depuración USB para las pruebas SQLite.
- Facilitar una grabación audible breve en español para verificar la transcripción real.
- Para Vosk, permitir la descarga inicial de biblioteca y modelo; no se requiere clave API.
  Si la descarga no es accesible desde este entorno, colocar el ZIP oficial del modelo
  en la raíz del proyecto permitirá incorporarlo después de verificar su contenido.

## Fuentes oficiales

- Vosk Android: https://alphacephei.com/vosk/android
- Modelos y licencias: https://alphacephei.com/vosk/models
- Ejemplo Android oficial: https://github.com/alphacep/vosk-android-demo
- Fuente de audio nativa: https://developer.android.com/reference/android/speech/RecognizerIntent#EXTRA_AUDIO_SOURCE
- Decodificación: https://developer.android.com/reference/android/media/MediaCodec

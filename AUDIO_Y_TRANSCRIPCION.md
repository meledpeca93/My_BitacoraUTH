# Audio y transcripción: decisión técnica

## Almacenamiento implementado

El audio se graba con MediaRecorder, en contenedor MPEG-4 y codificación AAC,
con extensión `.m4a`. Se guarda en `getFilesDir()/audios/`.

SQLite guarda únicamente la ruta relativa, por ejemplo `audios/nota_123.m4a`.
`ArchivoAudio.obtenerArchivo()` combina esa ruta con el directorio privado actual.
No se necesitan permisos de almacenamiento, y la carpeta no es una carpeta pública
visible desde el explorador de archivos. Desinstalar la aplicación elimina sus datos privados.

La versión 2 de DatabaseHelper convierte las rutas absolutas anteriores en rutas
relativas sin eliminar los apuntes ni cambiar la columna `audio TEXT NOT NULL`.
ArchivoAudio también interpreta rutas antiguas, limita las operaciones a la carpeta
audios y rechaza rutas que intenten salir de ella.

Los archivos temporales descartados se eliminan. Al actualizar se conserva el audio
original hasta confirmar el guardado; al eliminar se comprueba que ningún otro apunte
use el archivo. Si no se puede comprobar, se conserva el archivo.

## Qué significa BLOB

BLOB significa Binary Large Object: permite guardar los bytes de un archivo dentro
de SQLite. No es una extensión de audio, un códec ni un servicio de transcripción.
Cambiar `TEXT` por `BLOB` no convierte voz a texto.

Para este examen se conserva el modelo original: `String audio` contiene la ruta.
No se añadió una columna BLOB ni se duplicaron los archivos en la base de datos.

## Reconocimiento implementado

DictadoActivity usa RecognizerIntent.ACTION_RECOGNIZE_SPEECH, idioma `es-HN` y una
Activity externa compatible. El usuario habla, revisa el resultado y guarda la descripción.
Este flujo no transcribe la grabación de MediaRecorder ya guardada.

## Posibilidad de transcribir una grabación existente

Desde API 33, RecognizerIntent.EXTRA_AUDIO_SOURCE permite proporcionar un
ParcelFileDescriptor con una fuente de audio, junto con codificación, canales y frecuencia.
El formato predeterminado documentado es PCM de 16 bits, un canal y 16 kHz.
El archivo actual AAC/M4A necesitaría decodificación a un formato compatible: no basta
con pasar sus bytes como si fueran PCM.

El soporte depende del reconocedor instalado. Android advierte que, si no soporta
esa fuente, puede abrir el micrófono. Por eso no se debe presentar ese flujo como una
transcripción fiable de archivos en todos los teléfonos. Además, el proyecto soporta
Android desde API 24.

Otra posibilidad sería un motor local especializado o un servicio de transcripción;
ambos requieren revisar dependencias, tamaño, conectividad y cambios en el alcance
original. No se integró ningún servicio ni se envió audio fuera de la aplicación por
una nueva función de transcripción de archivos.

Se solicitó al usuario elegir entre mantener dictado, investigar transcripción de
archivos o guardar BLOB. Hasta recibir esa elección se conserva la implementación
original de dictado y archivos privados.

## Fuentes

- RecognizerIntent y EXTRA_AUDIO_SOURCE: https://developer.android.com/reference/android/speech/RecognizerIntent#EXTRA_AUDIO_SOURCE
- MediaRecorder: https://developer.android.com/media/platform/mediarecorder

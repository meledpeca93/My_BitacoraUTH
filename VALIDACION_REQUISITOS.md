# Validación contra el examen — 2026-10-18

Actualización después de autorizar Vosk: la transcripción local de archivos ya está
implementada. La compilación completa pasa y el APK incluye el modelo español.
Pasaron 8 pruebas unitarias; Lint tiene cero errores y 91 advertencias. Cinco pruebas
instrumentadas compilaron, pero no se ejecutaron porque no hay dispositivos conectados.
La voz real, la audibilidad y la precisión siguen pendientes de prueba en Android.
Consultar AUDIO_Y_TRANSCRIPCION.md para los archivos, el flujo y las instrucciones.

La tabla y comprobaciones siguientes documentan la revisión de la rúbrica y la etapa BLOB
previa a esa integración; no certifican el reconocimiento real por haber compilado.

Fuente revisada: `ExamenPM01-P1-VersionC-Bitacora-de-Clases.docx`, incluidas las
referencias y la rúbrica de 15 puntos. Este informe distingue código implementado
de pruebas realmente ejecutadas. No certifica una calificación.

| Requisito oficial | Implementación | Estado de verificación |
|---|---|---|
| Nombre PM1EC + últimos cuatro dígitos | PM1EC0100 en settings.gradle | Revisado |
| Java o Kotlin | Java y layouts XML | Compila |
| MVC + DAO | model, controller, dao, database y Activities | Revisado: SQL fuera de Activities |
| Modelo id, asignatura, tema, fecha, descripción, audio (1) | Apunte.java conserva los seis atributos | Compila |
| MediaRecorder y SQLite (2) | MainActivity, controlador y DAO; audio privado y BLOB | Compila; audio audible pendiente |
| Alertas de obligatorios (1) | Controlador y AlertDialog | 5 pruebas unitarias totales pasan; diálogos pendientes en dispositivo |
| Regex de fecha y tema (1) | Regex + fecha estricta, letras con acentos y números | Pruebas unitarias pasan |
| Listar (1) | DAO y ListView con selección única | Compila; prueba funcional pendiente |
| Eliminar (0.5) | Confirmación y DELETE parametrizado | Prueba instrumentada preparada |
| Actualizar (0.5) | Registro en modo edición y UPDATE | Prueba instrumentada preparada |
| Filtro por asignatura con ? (2) | asignatura = ? | Revisado; prueba de entrada SQL preparada |
| Reproducir con MediaPlayer (2) | Registro y Listado, atributos multimedia | Problema de silencio reportado por usuario; pendiente de diagnóstico en teléfono |
| Dictado con RecognizerIntent (2) | DictadoActivity, español es-HN y edición manual | Compila; prueba de voz pendiente |
| RECORD_AUDIO y explicación ante rechazo (2) | Manifest + solicitud en ejecución + AlertDialog | Revisado; rechazo pendiente en dispositivo |
| Agregar asignaturas (extra en imagen) | Diálogo y SharedPreferences | Implementado; persistencia pendiente de prueba manual |
| GitHub y enlace | Git inicializado y remoto origin configurado | Publicación y contenido remoto no verificados; no se publicó nada en esta tarea |

Las tres pantallas del examen se mantienen. InicioActivity es una cuarta pantalla
adicional de bienvenida, autorizada por el usuario.

## Resultado de las comprobaciones ejecutadas

- `./gradlew testDebugUnitTest lintDebug`: primero detectó tres errores de Lint y
  ejecutó correctamente las pruebas. Se corrigieron los tres errores: atributo de
  navegación que requería API 27 y dos tintes de ImageView incompatibles con AppCompat.
- `./gradlew lintDebug`: **BUILD SUCCESSFUL**, cero errores y 84 advertencias
  (incluyen textos directamente en layouts y recomendaciones de interfaz).
- `./gradlew testDebugUnitTest`: **BUILD SUCCESSFUL**, 5 pruebas, cero fallos y cero errores.
- `./gradlew connectedDebugAndroidTest`: generó los APK, pero falló al intentar
  ejecutar pruebas: **No connected devices**. No se adjudican resultados exitosos a esas pruebas.
- `./gradlew assembleDebugAndroidTest`: **BUILD SUCCESSFUL**, incluyendo las nuevas
  pruebas BLOB. El ensamblado de app también completó en el intento instrumentado.
- Se ejecutó con SQLite de Python el SQL real de creación de audio_fragmentos:
  admite los fragmentos binarios y elimina sus filas en cascada al borrar el apunte.
  Esto no sustituye la prueba de AudioBlobDAO sobre SQLite Android.

## BLOB implementado

Versión de base de datos 3, migración sin borrar apuntes. Se conserva el campo
`audio TEXT` del modelo y se añade `audio_fragmentos(apunte_id, parte, datos BLOB)`.
Los bloques de hasta 256 KiB evitan consultar un audio completo en una fila del Cursor.

Insertar o actualizar guarda registro y audio en una transacción: si falla el archivo,
la operación SQLite se revierte. Preparar audio recupera el archivo privado si falta;
los audios anteriores se incorporan a BLOB al utilizarlos. Las copias binarias se
eliminan por clave foránea con ON DELETE CASCADE. El listado no consulta los BLOB.

Hay cuatro pruebas instrumentadas preparadas: CRUD y persistencia; migración 1 a 3;
recuperación exacta de un archivo de más de 3 MiB y cascada; rollback por archivo ausente.
El archivo de prueba usa bytes sintéticos: no sirve para verificar reconocimiento ni reproducción.

## Pendientes reales

1. Encender el Pixel o conectar un teléfono para ejecutar las cuatro pruebas SQLite.
2. Repetir la lista manual de VERIFICACION_FASE6.md, especialmente audio audible,
   permisos, rotación, navegación y dictado. El problema de silencio no está resuelto por almacenar BLOB.
3. Probar Vosk, ya integrado y con modelo español incluido en el APK, con una grabación
   audible. Confirmar cancelación, corrección manual y guardado de la descripción.
4. README.md y DEFENSA_EXAMEN.md todavía no existen; corresponden a la fase de
   documentación pendiente. No son sustituidos por este informe.
5. Revisar la entrega GitHub cuando el usuario autorice esa fase; no publicar sin autorización.

package com.uth.mybitacorauth;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteException;
import android.media.MediaPlayer;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.uth.mybitacorauth.controller.ApunteController;
import com.uth.mybitacorauth.model.Apunte;
import com.uth.mybitacorauth.util.ArchivoAudio;
import com.uth.mybitacorauth.util.IconosUI;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_APUNTE_ID = "apunte_id";
    private Spinner asignatura;
    private EditText tema, fecha, descripcion;
    private TextView duracion, estadoAudio;
    private Button grabar, reproducir, guardar;
    private ApunteController controller;
    private ArchivoAudio archivos;
    private ArrayAdapter<String> asignaturasAdapter;
    private MediaRecorder recorder;
    private MediaPlayer player;
    private String rutaAudio = "";
    private String audioOriginal = "";
    private String archivoEnGrabacion = "";
    private int apunteId;
    private long inicioGrabacion;
    private long segundosGrabados;
    private int amplitudMaxima;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable actualizarTiempo = new Runnable() {
        @Override public void run() {
            segundosGrabados = (SystemClock.elapsedRealtime() - inicioGrabacion) / 1000;
            mostrarDuracion();
            if (recorder != null) {
                try {
                    amplitudMaxima = Math.max(amplitudMaxima, recorder.getMaxAmplitude());
                    estadoAudio.setText(amplitudMaxima > 0 ? "Recibiendo audio · toque Detener al terminar"
                            : "Grabando · esperando señal del micrófono");
                } catch (IllegalStateException e) {
                    // El grabador puede estar deteniéndose; no se consulta de nuevo hasta el próximo ciclo.
                }
            }
            handler.postDelayed(this, 500);
        }
    };

    private final ActivityResultLauncher<String> permisoMicrofono = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), concedido -> {
                if (concedido) iniciarGrabacion();
                else mostrarMensaje("Permiso de micrófono",
                        "El micrófono permite grabar la nota de voz del apunte. "
                        + "Puede seguir completando el formulario y volver a tocar Grabar cuando desee.");
            });

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.ime());
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });
        controller = new ApunteController(this);
        archivos = new ArchivoAudio(getFilesDir());
        asignatura = findViewById(R.id.spAsignatura);
        tema = findViewById(R.id.edtTema);
        fecha = findViewById(R.id.edtFecha);
        descripcion = findViewById(R.id.edtDescripcion);
        duracion = findViewById(R.id.txtDuracion);
        estadoAudio = findViewById(R.id.txtAudio);
        grabar = findViewById(R.id.btnGrabar);
        reproducir = findViewById(R.id.btnReproducir);
        guardar = findViewById(R.id.btnGuardar);
        findViewById(R.id.btnVolverInicio).setOnClickListener(v -> finish());
        cargarAsignaturas();
        apunteId = getIntent().getIntExtra(EXTRA_APUNTE_ID, 0);
        if (savedInstanceState != null) restaurarFormulario(savedInstanceState);
        else if (apunteId > 0) cargarApunte();
        actualizarEstadoAudio();
        grabar.setOnClickListener(v -> gestionarGrabacion());
        reproducir.setOnClickListener(v -> reproducirAudio());
        guardar.setOnClickListener(v -> guardarApunte());
        findViewById(R.id.btnAgregarAsignatura).setOnClickListener(v -> agregarAsignatura());
        findViewById(R.id.btnVerApuntes).setOnClickListener(v -> abrirApuntes());
        if (apunteId > 0) {
            guardar.setText("Actualizar Apunte");
            ((TextView) findViewById(R.id.txtEncabezado)).setText("Editar apunte");
        }
    }

    private void cargarAsignaturas() {
        ArrayList<String> materias = new ArrayList<>(Arrays.asList("Seleccione una asignatura",
                "Programación Móvil I", "Base de Datos I", "Redes I", "Inglés Técnico"));
        Set<String> adicionales = getSharedPreferences("asignaturas", MODE_PRIVATE)
                .getStringSet("adicionales", new HashSet<>());
        for (String materia : new TreeSet<>(adicionales)) {
            if (!materias.contains(materia)) materias.add(materia);
        }
        asignaturasAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, materias);
        asignaturasAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        asignatura.setAdapter(asignaturasAdapter);
    }

    private void agregarAsignatura() {
        EditText entrada = new EditText(this);
        entrada.setHint("Nombre de la asignatura");
        AlertDialog dialogo = new AlertDialog.Builder(this).setTitle("Agregar asignatura")
                .setView(entrada).setNegativeButton("Cancelar", null)
                .setPositiveButton("Agregar", null).create();
        dialogo.setOnShowListener(d -> dialogo.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String nombre = entrada.getText().toString().trim();
                    if (nombre.isEmpty()) { entrada.setError("Escriba una asignatura."); return; }
                    if (nombre.equalsIgnoreCase("Todas")) {
                        entrada.setError("Use otro nombre; Todas identifica el filtro general."); return;
                    }
                    for (int i = 0; i < asignaturasAdapter.getCount(); i++) {
                        if (nombre.equalsIgnoreCase(asignaturasAdapter.getItem(i))) {
                            entrada.setError("La asignatura ya existe."); return;
                        }
                    }
                    Set<String> adicionales = new HashSet<>(getSharedPreferences("asignaturas", MODE_PRIVATE)
                            .getStringSet("adicionales", new HashSet<>()));
                    adicionales.add(nombre);
                    getSharedPreferences("asignaturas", MODE_PRIVATE).edit()
                            .putStringSet("adicionales", adicionales).apply();
                    asignaturasAdapter.add(nombre);
                    asignatura.setSelection(asignaturasAdapter.getPosition(nombre));
                    dialogo.dismiss();
                }));
        dialogo.show();
    }

    private void gestionarGrabacion() {
        if (recorder != null) { detenerGrabacion(); return; }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) iniciarGrabacion();
        else if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
            new AlertDialog.Builder(this).setTitle("Permiso de micrófono")
                    .setMessage("Necesitamos el micrófono para grabar la nota de voz del apunte.")
                    .setNegativeButton("Ahora no", null)
                    .setPositiveButton("Continuar", (d, w) -> permisoMicrofono.launch(Manifest.permission.RECORD_AUDIO))
                    .show();
        } else permisoMicrofono.launch(Manifest.permission.RECORD_AUDIO);
    }

    private void iniciarGrabacion() {
        if (recorder != null || isFinishing() || isDestroyed()) return;
        liberarReproductor();
        try {
            File carpeta = new File(getFilesDir(), "audios");
            if (!carpeta.exists() && !carpeta.mkdirs()) throw new IOException("No se pudo crear la carpeta.");
            archivoEnGrabacion = File.createTempFile("nota_", ".m4a", carpeta).getAbsolutePath();
            recorder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    ? new MediaRecorder(this) : new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setOutputFile(archivoEnGrabacion);
            recorder.setOnErrorListener((r, what, extra) -> {
                descartarGrabacion();
                mostrarMensaje("Grabación", "La grabación se interrumpió. Intente nuevamente.");
            });
            recorder.prepare();
            recorder.start();
            amplitudMaxima = 0;
            inicioGrabacion = SystemClock.elapsedRealtime();
            handler.post(actualizarTiempo);
            IconosUI.actualizar(grabar, R.drawable.ic_stop, "Detener grabación");
            ((TextView) findViewById(R.id.txtEtiquetaGrabar)).setText("Detener");
            reproducir.setEnabled(false);
            guardar.setEnabled(false);
            estadoAudio.setText("Grabando… toque Detener al terminar");
        } catch (IOException | RuntimeException e) {
            descartarGrabacion();
            mostrarMensaje("Grabación", "No se pudo iniciar la grabación. Compruebe el micrófono y su permiso.");
        }
    }

    private void detenerGrabacion() {
        if (recorder == null) return;
        boolean correcta = false;
        try {
            amplitudMaxima = Math.max(amplitudMaxima, recorder.getMaxAmplitude());
            recorder.stop();
            File audio = new File(archivoEnGrabacion);
            correcta = audio.isFile() && audio.length() > 0;
        } catch (RuntimeException e) {
            // Una grabación demasiado corta puede no producir datos válidos.
        } finally {
            recorder.release();
            recorder = null;
            handler.removeCallbacks(actualizarTiempo);
        }
        if (correcta) {
            borrarAudioTemporal();
            rutaAudio = archivos.rutaRelativa(archivoEnGrabacion);
            segundosGrabados = (SystemClock.elapsedRealtime() - inicioGrabacion) / 1000;
        } else {
            new File(archivoEnGrabacion).delete();
            mostrarMensaje("Grabación", "No se pudo completar el audio. Grabe durante unos segundos e intente de nuevo.");
        }
        archivoEnGrabacion = "";
        actualizarEstadoAudio();
        if (correcta && segundosGrabados >= 2 && amplitudMaxima == 0) {
            mostrarMensaje("Micrófono sin señal",
                    "La grabación no recibió señal del micrófono. Compruebe que esté habilitado y vuelva a grabar.");
        }
    }

    private void descartarGrabacion() {
        handler.removeCallbacks(actualizarTiempo);
        if (recorder != null) { recorder.release(); recorder = null; }
        if (!archivoEnGrabacion.isEmpty()) new File(archivoEnGrabacion).delete();
        archivoEnGrabacion = "";
        actualizarEstadoAudio();
    }

    private void reproducirAudio() {
        if (player != null) { liberarReproductor(); return; }
        File archivo = archivos.obtenerArchivo(rutaAudio);
        if (archivo == null || !archivo.isFile() || archivo.length() == 0) {
            mostrarMensaje("Audio", "Debe grabar una nota de voz antes de reproducirla."); return;
        }
        try {
            player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
            player.setDataSource(archivo.getAbsolutePath());
            player.setOnCompletionListener(p -> liberarReproductor());
            player.setOnErrorListener((p, what, extra) -> {
                liberarReproductor();
                mostrarMensaje("Audio", "No se pudo reproducir la grabación.");
                return true;
            });
            player.prepare();
            player.start();
            IconosUI.actualizar(reproducir, R.drawable.ic_stop, "Detener audio");
            ((TextView) findViewById(R.id.txtEtiquetaReproducir)).setText("Detener");
        } catch (IOException | RuntimeException e) {
            liberarReproductor();
            mostrarMensaje("Audio", "No se pudo abrir la grabación.");
        }
    }

    private void liberarReproductor() {
        if (player != null) { player.release(); player = null; }
        if (reproducir != null) {
            IconosUI.actualizar(reproducir, R.drawable.ic_play, "Reproducir grabación");
            ((TextView) findViewById(R.id.txtEtiquetaReproducir)).setText("Escuchar");
        }
    }

    private void guardarApunte() {
        if (recorder != null) { mostrarMensaje("Grabación", "Detenga la grabación antes de guardar."); return; }
        Apunte apunte = new Apunte(apunteId,
                asignatura.getSelectedItemPosition() == 0 ? "" : asignatura.getSelectedItem().toString(),
                tema.getText().toString(), fecha.getText().toString(),
                descripcion.getText().toString(), rutaAudio);
        String error = controller.validarApunte(apunte);
        if (error != null) { mostrarMensaje("Revise los datos", error); return; }
        try {
            boolean guardado = apunteId == 0 ? controller.insertar(apunte) > 0
                    : controller.actualizar(apunte) == 1;
            if (!guardado) { mostrarMensaje("Guardar", "No se guardó el apunte. Puede que ya no exista."); return; }
        } catch (SQLiteException | IllegalArgumentException e) {
            mostrarMensaje("Guardar", "No se pudo guardar el apunte. Revise los datos y el espacio disponible."); return;
        }
        liberarReproductor();
        // Se conserva el original hasta que SQLite confirma la actualización.
        if (!audioOriginal.isEmpty() && !audioOriginal.equals(rutaAudio)) {
            try {
                if (!controller.audioEnUso(audioOriginal)) archivos.eliminar(audioOriginal);
            } catch (SQLiteException | IllegalArgumentException e) {
                // Conservamos el archivo si no se puede comprobar su uso.
            }
        }
        rutaAudio = "";
        audioOriginal = "";
        if (apunteId > 0) {
            new AlertDialog.Builder(this).setTitle("Apunte actualizado")
                    .setMessage("Los cambios se guardaron correctamente.")
                    .setPositiveButton("Aceptar", (d, w) -> finish()).setCancelable(false).show();
        } else {
            tema.setText(""); fecha.setText(""); descripcion.setText("");
            asignatura.setSelection(0); segundosGrabados = 0;
            actualizarEstadoAudio();
            mostrarMensaje("Apunte guardado", "El apunte se guardó correctamente.");
        }
    }

    private void cargarApunte() {
        try {
            Apunte apunte = controller.buscarPorId(apunteId);
            if (apunte == null) throw new IllegalArgumentException();
            controller.prepararAudio(apunte);
            seleccionarAsignatura(apunte.getAsignatura());
            tema.setText(apunte.getTema()); fecha.setText(apunte.getFecha());
            descripcion.setText(apunte.getDescripcion());
            rutaAudio = apunte.getAudio(); audioOriginal = rutaAudio;
        } catch (SQLiteException | IllegalArgumentException e) {
            new AlertDialog.Builder(this).setTitle("Apunte")
                    .setMessage("No se pudo cargar el apunte.").setCancelable(false)
                    .setPositiveButton("Aceptar", (d, w) -> finish()).show();
        }
    }

    private void seleccionarAsignatura(String nombre) {
        if (asignaturasAdapter.getPosition(nombre) < 0) asignaturasAdapter.add(nombre);
        asignatura.setSelection(asignaturasAdapter.getPosition(nombre));
    }

    private void abrirApuntes() {
        if (recorder != null) { mostrarMensaje("Grabación", "Detenga la grabación antes de continuar."); return; }
        liberarReproductor();
        startActivity(new Intent(this, ApuntesActivity.class));
    }

    private void mostrarDuracion() {
        duracion.setText(String.format(Locale.ROOT, "%02d:%02d", segundosGrabados / 60, segundosGrabados % 60));
    }

    private void actualizarEstadoAudio() {
        IconosUI.actualizar(grabar, R.drawable.ic_mic, "Grabar nota de voz");
        ((TextView) findViewById(R.id.txtEtiquetaGrabar)).setText("Grabar");
        reproducir.setEnabled(true); guardar.setEnabled(true);
        estadoAudio.setText(rutaAudio.isEmpty() ? "Sin grabación" : "Grabación disponible");
        mostrarDuracion();
    }

    private void mostrarMensaje(String titulo, String mensaje) {
        if (!isFinishing() && !isDestroyed()) new AlertDialog.Builder(this)
                .setTitle(titulo).setMessage(mensaje).setPositiveButton("Aceptar", null).show();
    }

    private void borrarAudioTemporal() {
        if (!rutaAudio.isEmpty() && !rutaAudio.equals(audioOriginal)) archivos.eliminar(rutaAudio);
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        if (recorder != null) detenerGrabacion();
        outState.putString("audio", rutaAudio);
        outState.putString("original", audioOriginal);
        outState.putLong("segundos", segundosGrabados);
        outState.putString("asignatura", asignatura.getSelectedItem().toString());
        super.onSaveInstanceState(outState);
    }

    private void restaurarFormulario(Bundle estado) {
        rutaAudio = estado.getString("audio", "");
        audioOriginal = estado.getString("original", "");
        segundosGrabados = estado.getLong("segundos", 0);
        seleccionarAsignatura(estado.getString("asignatura", "Seleccione una asignatura"));
        // Android restaura automáticamente los EditText gracias a sus identificadores.
    }

    @Override protected void onStop() {
        if (recorder != null) detenerGrabacion();
        liberarReproductor();
        super.onStop();
    }

    @Override protected void onDestroy() {
        handler.removeCallbacks(actualizarTiempo);
        if (recorder != null) descartarGrabacion();
        liberarReproductor();
        if (isFinishing()) borrarAudioTemporal();
        if (controller != null) controller.cerrar();
        super.onDestroy();
    }
}

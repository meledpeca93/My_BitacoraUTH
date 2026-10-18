package com.uth.mybitacorauth;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.Context;
import android.util.Log;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.uth.mybitacorauth.controller.ApunteController;
import com.uth.mybitacorauth.model.Apunte;
import com.uth.mybitacorauth.util.IconosUI;
import com.uth.mybitacorauth.audio.TranscriptorAudio;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import java.util.ArrayList;

public class DictadoActivity extends AppCompatActivity {
    private ApunteController controller;
    private EditText texto;
    private Button microfono, guardar, transcribir;
    private TextView estadoTranscripcion;
    private boolean transcribiendo;
    private int solicitud;
    private Future<?> trabajo;
    private final ExecutorService ejecutor = Executors.newSingleThreadExecutor();
    private int apunteId;
    private boolean reconociendo;
    private boolean apunteCargado;

    private final ActivityResultLauncher<Intent> dictado = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), resultado -> {
                reconociendo = false;
                actualizarBotones();
                if (resultado.getResultCode() != RESULT_OK) {
                    Toast.makeText(this, "Dictado cancelado o sin resultado. Su texto se conserva.",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                Intent datos = resultado.getData();
                ArrayList<String> resultados = datos == null ? null
                        : datos.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (resultados == null || resultados.isEmpty() || resultados.get(0) == null
                        || resultados.get(0).trim().isEmpty()) {
                    mostrarMensaje("Dictado", "No se reconoció texto. Puede intentarlo otra vez o escribir manualmente.");
                    return;
                }
                texto.setText(resultados.get(0));
                texto.setSelection(texto.length());
            });

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dictado);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.ime());
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });
        controller = new ApunteController(this);
        texto = findViewById(R.id.edtTextoDictado);
        microfono = findViewById(R.id.btnMicrofono);
        guardar = findViewById(R.id.btnGuardarDescripcion);
        transcribir = findViewById(R.id.btnTranscribirAudio);
        estadoTranscripcion = findViewById(R.id.txtEstadoTranscripcion);
        apunteId = getIntent().getIntExtra(MainActivity.EXTRA_APUNTE_ID, 0);
        reconociendo = savedInstanceState != null && savedInstanceState.getBoolean("reconociendo");
        cargarApunte(savedInstanceState);
        findViewById(R.id.btnAtrasDictado).setOnClickListener(v -> finish());
        microfono.setOnClickListener(v -> iniciarDictado());
        guardar.setOnClickListener(v -> guardarDescripcion());
        transcribir.setOnClickListener(v -> {
            if (transcribiendo) cancelarTranscripcion();
            else confirmarTranscripcion();
        });
        actualizarBotones();
    }

    private void cargarApunte(Bundle estado) {
        try {
            Apunte apunte = controller.buscarPorId(apunteId);
            if (apunte == null) {
                mostrarErrorCarga("El apunte seleccionado ya no existe."); return;
            }
            ((TextView) findViewById(R.id.txtTemaDictado)).setText(apunte.getTema());
            // Conserva las correcciones al girar el teléfono o regresar del reconocedor.
            texto.setText(estado == null ? apunte.getDescripcion() : estado.getString("texto", ""));
            apunteCargado = true;
        } catch (SQLiteException e) {
            mostrarErrorCarga("No se pudo consultar el apunte.");
        }
    }

    private void mostrarErrorCarga(String mensaje) {
        apunteCargado = false;
        texto.setEnabled(false);
        new AlertDialog.Builder(this).setTitle("Apunte").setMessage(mensaje)
                .setCancelable(false).setPositiveButton("Aceptar", (d, w) -> finish()).show();
    }

    private void iniciarDictado() {
        if (reconociendo || transcribiendo || !apunteCargado) return;
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-HN");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Dicte la descripción del apunte");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        try {
            reconociendo = true;
            actualizarBotones();
            // El reconocimiento lo realiza otra Activity; aquí no abrimos el micrófono directamente.
            dictado.launch(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            reconociendo = false;
            actualizarBotones();
            mostrarMensaje("Dictado no disponible",
                    "No se pudo abrir el reconocimiento de voz. Puede escribir la descripción manualmente.");
        }
    }

    private void guardarDescripcion() {
        if (!apunteCargado || reconociendo || transcribiendo) return;
        String descripcion = texto.getText().toString();
        String error = ApunteController.validarDescripcion(descripcion);
        if (error != null) { mostrarMensaje("Descripción", error); return; }
        try {
            // Solo cambia la descripción, sin modificar el tema, la fecha ni el audio.
            if (controller.actualizarDescripcion(apunteId, descripcion) != 1) {
                mostrarMensaje("Guardar", "No se pudo actualizar la descripción. El apunte puede haber sido eliminado.");
                return;
            }
            mostrarMensaje("Descripción guardada", "La descripción se actualizó correctamente.");
        } catch (SQLiteException | IllegalArgumentException e) {
            mostrarMensaje("Guardar", "No se pudo guardar la descripción. Su texto permanece disponible para reintentar.");
        }
    }

    private void confirmarTranscripcion() {
        if (!apunteCargado || reconociendo) return;
        new AlertDialog.Builder(this).setTitle("Transcribir grabación")
                .setMessage("El resultado reemplazará el texto de este campo. La descripción guardada "
                        + "solo cambiará cuando toque Guardar Descripción.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Transcribir", (d, w) -> iniciarTranscripcion()).show();
    }

    private void iniciarTranscripcion() {
        if (transcribiendo || reconociendo || !apunteCargado) return;
        transcribiendo = true;
        int numero = ++solicitud;
        estadoTranscripcion.setText("Preparando y transcribiendo la grabación…");
        actualizarBotones();
        Context contexto = getApplicationContext();
        int id = apunteId;
        trabajo = ejecutor.submit(() -> {
            // El trabajo posee su propio controlador para poder cerrar la pantalla con seguridad.
            ApunteController datos = new ApunteController(contexto);
            String resultado = "";
            String error = null;
            try {
                Apunte apunte = datos.buscarPorId(id);
                if (apunte == null) throw new IOException("El apunte seleccionado ya no existe.");
                File archivo = datos.prepararAudio(apunte);
                resultado = new TranscriptorAudio().transcribir(contexto, archivo);
            } catch (IOException e) {
                error = e.getMessage();
            } catch (RuntimeException | LinkageError e) {
                Log.e("DictadoActivity", "Error al transcribir la grabación", e);
                error = "No se pudo transcribir la grabación en este dispositivo. Su texto se conserva.";
            } finally { datos.cerrar(); }
            String textoFinal = resultado;
            String errorFinal = error;
            runOnUiThread(() -> terminarTranscripcion(numero, textoFinal, errorFinal));
        });
    }

    private void terminarTranscripcion(int numero, String resultado, String error) {
        if (isFinishing() || isDestroyed() || numero != solicitud) return;
        transcribiendo = false;
        actualizarBotones();
        if (error != null) {
            estadoTranscripcion.setText("No se completó la transcripción. El texto anterior se conserva.");
            mostrarMensaje("Transcripción", error);
        } else if (resultado.trim().isEmpty()) {
            estadoTranscripcion.setText("No se reconocieron palabras.");
            mostrarMensaje("Transcripción", "No se reconocieron palabras en la grabación. "
                    + "Compruebe que contenga voz audible o escriba la descripción manualmente.");
        } else {
            texto.setText(resultado);
            texto.setSelection(texto.length());
            estadoTranscripcion.setText("Transcripción lista. Revise el texto y guarde la descripción.");
        }
    }

    private void cancelarTranscripcion() {
        solicitud++;
        if (trabajo != null) trabajo.cancel(true);
        transcribiendo = false;
        estadoTranscripcion.setText("Transcripción cancelada. Su texto se conserva.");
        actualizarBotones();
    }

    private void actualizarBotones() {
        if (microfono != null) {
            microfono.setEnabled(apunteCargado && !reconociendo && !transcribiendo);
            IconosUI.actualizar(microfono, reconociendo ? R.drawable.ic_stop : R.drawable.ic_mic,
                    reconociendo ? "Reconocimiento en curso" : "Dictar descripción");
        }
        if (guardar != null) guardar.setEnabled(apunteCargado && !reconociendo && !transcribiendo);
        if (transcribir != null) {
            transcribir.setEnabled(apunteCargado && !reconociendo);
            transcribir.setText(transcribiendo ? "Cancelar transcripción" : "Transcribir audio guardado");
        }
        if (texto != null) texto.setEnabled(apunteCargado && !transcribiendo);
    }

    private void mostrarMensaje(String titulo, String mensaje) {
        if (!isFinishing() && !isDestroyed()) new AlertDialog.Builder(this).setTitle(titulo)
                .setMessage(mensaje).setPositiveButton("Aceptar", null).show();
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        outState.putString("texto", texto.getText().toString());
        outState.putBoolean("reconociendo", reconociendo);
        super.onSaveInstanceState(outState);
    }

    @Override protected void onDestroy() {
        solicitud++;
        if (trabajo != null) trabajo.cancel(true);
        ejecutor.shutdownNow();
        if (controller != null) controller.cerrar();
        super.onDestroy();
    }
}

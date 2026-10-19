package com.uth.mybitacorauth;

import com.uth.mybitacorauth.util.AvisosUI;

import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.media.MediaPlayer;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.uth.mybitacorauth.controller.ApunteController;
import com.uth.mybitacorauth.model.Apunte;
import com.uth.mybitacorauth.util.ArchivoAudio;
import com.uth.mybitacorauth.util.IconosUI;
import com.uth.mybitacorauth.view.ApunteAdapter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;

public class ApuntesActivity extends AppCompatActivity {
    private ApunteController controller;
    private Spinner filtro;
    private ListView lista;
    private TextView listaVacia;
    private Button reproducir;
    private ApunteAdapter apuntesAdapter;
    private List<Apunte> apuntes = new ArrayList<>();
    private int seleccionadoId;
    private String asignaturaActual = "Todas";
    private MediaPlayer player;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_apuntes);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });
        controller = new ApunteController(this);
        filtro = findViewById(R.id.spFiltroAsignatura);
        lista = findViewById(R.id.listaApuntes);
        listaVacia = findViewById(R.id.txtListaVacia);
        reproducir = findViewById(R.id.btnReproducirNota);
        lista.setEmptyView(findViewById(R.id.contenedorVacio));
        if (savedInstanceState != null) {
            seleccionadoId = savedInstanceState.getInt("seleccionado", 0);
            asignaturaActual = savedInstanceState.getString("filtro", "Todas");
        }
        lista.setOnItemClickListener((parent, view, position, id) -> {
            liberarReproductor();
            seleccionadoId = apuntes.get(position).getId();
            lista.setItemChecked(position, true);
            apuntesAdapter.seleccionar(seleccionadoId);
            mostrarSeleccion();
        });
        findViewById(R.id.btnAtras).setOnClickListener(v -> finish());
        reproducir.setOnClickListener(v -> reproducirNota());
        findViewById(R.id.btnActualizar).setOnClickListener(v -> actualizarApunte());
        findViewById(R.id.btnEliminar).setOnClickListener(v -> confirmarEliminacion());
        findViewById(R.id.btnDictar).setOnClickListener(v -> abrirDictado());
    }

    @Override protected void onResume() {
        super.onResume();
        cargarFiltros();
    }

    private void cargarFiltros() {
        try {
            TreeSet<String> materias = new TreeSet<>(Arrays.asList("Programación Móvil I",
                    "Base de Datos I", "Redes I", "Inglés Técnico"));
            materias.addAll(getSharedPreferences("asignaturas", MODE_PRIVATE)
                    .getStringSet("adicionales", new HashSet<>()));
            // Incluye también las asignaturas de registros existentes.
            for (Apunte apunte : controller.listarTodos()) materias.add(apunte.getAsignatura());
            ArrayList<String> opciones = new ArrayList<>();
            opciones.add("Todas"); opciones.addAll(materias);
            filtro.setOnItemSelectedListener(null);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, opciones);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            filtro.setAdapter(adapter);
            int posicion = opciones.indexOf(asignaturaActual);
            filtro.setSelection(posicion < 0 ? 0 : posicion);
            filtro.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String nuevaAsignatura = parent.getItemAtPosition(position).toString();
                    if (!nuevaAsignatura.equals(asignaturaActual)) seleccionadoId = 0;
                    asignaturaActual = nuevaAsignatura;
                    cargarApuntes();
                }
                @Override public void onNothingSelected(AdapterView<?> parent) { }
            });
        } catch (SQLiteException e) {
            mostrarErrorConsulta();
        }
    }

    private void cargarApuntes() {
        liberarReproductor();
        try {
            apuntes = "Todas".equals(asignaturaActual) ? controller.listarTodos()
                    : controller.filtrarPorAsignatura(asignaturaActual);
            int posicionSeleccionada = -1;
            for (int i = 0; i < apuntes.size(); i++) {
                Apunte apunte = apuntes.get(i);
                if (apunte.getId() == seleccionadoId) posicionSeleccionada = i;
            }
            apuntesAdapter = new ApunteAdapter(this, apuntes, seleccionadoId, apunte -> abrirTexto(apunte));
            lista.setAdapter(apuntesAdapter);
            lista.clearChoices();
            if (posicionSeleccionada >= 0) lista.setItemChecked(posicionSeleccionada, true);
            else seleccionadoId = 0;
            ((TextView) findViewById(R.id.txtCantidad)).setText(apuntes.size() + " apuntes guardados");
            mostrarSeleccion();
            listaVacia.setText("Todas".equals(asignaturaActual) ? "No hay apuntes guardados."
                    : "No hay apuntes de esta asignatura.");
        } catch (SQLiteException | IllegalArgumentException e) {
            mostrarErrorConsulta();
        }
    }

    private void mostrarErrorConsulta() {
        apuntes = new ArrayList<>();
        seleccionadoId = 0;
        mostrarSeleccion();
        ((TextView) findViewById(R.id.txtCantidad)).setText("Sin datos disponibles");
        apuntesAdapter = new ApunteAdapter(this, apuntes, 0, apunte -> abrirTexto(apunte));
        lista.setAdapter(apuntesAdapter);
        listaVacia.setText("No se pudo cargar el listado.");
        mostrarMensaje("Apuntes", "No se pudieron consultar los apuntes. Intente abrir el listado nuevamente.");
    }

    private void mostrarSeleccion() {
        String mensaje = "Selecciona un apunte para continuar";
        for (Apunte apunte : apuntes) {
            if (apunte.getId() == seleccionadoId) mensaje = "Seleccionado: " + apunte.getTema();
        }
        ((TextView) findViewById(R.id.txtSeleccion)).setText(mensaje);
    }

    private Apunte obtenerSeleccionado() {
        for (Apunte apunte : apuntes) {
            if (apunte.getId() == seleccionadoId) return apunte;
        }
        mostrarMensaje("Seleccione un apunte", "Toque un elemento de la lista antes de realizar esta acción.");
        return null;
    }

    private void reproducirNota() {
        if (player != null) { liberarReproductor(); return; }
        Apunte apunte = obtenerSeleccionado();
        if (apunte == null) return;
        try {
            File archivo = controller.prepararAudio(apunte);
            player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
            player.setDataSource(archivo.getAbsolutePath());
            player.setOnCompletionListener(p -> liberarReproductor());
            player.setOnErrorListener((p, what, extra) -> {
                liberarReproductor();
                mostrarMensaje("Audio", "No se pudo reproducir la nota de voz.");
                return true;
            });
            player.prepare(); player.start();
            IconosUI.actualizar(reproducir, R.drawable.ic_stop, "Detener audio");
        } catch (IOException | RuntimeException e) {
            liberarReproductor();
            mostrarMensaje("Audio", "No se pudo abrir la nota de voz.");
        }
    }

    private void actualizarApunte() {
        Apunte apunte = obtenerSeleccionado();
        if (apunte == null) return;
        liberarReproductor();
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_APUNTE_ID, apunte.getId());
        startActivity(intent);
    }

    private void confirmarEliminacion() {
        Apunte apunte = obtenerSeleccionado();
        if (apunte == null) return;
        new AlertDialog.Builder(this).setTitle("Eliminar apunte")
                .setMessage("¿Desea eliminar el apunte «" + apunte.getTema() + "»?")
                .setNegativeButton("No", null)
                .setPositiveButton("Sí", (dialog, which) -> eliminarApunte(apunte)).show();
    }

    private void eliminarApunte(Apunte apunte) {
        liberarReproductor();
        try {
            if (controller.eliminar(apunte.getId()) != 1) {
                mostrarMensaje("Eliminar", "El apunte ya no existe o no pudo eliminarse.");
                cargarApuntes(); return;
            }
        } catch (SQLiteException | IllegalArgumentException e) {
            mostrarMensaje("Eliminar", "No se pudo eliminar el apunte. Intente nuevamente."); return;
        }
        String mensaje = "El apunte se eliminó correctamente.";
        try {
            // Solo borramos el archivo privado si ningún registro lo utiliza.
            if (!controller.audioEnUso(apunte.getAudio())
                    && !new ArchivoAudio(getFilesDir()).eliminar(apunte.getAudio())) {
                mensaje += " No se pudo borrar el archivo de audio.";
            }
        } catch (SQLiteException | IllegalArgumentException e) {
            mensaje += " El archivo de audio se conservó porque no se pudo comprobar o eliminar.";
        }
        seleccionadoId = 0;
        cargarApuntes();
        mostrarMensaje("Eliminar", mensaje);
    }

    private void abrirDictado() {
        Apunte apunte = obtenerSeleccionado();
        if (apunte == null) return;
        liberarReproductor();
        Intent intent = new Intent(this, DictadoActivity.class);
        intent.putExtra(MainActivity.EXTRA_APUNTE_ID, apunte.getId());
        startActivity(intent);
    }

    private void abrirTexto(Apunte apunte) {
        liberarReproductor();
        seleccionadoId = apunte.getId();
        lista.setItemChecked(apuntes.indexOf(apunte), true);
        apuntesAdapter.seleccionar(seleccionadoId);
        mostrarSeleccion();
        Intent intent = new Intent(this, DictadoActivity.class);
        intent.putExtra(MainActivity.EXTRA_APUNTE_ID, apunte.getId());
        intent.putExtra(DictadoActivity.EXTRA_TRANSCRIBIR_AUDIO, true);
        startActivity(intent);
    }

    private void liberarReproductor() {
        if (player != null) { player.release(); player = null; }
        if (reproducir != null) IconosUI.actualizar(reproducir, R.drawable.ic_play, "Reproducir nota de voz");
    }

    private void mostrarMensaje(String titulo, String mensaje) {
        AvisosUI.mostrar(this, titulo, mensaje);
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("seleccionado", seleccionadoId);
        outState.putString("filtro", asignaturaActual);
        super.onSaveInstanceState(outState);
    }

    @Override protected void onStop() {
        liberarReproductor();
        super.onStop();
    }

    @Override protected void onDestroy() {
        liberarReproductor();
        if (controller != null) controller.cerrar();
        super.onDestroy();
    }
}

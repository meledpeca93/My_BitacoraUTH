package com.uth.mybitacorauth.controller;

import android.content.Context;
import com.uth.mybitacorauth.dao.ApunteDAO;
import com.uth.mybitacorauth.model.Apunte;
import com.uth.mybitacorauth.util.ArchivoAudio;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ApunteController {
    private final ApunteDAO dao;
    private final ArchivoAudio archivos;

    public ApunteController(Context context) {
        dao = new ApunteDAO(context);
        archivos = new ArchivoAudio(context.getFilesDir());
    }

    // Devuelve null si es válido; de lo contrario devuelve el mensaje para la vista.
    public String validarApunte(Apunte apunte) {
        return validarDatos(apunte, apunte == null ? null : archivos.obtenerArchivo(apunte.getAudio()));
    }

    public static String validar(Apunte apunte) {
        File archivo = apunte == null || estaVacio(apunte.getAudio()) ? null : new File(apunte.getAudio());
        return validarDatos(apunte, archivo);
    }

    private static String validarDatos(Apunte apunte, File audio) {
        if (apunte == null) return "No se recibieron los datos del apunte.";
        if (estaVacio(apunte.getAsignatura())) return "Seleccione una asignatura.";
        if (estaVacio(apunte.getTema())) return "Debe escribir un tema.";
        if (!apunte.getTema().trim().matches("[\\p{L}\\p{M}0-9 ]+")) {
            return "El tema solo permite letras, números y espacios.";
        }
        if (estaVacio(apunte.getFecha())) return "Debe escribir una fecha.";
        if (!esFechaValida(apunte.getFecha())) {
            return "Ingrese una fecha real con formato AAAA-MM-DD.";
        }
        String error = validarDescripcion(apunte.getDescripcion());
        if (error != null) return error;
        if (estaVacio(apunte.getAudio())) return "Debe grabar una nota de voz.";
        if (audio == null || !audio.isFile() || audio.length() == 0) {
            return "La grabación no existe o está vacía. Grabe nuevamente.";
        }
        return null;
    }

    public static String validarDescripcion(String descripcion) {
        return estaVacio(descripcion) ? "Debe escribir una descripción." : null;
    }

    public static boolean esFechaValida(String fecha) {
        if (fecha == null || !fecha.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) return false;
        SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        formato.setLenient(false);
        try {
            return formato.parse(fecha) != null;
        } catch (ParseException e) {
            return false;
        }
    }

    private static boolean estaVacio(String texto) {
        return texto == null || texto.trim().isEmpty();
    }

    public long insertar(Apunte apunte) {
        comprobarDatos(apunte);
        normalizar(apunte);
        return dao.insertar(apunte);
    }

    public int actualizar(Apunte apunte) {
        comprobarDatos(apunte);
        if (apunte.getId() <= 0) throw new IllegalArgumentException("Seleccione un apunte válido.");
        normalizar(apunte);
        return dao.actualizar(apunte);
    }

    public int actualizarDescripcion(int id, String descripcion) {
        if (id <= 0) throw new IllegalArgumentException("Seleccione un apunte válido.");
        String error = validarDescripcion(descripcion);
        if (error != null) throw new IllegalArgumentException(error);
        return dao.actualizarDescripcion(id, descripcion.trim());
    }

    public List<Apunte> listarTodos() { return dao.listarTodos(); }

    public List<Apunte> filtrarPorAsignatura(String asignatura) {
        if (estaVacio(asignatura)) throw new IllegalArgumentException("Seleccione una asignatura.");
        return dao.filtrarPorAsignatura(asignatura.trim());
    }

    public Apunte buscarPorId(int id) { return dao.buscarPorId(id); }

    public int eliminar(int id) {
        if (id <= 0) throw new IllegalArgumentException("Seleccione un apunte válido.");
        // El archivo se eliminará desde la vista solo si este resultado es 1.
        return dao.eliminar(id);
    }

    private void comprobarDatos(Apunte apunte) {
        String error = validarApunte(apunte);
        if (error != null) throw new IllegalArgumentException(error);
    }

    private void normalizar(Apunte apunte) {
        apunte.setAudio(archivos.rutaRelativa(apunte.getAudio()));
        apunte.setAsignatura(apunte.getAsignatura().trim());
        apunte.setTema(apunte.getTema().trim());
        apunte.setDescripcion(apunte.getDescripcion().trim());
    }

    public boolean audioEnUso(String ruta) { return dao.audioEnUso(archivos.rutaRelativa(ruta)); }

    public void cerrar() { dao.cerrar(); }
}

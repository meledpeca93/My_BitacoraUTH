package com.uth.mybitacorauth.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.uth.mybitacorauth.database.DatabaseHelper;
import com.uth.mybitacorauth.model.Apunte;

import java.io.File;
import java.io.IOException;
import android.database.sqlite.SQLiteException;
import com.uth.mybitacorauth.util.ArchivoAudio;
import java.util.ArrayList;
import java.util.List;

public class ApunteDAO {
    private final DatabaseHelper helper;
    private final ArchivoAudio archivos;

    public ApunteDAO(Context context) {
        helper = new DatabaseHelper(context);
        archivos = new ArchivoAudio(context.getFilesDir());
    }

    public long insertar(Apunte apunte) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            long id = db.insertOrThrow(DatabaseHelper.TABLA_APUNTES, null, crearValores(apunte));
            AudioBlobDAO.guardar(db, (int) id, archivos.obtenerArchivo(apunte.getAudio()));
            db.setTransactionSuccessful();
            return id;
        } catch (IOException e) {
            throw new SQLiteException("No se pudo guardar el audio en SQLite: " + e.getMessage());
        } finally { db.endTransaction(); }
    }

    public List<Apunte> listarTodos() {
        return consultar(null, null);
    }

    public List<Apunte> filtrarPorAsignatura(String asignatura) {
        return consultar("asignatura = ?", new String[]{asignatura});
    }

    public Apunte buscarPorId(int id) {
        List<Apunte> resultados = consultar("id = ?", new String[]{String.valueOf(id)});
        return resultados.isEmpty() ? null : resultados.get(0);
    }

    public int actualizar(Apunte apunte) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            int filas = db.update(DatabaseHelper.TABLA_APUNTES, crearValores(apunte),
                    "id = ?", new String[]{String.valueOf(apunte.getId())});
            if (filas == 1) AudioBlobDAO.guardar(db, apunte.getId(), archivos.obtenerArchivo(apunte.getAudio()));
            db.setTransactionSuccessful();
            return filas;
        } catch (IOException e) {
            throw new SQLiteException("No se pudo actualizar el audio: " + e.getMessage());
        } finally { db.endTransaction(); }
    }

    public int actualizarDescripcion(int id, String descripcion) {
        ContentValues valores = new ContentValues();
        valores.put("descripcion", descripcion);
        return helper.getWritableDatabase().update(DatabaseHelper.TABLA_APUNTES, valores,
                "id = ?", new String[]{String.valueOf(id)});
    }

    public int eliminar(int id) {
        SQLiteDatabase db = helper.getWritableDatabase();
        return db.delete(DatabaseHelper.TABLA_APUNTES, "id = ?",
                new String[]{String.valueOf(id)});
    }

    public File prepararAudio(Apunte apunte) {
        File archivo = archivos.obtenerArchivo(apunte.getAudio());
        SQLiteDatabase db = helper.getWritableDatabase();
        try {
            if (archivo == null) throw new IOException("Ruta de audio no válida.");
            if (!archivo.isFile() || archivo.length() == 0) {
                if (!AudioBlobDAO.recuperar(db, apunte.getId(), archivo)) {
                    throw new IOException("No existe el archivo ni una copia BLOB del audio.");
                }
            }
            // Incorpora a BLOB los audios anteriores cuando se utilizan por primera vez.
            if (!AudioBlobDAO.existe(db, apunte.getId())) {
                db.beginTransaction();
                try {
                    AudioBlobDAO.guardar(db, apunte.getId(), archivo);
                    db.setTransactionSuccessful();
                } finally { db.endTransaction(); }
            }
            return archivo;
        } catch (IOException e) {
            throw new SQLiteException("No se pudo recuperar o respaldar el audio: " + e.getMessage());
        }
    }

    public boolean audioEnUso(String ruta) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor cursor = db.query(DatabaseHelper.TABLA_APUNTES, new String[]{"id"},
                "audio = ?", new String[]{ruta}, null, null, null, "1")) {
            return cursor.moveToFirst();
        }
    }

    private List<Apunte> consultar(String seleccion, String[] argumentos) {
        List<Apunte> apuntes = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        // try-with-resources cierra el Cursor incluso si ocurre un error.
        try (Cursor cursor = db.query(DatabaseHelper.TABLA_APUNTES, null,
                seleccion, argumentos, null, null, "fecha DESC, id DESC")) {
            while (cursor.moveToNext()) {
                apuntes.add(new Apunte(
                        cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("asignatura")),
                        cursor.getString(cursor.getColumnIndexOrThrow("tema")),
                        cursor.getString(cursor.getColumnIndexOrThrow("fecha")),
                        cursor.getString(cursor.getColumnIndexOrThrow("descripcion")),
                        cursor.getString(cursor.getColumnIndexOrThrow("audio"))));
            }
        }
        return apuntes;
    }

    private ContentValues crearValores(Apunte apunte) {
        ContentValues valores = new ContentValues();
        valores.put("asignatura", apunte.getAsignatura());
        valores.put("tema", apunte.getTema());
        valores.put("fecha", apunte.getFecha());
        valores.put("descripcion", apunte.getDescripcion());
        valores.put("audio", apunte.getAudio());
        return valores;
    }

    // La Activity cerrará su controlador al destruirse; no se cierra entre consultas.
    public void cerrar() {
        helper.close();
    }
}

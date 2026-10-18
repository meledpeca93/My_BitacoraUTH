package com.uth.mybitacorauth.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.uth.mybitacorauth.database.DatabaseHelper;
import com.uth.mybitacorauth.model.Apunte;

import java.util.ArrayList;
import java.util.List;

public class ApunteDAO {
    private final DatabaseHelper helper;

    public ApunteDAO(Context context) {
        helper = new DatabaseHelper(context);
    }

    public long insertar(Apunte apunte) {
        SQLiteDatabase db = helper.getWritableDatabase();
        return db.insertOrThrow(DatabaseHelper.TABLA_APUNTES, null, crearValores(apunte));
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
        return db.update(DatabaseHelper.TABLA_APUNTES, crearValores(apunte),
                "id = ?", new String[]{String.valueOf(apunte.getId())});
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

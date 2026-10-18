package com.uth.mybitacorauth.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

/** Guarda el audio en bloques para no cargar un BLOB grande en un CursorWindow. */
class AudioBlobDAO {
    private static final int TAMANO_BLOQUE = 256 * 1024;

    static void guardar(SQLiteDatabase db, int id, File archivo) throws IOException {
        if (archivo == null || !archivo.isFile() || archivo.length() == 0) {
            throw new IOException("No existe una grabación para guardar en SQLite.");
        }
        db.delete("audio_fragmentos", "apunte_id = ?", new String[]{String.valueOf(id)});
        try (FileInputStream entrada = new FileInputStream(archivo)) {
            byte[] bloque = new byte[TAMANO_BLOQUE];
            int leidos;
            int parte = 0;
            while ((leidos = entrada.read(bloque)) != -1) {
                ContentValues valores = new ContentValues();
                valores.put("apunte_id", id);
                valores.put("parte", parte++);
                valores.put("datos", Arrays.copyOf(bloque, leidos));
                db.insertOrThrow("audio_fragmentos", null, valores);
            }
        }
    }

    static boolean existe(SQLiteDatabase db, int id) {
        try (Cursor cursor = db.query("audio_fragmentos", new String[]{"parte"},
                "apunte_id = ?", new String[]{String.valueOf(id)}, null, null, null, "1")) {
            return cursor.moveToFirst();
        }
    }

    static boolean recuperar(SQLiteDatabase db, int id, File destino) throws IOException {
        if (destino == null || !existe(db, id)) return false;
        File carpeta = destino.getParentFile();
        if (carpeta == null || (!carpeta.exists() && !carpeta.mkdirs())) {
            throw new IOException("No se pudo crear la carpeta privada de audio.");
        }
        File temporal = File.createTempFile("restaurar_", ".m4a", carpeta);
        boolean completado = false;
        try {
            try (FileOutputStream salida = new FileOutputStream(temporal);
                 Cursor cursor = db.query("audio_fragmentos", new String[]{"parte", "datos"},
                         "apunte_id = ?", new String[]{String.valueOf(id)}, null, null, "parte ASC")) {
                int parte = 0;
                while (cursor.moveToNext()) {
                    if (cursor.getInt(0) != parte++) throw new IOException("El audio está incompleto.");
                    salida.write(cursor.getBlob(1));
                }
            }
            if (temporal.length() == 0 || !temporal.renameTo(destino)) {
                throw new IOException("No se pudo recuperar la grabación de SQLite.");
            }
            completado = true;
            return true;
        } finally {
            if (!completado) temporal.delete();
        }
    }
}

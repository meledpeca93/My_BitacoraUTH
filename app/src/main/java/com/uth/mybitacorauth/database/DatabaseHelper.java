package com.uth.mybitacorauth.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String TABLA_APUNTES = "apuntes";
    private static final String NOMBRE_BASE = "bitacora_clases.db";
    private static final int VERSION_BASE = 2;

    public DatabaseHelper(Context context) {
        super(context.getApplicationContext(), NOMBRE_BASE, null, VERSION_BASE);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLA_APUNTES + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "asignatura TEXT NOT NULL, tema TEXT NOT NULL, "
                + "fecha TEXT NOT NULL, descripcion TEXT NOT NULL, audio TEXT NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Conserva los registros; sustituye el prefijo absoluto por audios/.
            db.execSQL("UPDATE apuntes SET audio = 'audios/' || "
                    + "substr(audio, instr(audio, '/audios/') + 8) WHERE audio LIKE ?",
                    new Object[]{"/%/audios/%"});
        }
    }
}

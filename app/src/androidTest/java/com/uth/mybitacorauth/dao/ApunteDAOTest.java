package com.uth.mybitacorauth.dao;

import android.content.Context;
import android.content.ContextWrapper;
import android.database.DatabaseErrorHandler;
import java.io.File;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.platform.app.InstrumentationRegistry;
import com.uth.mybitacorauth.database.DatabaseHelper;
import com.uth.mybitacorauth.model.Apunte;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/** Usa un archivo separado en caché para no tocar los apuntes del usuario. */
public class ApunteDAOTest {
    private Context contexto;
    private ApunteDAO dao;

    @Before public void preparar() {
        Context original = InstrumentationRegistry.getInstrumentation().getTargetContext();
        contexto = new ContextWrapper(original) {
            @Override public Context getApplicationContext() { return this; }
            @Override public File getDatabasePath(String nombre) {
                return new File(getCacheDir(), "pruebas_" + nombre);
            }
            @Override public SQLiteDatabase openOrCreateDatabase(String nombre, int mode,
                    SQLiteDatabase.CursorFactory factory) {
                return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(nombre), factory);
            }
            @Override public SQLiteDatabase openOrCreateDatabase(String nombre, int mode,
                    SQLiteDatabase.CursorFactory factory, DatabaseErrorHandler handler) {
                return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(nombre).getPath(), factory, handler);
            }
            @Override public boolean deleteDatabase(String nombre) {
                return SQLiteDatabase.deleteDatabase(getDatabasePath(nombre));
            }
        };
        contexto.deleteDatabase("bitacora_clases.db");
        dao = new ApunteDAO(contexto);
    }

    @After public void limpiar() {
        dao.cerrar();
        contexto.deleteDatabase("bitacora_clases.db");
    }

    @Test public void crudFiltroYPersistencia() {
        Apunte apunte = new Apunte(0, "Redes I", "Modelo OSI", "2026-10-17",
                "Descripción original", "audios/nota.m4a");
        long id = dao.insertar(apunte);
        assertTrue(id > 0);
        apunte.setId((int) id);
        assertEquals(1, dao.listarTodos().size());
        assertEquals(1, dao.filtrarPorAsignatura("Redes I").size());
        assertTrue(dao.filtrarPorAsignatura("Redes I' OR 1=1 --").isEmpty());
        apunte.setTema("Modelo actualizado");
        assertEquals(1, dao.actualizar(apunte));
        assertEquals(1, dao.actualizarDescripcion((int) id, "Texto dictado"));
        dao.cerrar();
        dao = new ApunteDAO(contexto);
        Apunte recuperado = dao.buscarPorId((int) id);
        assertNotNull(recuperado);
        assertEquals("Modelo actualizado", recuperado.getTema());
        assertEquals("Texto dictado", recuperado.getDescripcion());
        assertEquals("audios/nota.m4a", recuperado.getAudio());
        assertTrue(dao.audioEnUso("audios/nota.m4a"));
        assertEquals(1, dao.eliminar((int) id));
        assertFalse(dao.audioEnUso("audios/nota.m4a"));
        assertNull(dao.buscarPorId((int) id));
        assertTrue(dao.listarTodos().isEmpty());
    }

    @Test public void migracionConservaApunteYConvierteRuta() {
        try (SQLiteDatabase db = SQLiteDatabase.create(null)) {
            DatabaseHelper helper = new DatabaseHelper(contexto);
            helper.onCreate(db);
            db.execSQL("INSERT INTO apuntes (asignatura, tema, fecha, descripcion, audio) VALUES (?, ?, ?, ?, ?)",
                    new Object[]{"Redes I", "Tema", "2026-10-17", "Texto",
                            "/data/user/0/com.uth.mybitacorauth/files/audios/nota.m4a"});
            helper.onUpgrade(db, 1, 2);
            try (Cursor cursor = db.rawQuery("SELECT tema, audio FROM apuntes", null)) {
                assertTrue(cursor.moveToFirst());
                assertEquals("Tema", cursor.getString(0));
                assertEquals("audios/nota.m4a", cursor.getString(1));
            }
            helper.close();
        }
    }
}

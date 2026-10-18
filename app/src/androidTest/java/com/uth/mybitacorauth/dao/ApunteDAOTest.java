package com.uth.mybitacorauth.dao;

import android.content.Context;
import android.content.ContextWrapper;
import android.database.DatabaseErrorHandler;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.util.Arrays;
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
            @Override public File getFilesDir() {
                File carpeta = new File(getCacheDir(), "pruebas_archivos");
                carpeta.mkdirs();
                return carpeta;
            }
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
        File carpeta = new File(contexto.getFilesDir(), "audios");
        File[] archivos = carpeta.listFiles();
        if (archivos != null) for (File archivo : archivos) archivo.delete();
        carpeta.delete();
        contexto.getFilesDir().delete();
    }

    private File crearAudio(byte[] datos) throws Exception {
        File carpeta = new File(contexto.getFilesDir(), "audios");
        carpeta.mkdirs();
        File archivo = new File(carpeta, "nota.m4a");
        try (FileOutputStream salida = new FileOutputStream(archivo)) { salida.write(datos); }
        return archivo;
    }

    @Test public void crudFiltroYPersistencia() throws Exception {
        crearAudio(new byte[]{1, 2, 3});
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

    @Test public void blobGrandeSeRecuperaYSeEliminaEnCascada() throws Exception {
        byte[] datos = new byte[3 * 1024 * 1024 + 17];
        Arrays.fill(datos, (byte) 37);
        File archivo = crearAudio(datos);
        Apunte apunte = new Apunte(0, "Redes I", "Audio grande", "2026-10-18", "Texto", "audios/nota.m4a");
        apunte.setId((int) dao.insertar(apunte));
        assertTrue(archivo.delete());
        File recuperado = dao.prepararAudio(apunte);
        assertEquals(datos.length, recuperado.length());
        try (FileInputStream entrada = new FileInputStream(recuperado)) {
            byte[] copia = new byte[datos.length];
            int total = 0;
            int leidos;
            while (total < copia.length && (leidos = entrada.read(copia, total, copia.length - total)) > 0) {
                total += leidos;
            }
            assertEquals(datos.length, total);
            assertArrayEquals(datos, copia);
        }
        assertEquals(1, dao.eliminar(apunte.getId()));
        DatabaseHelper helper = new DatabaseHelper(contexto);
        try (Cursor cursor = helper.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM audio_fragmentos", null)) {
            assertTrue(cursor.moveToFirst());
            assertEquals(0, cursor.getInt(0));
        }
        helper.close();
    }

    @Test public void insercionSinArchivoHaceRollback() {
        try {
            dao.insertar(new Apunte(0, "Redes I", "Sin audio", "2026-10-18", "Texto", "audios/no_existe.m4a"));
            fail("Debe rechazar el audio inexistente.");
        } catch (android.database.sqlite.SQLiteException esperado) {
            assertTrue(dao.listarTodos().isEmpty());
        }
    }

    @Test public void conservaTextoYLoInvalidaAlReemplazarAudio() throws Exception {
        crearAudio(new byte[]{1, 2, 3});
        Apunte apunte = new Apunte(0, "Redes I", "Tema", "2026-10-18", "Descripción original", "audios/nota.m4a");
        apunte.setId((int) dao.insertar(apunte));
        assertTrue(dao.guardarTranscripcion(apunte.getId(), apunte.getAudio(), "Texto del audio"));
        assertEquals("Descripción original", dao.buscarPorId(apunte.getId()).getDescripcion());
        dao.cerrar();
        dao = new ApunteDAO(contexto);
        assertEquals("Texto del audio", dao.obtenerTranscripcion(apunte.getId()));
        apunte.setTema("Tema actualizado");
        assertEquals(1, dao.actualizar(apunte));
        assertEquals("Texto del audio", dao.obtenerTranscripcion(apunte.getId()));
        assertEquals(1, dao.actualizarDescripcion(apunte.getId(), "Texto corregido", true));
        assertEquals("Texto corregido", dao.obtenerTranscripcion(apunte.getId()));
        assertEquals("Texto corregido", dao.buscarPorId(apunte.getId()).getDescripcion());
        assertEquals(1, dao.actualizarDescripcion(apunte.getId(), "Dictado nuevo", false));
        assertEquals("Texto corregido", dao.obtenerTranscripcion(apunte.getId()));
        File otro = new File(contexto.getFilesDir(), "audios/otra.m4a");
        try (FileOutputStream salida = new FileOutputStream(otro)) { salida.write(new byte[]{4, 5}); }
        apunte.setAudio("audios/otra.m4a");
        assertEquals(1, dao.actualizar(apunte));
        assertNull(dao.obtenerTranscripcion(apunte.getId()));
        assertFalse(dao.guardarTranscripcion(apunte.getId(), "audios/nota.m4a", "Resultado antiguo"));
        assertTrue(dao.guardarTranscripcion(apunte.getId(), apunte.getAudio(), "Otro audio"));
        assertEquals(1, dao.eliminar(apunte.getId()));
        DatabaseHelper helper = new DatabaseHelper(contexto);
        try (Cursor cursor = helper.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM transcripciones", null)) {
            assertTrue(cursor.moveToFirst());
            assertEquals(0, cursor.getInt(0));
        }
        helper.close();
    }

    @Test public void migracionConservaApunteYConvierteRuta() {
        try (SQLiteDatabase db = SQLiteDatabase.create(null)) {
            DatabaseHelper helper = new DatabaseHelper(contexto);
            db.execSQL("CREATE TABLE apuntes (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "asignatura TEXT NOT NULL, tema TEXT NOT NULL, fecha TEXT NOT NULL, "
                    + "descripcion TEXT NOT NULL, audio TEXT NOT NULL)");
            db.execSQL("INSERT INTO apuntes (asignatura, tema, fecha, descripcion, audio) VALUES (?, ?, ?, ?, ?)",
                    new Object[]{"Redes I", "Tema", "2026-10-17", "Texto",
                            "/data/user/0/com.uth.mybitacorauth/files/audios/nota.m4a"});
            helper.onUpgrade(db, 1, 4);
            try (Cursor cursor = db.rawQuery("SELECT tema, audio FROM apuntes", null)) {
                assertTrue(cursor.moveToFirst());
                assertEquals("Tema", cursor.getString(0));
                assertEquals("audios/nota.m4a", cursor.getString(1));
            }
            try (Cursor tablas = db.rawQuery("SELECT name FROM sqlite_master WHERE name = ?",
                    new String[]{"audio_fragmentos"})) {
                assertTrue(tablas.moveToFirst());
            }
            helper.close();
        }
    }
}

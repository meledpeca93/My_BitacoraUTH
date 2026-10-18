package com.uth.mybitacorauth.util;

import org.junit.Test;
import java.io.File;
import java.nio.file.Files;
import static org.junit.Assert.*;

public class ArchivoAudioTest {
    @Test public void resuelveRutasNuevasYAnterioresSinSalirDeAudios() throws Exception {
        File privado = Files.createTempDirectory("bitacora-rutas").toFile();
        try {
            ArchivoAudio archivos = new ArchivoAudio(privado);
            File esperado = new File(privado, "audios/nota.m4a").getCanonicalFile();
            assertEquals(esperado, archivos.obtenerArchivo("audios/nota.m4a"));
            assertEquals(esperado, archivos.obtenerArchivo("/data/user/0/anterior/files/audios/nota.m4a"));
            assertEquals("audios/nota.m4a", archivos.rutaRelativa(esperado.getPath()));
            assertNull(archivos.obtenerArchivo("../nota.m4a"));
            assertNull(archivos.obtenerArchivo("audios/../../nota.m4a"));
            assertNull(archivos.obtenerArchivo("/etc/passwd"));
            assertNull(archivos.obtenerArchivo("audios"));
            assertNull(archivos.obtenerArchivo(""));
            assertNull(archivos.obtenerArchivo(null));
        } finally { privado.delete(); }
    }

    @Test public void eliminaSoloElArchivoPrivadoSolicitado() throws Exception {
        File privado = Files.createTempDirectory("bitacora-borrar").toFile();
        File carpeta = new File(privado, "audios");
        assertTrue(carpeta.mkdir());
        File audio = new File(carpeta, "nota.m4a");
        try {
            assertTrue(audio.createNewFile());
            ArchivoAudio archivos = new ArchivoAudio(privado);
            assertTrue(archivos.eliminar("audios/nota.m4a"));
            assertFalse(audio.exists());
            assertFalse(archivos.eliminar("../otro.m4a"));
        } finally { audio.delete(); carpeta.delete(); privado.delete(); }
    }
}

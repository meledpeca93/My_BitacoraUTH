package com.uth.mybitacorauth.controller;

import com.uth.mybitacorauth.model.Apunte;
import org.junit.Test;
import java.io.File;
import java.nio.file.Files;
import static org.junit.Assert.*;

public class ApunteControllerTest {
    @Test public void descripcionRequiereTexto() {
        assertNotNull(ApunteController.validarDescripcion(null));
        assertNotNull(ApunteController.validarDescripcion(""));
        assertNotNull(ApunteController.validarDescripcion("   "));
        assertNull(ApunteController.validarDescripcion("La Activity pasa a onPause cuando…"));
    }

    @Test public void fechasExigenFormatoYDiaReal() {
        assertTrue(ApunteController.esFechaValida("2024-02-29"));
        assertTrue(ApunteController.esFechaValida("2026-10-17"));
        for (String fecha : new String[]{"2026-02-31", "2026-02-29", "2026-13-01",
                "2026-00-10", "2026-01-00", "2026-1-01", "0000-01-01", "", "17/10/2026"}) {
            assertFalse(fecha, ApunteController.esFechaValida(fecha));
        }
        assertFalse(ApunteController.esFechaValida(null));
    }

    @Test public void validaCamposTemaYArchivoDeAudio() throws Exception {
        File audio = File.createTempFile("apunte", ".m4a");
        try {
            Files.write(audio.toPath(), new byte[]{1, 2, 3});
            Apunte apunte = new Apunte(0, "Programación Móvil I", "Lección de español 1",
                    "2026-10-17", "Descripción", audio.getAbsolutePath());
            // Esta prueba verifica existencia y tamaño; no verifica que el audio sea reproducible.
            assertNull(ApunteController.validar(apunte));
            apunte.setAsignatura(" ");
            assertEquals("Seleccione una asignatura.", ApunteController.validar(apunte));
            apunte.setAsignatura("Redes I");
            apunte.setTema("");
            assertEquals("Debe escribir un tema.", ApunteController.validar(apunte));
            apunte.setTema("Tema #1");
            assertNotNull(ApunteController.validar(apunte));
            apunte.setTema("Tema 1");
            apunte.setFecha("");
            assertEquals("Debe escribir una fecha.", ApunteController.validar(apunte));
            apunte.setFecha("2026-02-31");
            assertNotNull(ApunteController.validar(apunte));
            apunte.setFecha("2026-10-17");
            apunte.setDescripcion(" ");
            assertEquals("Debe escribir una descripción.", ApunteController.validar(apunte));
            apunte.setDescripcion("Texto");
            apunte.setAudio(null);
            assertEquals("Debe grabar una nota de voz.", ApunteController.validar(apunte));
            apunte.setAudio(audio.getAbsolutePath());
            Files.write(audio.toPath(), new byte[0]);
            assertNotNull(ApunteController.validar(apunte));
            assertTrue(audio.delete());
            assertNotNull(ApunteController.validar(apunte));
        } finally {
            audio.delete();
        }
    }
}

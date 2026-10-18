package com.uth.mybitacorauth.util;

import java.io.File;
import java.io.IOException;

/** Resuelve rutas relativas únicamente dentro de files/audios. */
public class ArchivoAudio {
    private final File directorioPrivado;

    public ArchivoAudio(File directorioPrivado) {
        this.directorioPrivado = directorioPrivado;
    }

    public File obtenerArchivo(String ruta) {
        if (ruta == null || ruta.trim().isEmpty()) return null;
        File archivo = new File(ruta);
        // Compatibilidad con rutas absolutas guardadas por la primera versión.
        if (archivo.isAbsolute()) {
            File padre = archivo.getParentFile();
            if (padre == null || !"audios".equals(padre.getName())) return null;
            archivo = new File(new File(directorioPrivado, "audios"), archivo.getName());
        } else archivo = new File(directorioPrivado, ruta);
        try {
            File carpeta = new File(directorioPrivado, "audios").getCanonicalFile();
            File canonico = archivo.getCanonicalFile();
            return carpeta.equals(canonico.getParentFile()) ? canonico : null;
        } catch (IOException e) {
            return null;
        }
    }

    public String rutaRelativa(String ruta) {
        File archivo = obtenerArchivo(ruta);
        if (archivo == null) throw new IllegalArgumentException("La ruta del audio no es válida.");
        return "audios/" + archivo.getName();
    }

    public boolean eliminar(String ruta) {
        File archivo = obtenerArchivo(ruta);
        return archivo != null && (!archivo.exists() || archivo.delete());
    }
}

package com.uth.mybitacorauth.audio;

import android.content.Context;
import android.content.res.AssetManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/** Copia el modelo incluido en el APK al directorio privado que necesita la biblioteca nativa. */
public class ModeloVoz {
    public static synchronized File preparar(Context context) throws IOException {
        DecodificadorAudio.comprobarCancelacion();
        File modelo = new File(context.getFilesDir(), "modelos/model-es-0.42");
        if (new File(modelo, "listo").isFile() && new File(modelo, "am/final.mdl").isFile()) return modelo;
        AssetManager assets = context.getAssets();
        try (InputStream archivo = assets.open("model-es/am/final.mdl")) {
            // Verifica que la preparación del modelo se realizó antes de compilar el APK.
        } catch (IOException e) {
            throw new IOException("Falta el modelo español. Prepare el modelo y reinstale la aplicación.");
        }
        File marca = new File(modelo, "listo");
        if (marca.exists() && !marca.delete()) throw new IOException("No se pudo preparar el modelo español.");
        copiar(assets, "model-es", modelo);
        if (!new File(modelo, "listo").createNewFile()) {
            throw new IOException("No se pudo completar la instalación del modelo español.");
        }
        return modelo;
    }

    private static void copiar(AssetManager assets, String origen, File destino) throws IOException {
        DecodificadorAudio.comprobarCancelacion();
        String[] hijos = assets.list(origen);
        if (hijos != null && hijos.length > 0) {
            if (!destino.exists() && !destino.mkdirs()) throw new IOException("No hay espacio para el modelo español.");
            for (String hijo : hijos) copiar(assets, origen + "/" + hijo, new File(destino, hijo));
        } else {
            try (InputStream entrada = assets.open(origen);
                 FileOutputStream salida = new FileOutputStream(destino)) {
                byte[] buffer = new byte[8192];
                int leidos;
                while ((leidos = entrada.read(buffer)) != -1) {
                    DecodificadorAudio.comprobarCancelacion();
                    salida.write(buffer, 0, leidos);
                }
            }
        }
    }
}

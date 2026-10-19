package com.uth.mybitacorauth.audio;

import android.content.Context;
import org.json.JSONException;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/** Reconoce el archivo guardado sin abrir el micrófono ni enviar audio a Internet. */
public class TranscriptorAudio {
    public interface Progreso {
        void actualizar(int porcentaje);
    }

    public String transcribir(Context context, File audio) throws IOException {
        return transcribir(context, audio, porcentaje -> {});
    }

    public String transcribir(Context context, File audio, Progreso progreso) throws IOException {
        File modelo = ModeloVoz.preparar(context);
        File pcm = File.createTempFile("transcribir_", ".pcm", context.getCacheDir());
        try {
            int frecuencia = new DecodificadorAudio().decodificar(audio, pcm);
            progreso.actualizar(0);
            StringBuilder texto = new StringBuilder();
            try (Model vosk = new Model(modelo.getAbsolutePath());
                 Recognizer reconocedor = new Recognizer(vosk, frecuencia);
                 FileInputStream entrada = new FileInputStream(pcm)) {
                byte[] buffer = new byte[4096];
                int leidos;
                long procesados = 0;
                long total = pcm.length();
                int ultimoPorcentaje = 0;
                while ((leidos = entrada.read(buffer)) != -1) {
                    DecodificadorAudio.comprobarCancelacion();
                    if (reconocedor.acceptWaveForm(buffer, leidos)) agregarTexto(texto, reconocedor.getResult());
                    procesados += leidos;
                    int porcentaje = (int) Math.min(99, procesados * 100 / total);
                    if (porcentaje != ultimoPorcentaje) {
                        progreso.actualizar(porcentaje);
                        ultimoPorcentaje = porcentaje;
                    }
                }
                DecodificadorAudio.comprobarCancelacion();
                agregarTexto(texto, reconocedor.getFinalResult());
            }
            return texto.toString().trim();
        } catch (JSONException e) {
            throw new IOException("No se pudo interpretar el resultado de la transcripción.");
        } finally { pcm.delete(); }
    }

    private void agregarTexto(StringBuilder texto, String resultado) throws JSONException {
        String fragmento = new JSONObject(resultado).optString("text", "").trim();
        if (!fragmento.isEmpty()) {
            if (texto.length() > 0) texto.append(' ');
            texto.append(fragmento);
        }
    }
}

package com.uth.mybitacorauth.audio;

import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.SystemClock;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;

/** Convierte el M4A comprimido a PCM mono que Vosk puede reconocer. Se ejecuta en segundo plano. */
public class DecodificadorAudio {
    public int decodificar(File origen, File destino) throws IOException {
        MediaExtractor extractor = new MediaExtractor();
        MediaCodec codec = null;
        boolean iniciado = false;
        try {
            extractor.setDataSource(origen.getAbsolutePath());
            MediaFormat formato = seleccionarAudio(extractor);
            codec = MediaCodec.createDecoderByType(formato.getString(MediaFormat.KEY_MIME));
            formato.setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT);
            codec.configure(formato, null, null, 0);
            codec.start();
            iniciado = true;
            return extraerPcm(extractor, codec, formato, destino);
        } finally {
            try {
                if (codec != null) {
                    try { if (iniciado) codec.stop(); }
                    finally { codec.release(); }
                }
            } finally { extractor.release(); }
        }
    }

    private MediaFormat seleccionarAudio(MediaExtractor extractor) throws IOException {
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat formato = extractor.getTrackFormat(i);
            String tipo = formato.getString(MediaFormat.KEY_MIME);
            if (tipo != null && tipo.startsWith("audio/")) {
                extractor.selectTrack(i);
                return formato;
            }
        }
        throw new IOException("La grabación no contiene una pista de audio válida.");
    }

    private int extraerPcm(MediaExtractor extractor, MediaCodec codec,
                           MediaFormat formato, File destino) throws IOException {
        int frecuencia = formato.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        int canales = formato.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        boolean entradaTerminada = false;
        boolean salidaTerminada = false;
        long ultimoAvance = SystemClock.elapsedRealtime();
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        try (FileOutputStream salida = new FileOutputStream(destino)) {
            while (!salidaTerminada) {
                comprobarCancelacion();
                if (!entradaTerminada) {
                    int indice = codec.dequeueInputBuffer(10000);
                    if (indice >= 0) {
                        ByteBuffer buffer = codec.getInputBuffer(indice);
                        if (buffer == null) throw new IOException("No se pudo leer la grabación.");
                        buffer.clear();
                        int cantidad = extractor.readSampleData(buffer, 0);
                        if (cantidad < 0) {
                            codec.queueInputBuffer(indice, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            entradaTerminada = true;
                        } else {
                            codec.queueInputBuffer(indice, 0, cantidad, extractor.getSampleTime(), 0);
                            extractor.advance();
                        }
                        ultimoAvance = SystemClock.elapsedRealtime();
                    }
                }
                int indice = codec.dequeueOutputBuffer(info, 10000);
                if (indice == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat nuevo = codec.getOutputFormat();
                    frecuencia = nuevo.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                    canales = nuevo.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                    if (nuevo.containsKey(MediaFormat.KEY_PCM_ENCODING)
                            && nuevo.getInteger(MediaFormat.KEY_PCM_ENCODING) != AudioFormat.ENCODING_PCM_16BIT) {
                        throw new IOException("Este teléfono no pudo convertir la grabación a un formato compatible.");
                    }
                } else if (indice >= 0) {
                    try {
                        ByteBuffer buffer = codec.getOutputBuffer(indice);
                        if (info.size > 0 && (info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                            if (buffer == null) throw new IOException("No se pudo convertir la grabación.");
                            buffer.position(info.offset);
                            buffer.limit(info.offset + info.size);
                            byte[] datos = new byte[info.size];
                            buffer.get(datos);
                            salida.write(AudioPcm.aMono(datos, canales));
                        }
                        salidaTerminada = (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                    } finally { codec.releaseOutputBuffer(indice, false); }
                    ultimoAvance = SystemClock.elapsedRealtime();
                }
                if (SystemClock.elapsedRealtime() - ultimoAvance > 15000) {
                    throw new IOException("La conversión de la grabación no pudo completarse.");
                }
            }
        }
        if (destino.length() == 0 || frecuencia <= 0) throw new IOException("La grabación está vacía.");
        return frecuencia;
    }

    public static void comprobarCancelacion() throws InterruptedIOException {
        if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Transcripción cancelada.");
    }
}

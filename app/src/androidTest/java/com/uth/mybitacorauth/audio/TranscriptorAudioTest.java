package com.uth.mybitacorauth.audio;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.nio.ByteBuffer;
import org.junit.Test;
import static org.junit.Assert.*;

/** Prueba el contenedor real M4A/AAC y el modelo nativo, sin necesitar un micrófono. */
public class TranscriptorAudioTest {
    @Test public void convierteAacSilenciosoYNoInventaUnaDescripcion() throws Exception {
        Context contexto = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File audio = File.createTempFile("prueba_silencio_", ".m4a", contexto.getCacheDir());
        File pcm = File.createTempFile("prueba_pcm_", ".pcm", contexto.getCacheDir());
        try {
            crearAacSilencioso(audio);
            assertTrue(audio.length() > 0);
            assertEquals(16000, new DecodificadorAudio().decodificar(audio, pcm));
            assertTrue(pcm.length() > 0);
            assertEquals("", new TranscriptorAudio().transcribir(contexto, audio));
        } finally { audio.delete(); pcm.delete(); }
    }

    private void crearAacSilencioso(File archivo) throws Exception {
        MediaFormat formato = MediaFormat.createAudioFormat("audio/mp4a-latm", 16000, 1);
        formato.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        formato.setInteger(MediaFormat.KEY_BIT_RATE, 64000);
        MediaCodec encoder = MediaCodec.createEncoderByType("audio/mp4a-latm");
        MediaMuxer muxer = new MediaMuxer(archivo.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        boolean encoderIniciado = false;
        boolean muxerIniciado = false;
        try {
            encoder.configure(formato, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            encoder.start();
            encoderIniciado = true;
            int enviados = 0;
            int pista = -1;
            boolean entradaTerminada = false;
            boolean salidaTerminada = false;
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            long limite = android.os.SystemClock.elapsedRealtime() + 15000;
            while (!salidaTerminada) {
                if (android.os.SystemClock.elapsedRealtime() > limite) fail("El codificador no terminó.");
                if (!entradaTerminada) {
                    int indice = encoder.dequeueInputBuffer(10000);
                    if (indice >= 0) {
                        ByteBuffer buffer = encoder.getInputBuffer(indice);
                        assertNotNull(buffer);
                        buffer.clear();
                        int cantidad = Math.min(2048, 64000 - enviados);
                        for (int i = 0; i < cantidad; i++) buffer.put((byte) 0);
                        int flags = cantidad == 0 ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0;
                        encoder.queueInputBuffer(indice, 0, cantidad, enviados * 1000000L / 32000, flags);
                        enviados += cantidad;
                        entradaTerminada = cantidad == 0;
                    }
                }
                int indice = encoder.dequeueOutputBuffer(info, 10000);
                if (indice == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    pista = muxer.addTrack(encoder.getOutputFormat());
                    muxer.start();
                    muxerIniciado = true;
                } else if (indice >= 0) {
                    try {
                        ByteBuffer buffer = encoder.getOutputBuffer(indice);
                        if (info.size > 0 && (info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                            assertTrue(muxerIniciado);
                            assertNotNull(buffer);
                            buffer.position(info.offset);
                            buffer.limit(info.offset + info.size);
                            muxer.writeSampleData(pista, buffer, info);
                        }
                        salidaTerminada = (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                    } finally { encoder.releaseOutputBuffer(indice, false); }
                }
            }
        } finally {
            try { if (encoderIniciado) encoder.stop(); }
            finally {
                encoder.release();
                try { if (muxerIniciado) muxer.stop(); }
                finally { muxer.release(); }
            }
        }
    }
}

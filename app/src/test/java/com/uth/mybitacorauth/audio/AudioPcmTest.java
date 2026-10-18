package com.uth.mybitacorauth.audio;

import org.junit.Test;
import static org.junit.Assert.*;

public class AudioPcmTest {
    @Test public void conservaMuestrasMonoIncluidosNegativos() {
        byte[] datos = {0, 0, (byte) 255, 127, 0, (byte) 128, (byte) 255, (byte) 255};
        assertArrayEquals(datos, AudioPcm.aMono(datos, 1));
    }

    @Test public void mezclaCanalesEstereoSinDesbordamiento() {
        // Pares: (32767,32767), (-32768,-32768), (1000,-1000).
        byte[] estereo = {(byte)255,127,(byte)255,127,0,(byte)128,0,(byte)128,
                (byte)232,3,24,(byte)252};
        byte[] esperado = {(byte)255,127,0,(byte)128,0,0};
        assertArrayEquals(esperado, AudioPcm.aMono(estereo, 2));
    }

    @Test public void rechazaCanalesYTramasIncompletas() {
        for (int canales : new int[]{0, -1, 9}) {
            try { AudioPcm.aMono(new byte[4], canales); fail("Debe rechazar canales inválidos."); }
            catch (IllegalArgumentException esperado) { }
        }
        try { AudioPcm.aMono(new byte[3], 2); fail("Debe rechazar una trama incompleta."); }
        catch (IllegalArgumentException esperado) { }
    }
}

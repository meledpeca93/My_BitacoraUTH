package com.uth.mybitacorauth.audio;

/** Convierte muestras PCM de 16 bits little-endian a un canal, sin cambiar la frecuencia. */
public class AudioPcm {
    public static byte[] aMono(byte[] datos, int canales) {
        if (canales < 1 || canales > 8 || datos.length % (canales * 2) != 0) {
            throw new IllegalArgumentException("El formato de muestras de audio no es válido.");
        }
        byte[] mono = new byte[datos.length / canales];
        int salida = 0;
        for (int inicio = 0; inicio < datos.length; inicio += canales * 2) {
            int suma = 0;
            for (int canal = 0; canal < canales; canal++) {
                int posicion = inicio + canal * 2;
                suma += (short) ((datos[posicion] & 255) | ((datos[posicion + 1] & 255) << 8));
            }
            int muestra = suma / canales;
            mono[salida++] = (byte) muestra;
            mono[salida++] = (byte) (muestra >> 8);
        }
        return mono;
    }
}

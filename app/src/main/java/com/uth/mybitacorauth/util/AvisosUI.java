package com.uth.mybitacorauth.util;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;

import com.google.android.material.snackbar.Snackbar;
import com.uth.mybitacorauth.R;

/** Avisos accesibles dentro de la app con icono y movimiento de Material. */
public final class AvisosUI {
    private AvisosUI() {}

    public static void mostrar(Activity activity, String titulo, String mensaje) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        Snackbar aviso = Snackbar.make(activity.findViewById(android.R.id.content),
                titulo + ": " + mensaje, Snackbar.LENGTH_LONG);
        aviso.setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE);
        aviso.setBackgroundTint(activity.getColor(R.color.primary));
        aviso.setTextColor(Color.WHITE);
        aviso.setActionTextColor(Color.WHITE);
        aviso.setAction("Cerrar", view -> aviso.dismiss());
        TextView texto = aviso.getView().findViewById(com.google.android.material.R.id.snackbar_text);
        texto.setMaxLines(Integer.MAX_VALUE);
        Drawable icono = AppCompatResources.getDrawable(activity, R.drawable.ic_aviso);
        if (icono != null) {
            icono = DrawableCompat.wrap(icono.mutate());
            DrawableCompat.setTint(icono, Color.WHITE);
            texto.setCompoundDrawablesRelativeWithIntrinsicBounds(icono, null, null, null);
            texto.setCompoundDrawablePadding(Math.round(12 * activity.getResources().getDisplayMetrics().density));
        }
        aviso.show();
    }
}

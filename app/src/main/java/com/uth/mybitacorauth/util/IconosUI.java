package com.uth.mybitacorauth.util;

import android.os.Build;
import android.widget.Button;
import com.google.android.material.button.MaterialButton;

/** Cambia el icono y mantiene una descripción accesible del estado actual. */
public class IconosUI {
    public static void actualizar(Button boton, int icono, String descripcion) {
        ((MaterialButton) boton).setIconResource(icono);
        boton.setText("");
        boton.setContentDescription(descripcion);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) boton.setTooltipText(descripcion);
    }
}

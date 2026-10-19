package com.uth.mybitacorauth;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Pantalla de bienvenida y acceso a las funciones principales. */
public class InicioActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_inicio);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });
        findViewById(R.id.btnNuevoApunte).setOnClickListener(v ->
                startActivity(new Intent(this, MainActivity.class)));
        findViewById(R.id.btnMisApuntes).setOnClickListener(v ->
                startActivity(new Intent(this, ApuntesActivity.class)));
        findViewById(R.id.btnAcercaDe).setOnClickListener(v ->
                new MaterialAlertDialogBuilder(this)
                        .setIcon(R.drawable.ic_book)
                        .setTitle("Acerca de Mi Bitácora")
                        .setMessage("CREADOR DEL PROYECTO\nMelvin Edgardo Peña Caballero\n\n"
                                + "CLASE\nProgramación Móvil 1\n\n"
                                + "DOCENTE\nIngeniero Enrique Lagos\n\n"
                                + "HORARIO\nSábado de 3:30 p. m. a 6:30 p. m.\n\n"
                                + "Universidad Tecnológica de Honduras")
                        .setPositiveButton("Cerrar", null)
                        .show());
    }
}

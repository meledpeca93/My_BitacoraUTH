package com.uth.mybitacorauth.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.uth.mybitacorauth.R;
import com.uth.mybitacorauth.model.Apunte;
import java.util.List;

/** Fila del ListView: fecha, tema y un botón para ver el texto de ese audio. */
public class ApunteAdapter extends ArrayAdapter<Apunte> {
    public interface AlVerTexto { void abrir(Apunte apunte); }
    private final AlVerTexto accion;
    private int seleccionadoId;

    public ApunteAdapter(Context contexto, List<Apunte> apuntes, int seleccionadoId, AlVerTexto accion) {
        super(contexto, R.layout.item_apunte, apuntes);
        this.seleccionadoId = seleccionadoId;
        this.accion = accion;
    }

    public void seleccionar(int id) {
        seleccionadoId = id;
        notifyDataSetChanged();
    }

    @Override public View getView(int posicion, View fila, ViewGroup padre) {
        if (fila == null) fila = LayoutInflater.from(getContext()).inflate(R.layout.item_apunte, padre, false);
        Apunte apunte = getItem(posicion);
        ((TextView) fila.findViewById(R.id.txtFechaFila)).setText(apunte.getFecha());
        ((TextView) fila.findViewById(R.id.txtTemaFila)).setText(apunte.getTema());
        fila.setActivated(apunte.getId() == seleccionadoId);
        View boton = fila.findViewById(R.id.btnTextoFila);
        boton.setContentDescription("Ver transcripción de " + apunte.getTema());
        boton.setOnClickListener(v -> accion.abrir(apunte));
        return fila;
    }
}

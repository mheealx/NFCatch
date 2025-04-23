package com.esime.nfcdroid2.ui.informacion;

import android.content.Context;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.esime.nfcdroid2.R;
import com.esime.nfcdroid2.ui.informacion.componentes.DeviceNames;
import com.esime.nfcdroid2.ui.informacion.componentes.InfoAdapter;
import com.esime.nfcdroid2.ui.informacion.componentes.NfcChipIdentifier;

import java.util.ArrayList;
import java.util.List;

public class InfoFragment extends Fragment {

    private ListView statusListView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_informacion, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        statusListView = view.findViewById(R.id.status_list);
        ProgressBar progressBar = view.findViewById(R.id.loading_indicator);
        Context context = requireContext();

        // Mostrar el ProgressBar
        progressBar.setVisibility(View.VISIBLE);
        statusListView.setVisibility(View.GONE);

        new Thread(() -> {
            List<String> titles = new ArrayList<>();
            List<String> values = new ArrayList<>();

            // 1. Nombre comercial
            String deviceName = new DeviceNames(context).formatCurrentDeviceName();
            titles.add("Nombre del dispositivo");
            values.add(deviceName);

            // 2. Versión de Android
            titles.add("Versión del sistema operativo");
            values.add(Build.VERSION.RELEASE);

            // 3. Número de compilación
            titles.add("Número de compilación");
            values.add(Build.DISPLAY);

            // 4. NFC disponible
            NfcAdapter adapter = NfcAdapter.getDefaultAdapter(context);
            boolean hasNfc = adapter != null;
            titles.add("¿Cuenta con NFC?");
            values.add(hasNfc ? "Sí" : "No");

            // 5. Modelo del chip
            String chipName = hasNfc ? NfcChipIdentifier.detect() : "NFC no disponible";
            titles.add("Modelo del chip NFC");
            values.add(chipName != null && !chipName.isEmpty() ? chipName : "Desconocido");


            requireActivity().runOnUiThread(() -> {
                // Ocultar ProgressBar y mostrar resultados
                progressBar.setVisibility(View.GONE);
                statusListView.setVisibility(View.VISIBLE);

                InfoAdapter adapterList = new InfoAdapter(context, titles, values);
                statusListView.setAdapter(adapterList);
            });
        }).start();
    }

}

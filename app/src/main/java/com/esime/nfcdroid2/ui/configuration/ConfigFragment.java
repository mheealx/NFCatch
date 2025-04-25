package com.esime.nfcdroid2.ui.configuration;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.esime.nfcdroid2.databinding.FragmentConfigBinding;

public class ConfigFragment extends Fragment {

    private FragmentConfigBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // Inflar el layout
        binding = FragmentConfigBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Obtener el Switch por su ID
        Switch inicioAutoSwitch = binding.inicioautoSwitch;

        // Cargar el estado guardado en SharedPreferences
        SharedPreferences preferences = getActivity().getSharedPreferences("config_preferences", Context.MODE_PRIVATE);
        boolean isAutoStartEnabled = preferences.getBoolean("auto_start_service", true);
        inicioAutoSwitch.setChecked(isAutoStartEnabled);

        // Configurar listener para el cambio de estado del Switch
        inicioAutoSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Guardar el estado del Switch en SharedPreferences
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("auto_start_service", isChecked);
            editor.apply();

            // Mostrar un mensaje de confirmación al usuario
            if (isChecked) {
                Toast.makeText(getContext(), "Inicio automático activado", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Inicio automático desactivado", Toast.LENGTH_SHORT).show();
            }
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

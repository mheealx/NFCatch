package com.esime.nfcdroid2.ui.historial;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.esime.nfcdroid2.databinding.FragmentGalleryBinding;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

public class HistorialFragment extends Fragment {

    private FragmentGalleryBinding binding;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yy", Locale.getDefault());

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentGalleryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        binding.logRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        cargarArchivosGuardados();

        // Agregamos el botón de filtrado por fecha
        binding.filterDateButton.setOnClickListener(v -> mostrarCalendario());

        binding.clearFilterButton.setOnClickListener(v -> {
            cargarArchivosGuardados();
            Toast.makeText(requireContext(), "Filtros eliminados", Toast.LENGTH_SHORT).show();
        });


        return root;
    }

    private void mostrarCalendario() {
        final Calendar calendario = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (DatePicker view, int year, int month, int dayOfMonth) -> {
                    Calendar fechaSeleccionada = Calendar.getInstance();
                    fechaSeleccionada.set(year, month, dayOfMonth);
                    filtrarArchivosPorFecha(dateFormat.format(fechaSeleccionada.getTime()));
                },
                calendario.get(Calendar.YEAR),
                calendario.get(Calendar.MONTH),
                calendario.get(Calendar.DAY_OF_MONTH)
        );

        // Agregamos botón para "Hoy"
        datePickerDialog.setButton(DatePickerDialog.BUTTON_NEUTRAL, "Hoy", (dialog, which) -> {
            if (which == DatePickerDialog.BUTTON_NEUTRAL) {
                String fechaHoy = dateFormat.format(new Date());
                filtrarArchivosPorFecha(fechaHoy);
            }
        });

        datePickerDialog.show();
    }

    private void filtrarArchivosPorFecha(String fechaDeseada_ddMMyy) {
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "NFCDroid");

        // Convertir la fecha recibida (formato dd-MM-yy) a yyyyMMdd
        try {
            SimpleDateFormat formatoEntrada = new SimpleDateFormat("dd-MM-yy", Locale.getDefault());
            SimpleDateFormat formatoArchivo = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());

            Date fecha = formatoEntrada.parse(fechaDeseada_ddMMyy);
            String fechaFormateada = formatoArchivo.format(fecha); // → "20250420"

            File[] archivos = dir.listFiles((d, name) ->
                    name.startsWith("log_nfc_" + fechaFormateada) && name.endsWith(".txt"));

            if (archivos != null && archivos.length > 0) {
                List<File> lista = Arrays.asList(archivos);
                lista.sort(Comparator.comparingLong(File::lastModified));
                binding.logRecyclerView.setAdapter(new LogFileAdapter(requireContext(), lista));
                binding.emptyView.setVisibility(View.GONE);
            } else {
                binding.logRecyclerView.setAdapter(null);
                binding.emptyView.setVisibility(View.VISIBLE);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Error al filtrar fecha", Toast.LENGTH_SHORT).show();
        }
    }



    private void cargarArchivosGuardados() {
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "NFCDroid");

        if (dir.exists() && dir.isDirectory()) {
            File[] archivos = dir.listFiles((d, name) -> name.endsWith(".txt"));
            if (archivos != null && archivos.length > 0) {
                List<File> lista = Arrays.asList(archivos);
                lista.sort(Comparator.comparingLong(File::lastModified));
                binding.logRecyclerView.setAdapter(new LogFileAdapter(requireContext(), lista));
                binding.emptyView.setVisibility(View.GONE);
            } else {
                binding.logRecyclerView.setAdapter(null);
                binding.emptyView.setVisibility(View.VISIBLE);
            }
        } else {
            binding.logRecyclerView.setAdapter(null);
            binding.emptyView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

}

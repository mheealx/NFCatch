package com.esime.nfcdroid2.ui.home;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.*;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.esime.nfcdroid2.R;
import com.esime.nfcdroid2.services.ServicioSegundoPlano;
import com.esime.nfcdroid2.utils.*;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class HomeFragment extends Fragment implements LogRegistry.LogUpdateListener {

    private static final String TAG = "NFC_LOG";

    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;

    private TextView consoleTextView;
    private ScrollView consoleScrollView;
    private SearchView searchView;
    private Button filterButton, saveButton, clearConsoleButton;

    private final StringBuilder fullLog = new StringBuilder();
    private final List<String> allLogs = new ArrayList<>();
    private final Set<String> selectedTechFilters = new HashSet<>();
    private String currentQuery = "";

    private final String[] techOptions = {
            "NfcA","NfcB", "NfcV","NfcF","IsoDep","Ndef","NdefFormatable","MifareClassic", "MifareUltralight"
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        inicializarComponentes(root);

        nfcAdapter = NfcAdapter.getDefaultAdapter(requireContext());
        if (nfcAdapter == null) {
            manejarDispositivoSinNfc(root);
            return root;
        }

        configurarListeners();

        Intent intent = new Intent(requireActivity(), requireActivity().getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        pendingIntent = PendingIntent.getActivity(requireContext(), 0, intent, PendingIntent.FLAG_MUTABLE);

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (nfcAdapter != null) {
            if (!nfcAdapter.isEnabled()) {
                mostrarDialogoNfcDesactivado();
            } else {
                nfcAdapter.enableForegroundDispatch(requireActivity(), pendingIntent, null, null);
            }
        }

        LogRegistry.setListener(this);
        for (String linea : LogRegistry.getLogEvents()) {
            allLogs.add(linea);
            fullLog.append(linea).append("\n");
        }
        aplicarFiltros();

        if (getActivity() != null && getActivity().getIntent() != null) {
            handleNfcIntent(getActivity().getIntent());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (nfcAdapter != null) nfcAdapter.disableForegroundDispatch(requireActivity());
        LogRegistry.removeListener();
    }

    // Inicializa componentes de la interfaz
    private void inicializarComponentes(View root) {
        consoleTextView = root.findViewById(R.id.consoleTextView);
        consoleScrollView = root.findViewById(R.id.consoleScrollView);
        searchView = root.findViewById(R.id.searchView);
        filterButton = root.findViewById(R.id.filterButton);
        saveButton = root.findViewById(R.id.saveButton);
        clearConsoleButton = root.findViewById(R.id.clearConsoleButton);
    }

    // Configura los listeners de botones y búsqueda
    private void configurarListeners() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { currentQuery = query; aplicarFiltros(); return true; }
            @Override public boolean onQueryTextChange(String newText) { currentQuery = newText; aplicarFiltros(); return true; }
        });

        filterButton.setOnClickListener(v -> mostrarDialogoFiltros());
        saveButton.setOnClickListener(v -> guardarLog());
        clearConsoleButton.setOnClickListener(v -> limpiarConsola());
    }

    // Muestra si el dispositivo no tiene NFC y cierra la app después de 5 segundos
    private void manejarDispositivoSinNfc(View root) {
        disableUi();

        root.post(() -> consoleTextView.setText("Este dispositivo no es compatible con NFC."));
        Toast.makeText(requireContext(), "La aplicación se cerrará automáticamente en 5 segundos.", Toast.LENGTH_LONG).show();

        new android.os.Handler().postDelayed(() -> {
            Intent stopIntent = new Intent(requireContext(), ServicioSegundoPlano.class);
            requireContext().stopService(stopIntent);
            requireActivity().finishAffinity();
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
        }, 5000);
    }

    // Desactiva los botones de la UI si el dispositivo no tiene NFC
    private void disableUi() {
        searchView.setEnabled(false);
        filterButton.setEnabled(false);
        saveButton.setEnabled(false);
        clearConsoleButton.setEnabled(false);
    }

    // Limpia la consola de logs
    private void limpiarConsola() {
        fullLog.setLength(0);
        allLogs.clear();
        currentQuery = "";
        selectedTechFilters.clear();
        consoleTextView.setText("NFC:/\n");
        LogRegistry.clear();
        Toast.makeText(requireContext(), "Consola reiniciada", Toast.LENGTH_SHORT).show();
    }

    // Muestra un diálogo si el NFC está apagado
    private void mostrarDialogoNfcDesactivado() {
        new AlertDialog.Builder(requireContext())
                .setTitle("NFC DESACTIVADO")
                .setMessage("El dispositivo tiene el chip NFC APAGADO.\nPor favor, enciéndelo para continuar.")
                .setCancelable(false)
                .setPositiveButton("Ir a configuración", (dialog, which) -> {
                    Intent intent = new Intent(android.provider.Settings.ACTION_NFC_SETTINGS);
                    startActivity(intent);
                }).show();
    }

    @Override
    public void onLogUpdated(String newLog) {
        allLogs.add(newLog);
        fullLog.append(newLog).append("\n");

        requireActivity().runOnUiThread(() -> {
            if (currentQuery.isEmpty() && selectedTechFilters.isEmpty()) {
                String current = consoleTextView.getText().toString();
                consoleTextView.setText(current + newLog + "\n");
                consoleScrollView.post(() -> consoleScrollView.fullScroll(View.FOCUS_DOWN));
            } else {
                aplicarFiltros();
            }
        });
    }

    // Maneja la detección de la etiqueta NFC
    public void handleNfcIntent(Intent intent) {
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag != null) {
            ServicioSegundoPlano.marcarLecturaNfc();

            // Llamada al nuevo método identificarDispositivo
            appendLog("D", TAG, "--------------------------------------------------------------------------------------\n");
            appendLog("D", TAG, "Tag detectado");
            identificarDispositivo(tag);
            appendLog("D", TAG, "--------------------------------------------------------------------------------------\n");
            appendLog("D", TAG, "UID: " + bytesToHex(tag.getId()));
            appendLog("D", TAG, "Techs: " + Arrays.toString(tag.getTechList()));


            LogCallback logger = this::appendLog;

            for (String tech : tag.getTechList()) {
                switch (tech) {
                    case "android.nfc.tech.Ndef": NfcNdefHelper.read(tag, logger); break;
                    case "android.nfc.tech.NdefFormatable": NfcNdefFormatableHelper.read(tag, logger); break;
                    case "android.nfc.tech.MifareClassic": NfcMifareClassicHelper.read(tag, logger); break;
                    case "android.nfc.tech.MifareUltralight": NfcMifareUltralightHelper.read(tag, logger); break;
                    case "android.nfc.tech.NfcA": NfcAHelper.read(tag, logger); break;
                    case "android.nfc.tech.NfcB": NfcBHelper.read(tag, logger); break;
                    case "android.nfc.tech.NfcF": NfcFHelper.read(tag, logger); break;
                    case "android.nfc.tech.NfcV": NfcVHelper.read(tag, logger); break;
                    case "android.nfc.tech.IsoDep": NfcIsoDepHelper.read(tag, logger); break;
                }
            }
        }
        requireActivity().setIntent(new Intent());

    }


    private void identificarDispositivo(Tag tag) {
        // Obtiene las tecnologías detectadas en el tag
        String[] techs = tag.getTechList();
        String uid = bytesToHex(tag.getId());

        // Identificación de dispositivos móviles (Pueden ser solo el dispositivo, Google Pay o Apple Pay) (especialmente si el UID empieza con "08")
        if (uid.startsWith("08")) {
            appendLog("I", TAG, "Dispositivo: Teléfono móvil\n");
        }


        // Identificación de llave de seguridad de marca Yubico
        else if (Arrays.asList(techs).contains("android.nfc.tech.IsoDep") && Arrays.asList(techs).contains("android.nfc.tech.NfcA") && Arrays.asList(techs).contains("android.nfc.tech.Ndef")) {
            appendLog("I", TAG, "Dispositivo: Yubikey\n");
        }

        // Identificación de dispositivos de audio
        else if (Arrays.asList(techs).contains("android.nfc.tech.NfcV") || Arrays.asList(techs).contains("android.nfc.tech.NfcF")) {
            appendLog("I", TAG, "Dispositivo: Bocina o dispositivo de audio\n");
        }

        // Identificación de tags NFC con soporte NDEF
        else if (Arrays.asList(techs).contains("android.nfc.tech.Ndef")) {
            appendLog("I", TAG, "Dispositivo: Tag NFC con NDEF\n");
        }

        // Identificación de tarjetas de transporte público
        else if (Arrays.asList(techs).contains("android.nfc.tech.IsoDep") && Arrays.asList(techs).contains("android.nfc.tech.NfcB")) {
            appendLog("I", TAG, "Dispositivo: Tarjeta de Metro\n");
        }

        // Identificación de dispositivo para automóvil
        else if (Arrays.asList(techs).contains("android.nfc.tech.NfcA") && Arrays.asList(techs).contains("android.nfc.tech.MifareUltralight") && Arrays.asList(techs).contains("android.nfc.tech.Ndef")) {
            appendLog("I", TAG, "Dispositivo: Mi TAG\n");
        }

        // Identificación de tarjetas bancarias
        else if (Arrays.asList(techs).contains("android.nfc.tech.IsoDep") && Arrays.asList(techs).contains("android.nfc.tech.NfcA")) {
            appendLog("I", TAG, "Dispositivo: Tarjeta Bancaria\n");
        }

        // **Dispositivo desconocido**: Si no se puede identificar
        else {
            appendLog("I", TAG, "Dispositivo: Desconocido\n");
        }
    }


    // Añade un log al registro
    private void appendLog(String level, String tag, String message) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(new Date());
        int pid = android.os.Process.myPid();
        int tid = android.os.Process.myTid();
        String pkg = requireContext().getPackageName();

        String formatted = String.format(Locale.getDefault(),
                "%s %5d-%5d %-25s %-35s %-1s  %s",
                timestamp, pid, tid, tag, pkg, level.toUpperCase(), message
        );

        Log.println(getAndroidLogLevel(level), tag, message);
        allLogs.add(formatted);
        fullLog.append(formatted).append("\n");

        aplicarFiltros();
    }

    // Convierte el nivel de log a constante de Android
    private int getAndroidLogLevel(String level) {
        switch (level.toUpperCase()) {
            case "V": return Log.VERBOSE;
            case "I": return Log.INFO;
            case "W": return Log.WARN;
            case "E": return Log.ERROR;
            default: return Log.DEBUG;
        }
    }

    // Aplica filtros de búsqueda y tecnologías
    private void aplicarFiltros() {
        List<String> finalLogs = new ArrayList<>();

        for (String log : allLogs) {
            boolean matchesQuery = currentQuery.isEmpty() || log.toLowerCase().contains(currentQuery.toLowerCase());
            boolean matchesFilter = selectedTechFilters.isEmpty() || selectedTechFilters.stream().anyMatch(log::contains);
            if (matchesQuery && matchesFilter) finalLogs.add(log);
        }

        StringBuilder visibleLog = new StringBuilder("NFC:/\n");
        for (String log : finalLogs) visibleLog.append(log).append("\n");

        consoleTextView.setText(visibleLog.toString());
        consoleScrollView.post(() -> consoleScrollView.fullScroll(View.FOCUS_DOWN));
    }

    // Muestra diálogo de filtro por tecnologías
    private void mostrarDialogoFiltros() {
        boolean[] checkedItems = new boolean[techOptions.length];
        for (int i = 0; i < techOptions.length; i++)
            checkedItems[i] = selectedTechFilters.contains(techOptions[i]);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Filtrar por tecnologías")
                .setMultiChoiceItems(techOptions, checkedItems, (dialog, which, isChecked) -> {
                    String tech = techOptions[which];
                    if (isChecked) selectedTechFilters.add(tech); else selectedTechFilters.remove(tech);
                })
                .setPositiveButton("Aplicar", (dialog, which) -> aplicarFiltros())
                .setNegativeButton("Cancelar", null)
                .setNeutralButton("Deseleccionar todo", (dialog, which) -> {
                    selectedTechFilters.clear();
                    aplicarFiltros();
                    Toast.makeText(requireContext(), "Filtros retirados", Toast.LENGTH_SHORT).show();
                }).show();
    }

    // Guarda el log actual en almacenamiento externo
    private void guardarLog() {
        try {
            File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            File appDir = new File(documentsDir, "NFCatch");
            if (!appDir.exists()) appDir.mkdirs();

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File file = new File(appDir, "log_nfc_" + timeStamp + ".txt");

            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(fullLog.toString().getBytes());
                Toast.makeText(getContext(), "Log guardado en Documents/NFCatch", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error al guardar el log: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Convierte un arreglo de bytes a representación hexadecimal para el UID del chip
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02X ", b));
        return sb.toString().trim();
    }
}

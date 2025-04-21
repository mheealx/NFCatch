package com.esime.nfcdroid2.ui.home;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.esime.nfcdroid2.R;
import com.esime.nfcdroid2.services.NfcBackgroundService;
import com.esime.nfcdroid2.utils.LogCallback;
import com.esime.nfcdroid2.utils.LogRegistry;
import com.esime.nfcdroid2.utils.NfcAHelper;
import com.esime.nfcdroid2.utils.NfcBHelper;
import com.esime.nfcdroid2.utils.NfcIsoDepHelper;
import com.esime.nfcdroid2.utils.NfcMifareClassicHelper;
import com.esime.nfcdroid2.utils.NfcMifareUltralightHelper;
import com.esime.nfcdroid2.utils.NfcNdefFormatableHelper;
import com.esime.nfcdroid2.utils.NfcNdefHelper;
import com.esime.nfcdroid2.utils.NfcVHelper;

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
            "Ndef", "MifareClassic", "MifareUltralight", "NfcA", "NfcB", "NfcV", "IsoDep", "NdefFormatable"
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        consoleTextView = root.findViewById(R.id.consoleTextView);
        consoleScrollView = root.findViewById(R.id.consoleScrollView);
        searchView = root.findViewById(R.id.searchView);
        filterButton = root.findViewById(R.id.filterButton);
        saveButton = root.findViewById(R.id.saveButton);
        clearConsoleButton = root.findViewById(R.id.clearConsoleButton);

        nfcAdapter = NfcAdapter.getDefaultAdapter(requireContext());

        if (nfcAdapter == null) {
            disableUi();
            consoleTextView.setText("Este dispositivo no es compatible con NFC.");
            Toast.makeText(requireContext(), "La aplicación se cerrará automáticamente en 5 segundos.", Toast.LENGTH_LONG).show();
            new android.os.Handler().postDelayed(() -> requireActivity().finishAffinity(), 5000);
            return root;
        }

        Intent intent = new Intent(requireActivity(), requireActivity().getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        pendingIntent = PendingIntent.getActivity(requireContext(), 0, intent, PendingIntent.FLAG_MUTABLE);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { currentQuery = query; applyFilters(); return true; }
            @Override public boolean onQueryTextChange(String newText) { currentQuery = newText; applyFilters(); return true; }
        });

        filterButton.setOnClickListener(v -> showFilterDialog());
        saveButton.setOnClickListener(v -> guardarLog());
        clearConsoleButton.setOnClickListener(v -> clearConsole());

        return root;
    }

    private void disableUi() {
        searchView.setEnabled(false);
        filterButton.setEnabled(false);
        saveButton.setEnabled(false);
        clearConsoleButton.setEnabled(false);
    }

    private void clearConsole() {
        fullLog.setLength(0);
        allLogs.clear();
        currentQuery = "";
        selectedTechFilters.clear();
        consoleTextView.setText("NFC:/\n");
        Toast.makeText(requireContext(), "Consola reiniciada", Toast.LENGTH_SHORT).show();
    }

    private void showNfcDisabledDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("NFC DESACTIVADO")
                .setMessage("El dispositivo tiene el chip NFC APAGADO.\nPor favor, enciéndalo para continuar.")
                .setCancelable(false)
                .setPositiveButton("Ir a configuración", (dialog, which) -> {
                    Intent intent = new Intent(android.provider.Settings.ACTION_NFC_SETTINGS);
                    startActivity(intent);
                }).show();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (nfcAdapter != null) {
            if (!nfcAdapter.isEnabled()) {
                showNfcDisabledDialog();
            } else {
                nfcAdapter.enableForegroundDispatch(requireActivity(), pendingIntent, null, null);
            }
        }

        LogRegistry.setListener(this);
        for (String linea : LogRegistry.getLogEvents()) {
            allLogs.add(linea);
            fullLog.append(linea).append("\n");
        }
        applyFilters();

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
                applyFilters();
            }
        });
    }

    public void handleNfcIntent(Intent intent) {
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag != null) {
            NfcBackgroundService.marcarLecturaNfc(); // ✅ Evitar falso positivo de pantalla apagada

            appendLog("D", TAG, "Tag detectado");
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
                    case "android.nfc.tech.NfcV": NfcVHelper.read(tag, logger); break;
                    case "android.nfc.tech.IsoDep": NfcIsoDepHelper.read(tag, logger); break;
                }
            }
        }
    }

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

        if (currentQuery.isEmpty() && selectedTechFilters.isEmpty()) {
            consoleTextView.post(() -> {
                String current = consoleTextView.getText().toString();
                consoleTextView.setText(current + formatted + "\n");
            });
        } else {
            applyFilters();
        }
    }

    private int getAndroidLogLevel(String level) {
        switch (level.toUpperCase()) {
            case "V": return Log.VERBOSE;
            case "D": return Log.DEBUG;
            case "I": return Log.INFO;
            case "W": return Log.WARN;
            case "E": return Log.ERROR;
            default: return Log.DEBUG;
        }
    }

    private void applyFilters() {
        List<String> finalLogs = new ArrayList<>();

        for (String log : allLogs) {
            boolean matchesQuery = currentQuery.isEmpty() || log.toLowerCase().contains(currentQuery.toLowerCase());
            boolean matchesFilter = selectedTechFilters.isEmpty() || selectedTechFilters.stream().anyMatch(log::contains);
            if (matchesQuery && matchesFilter) finalLogs.add(log);
        }

        StringBuilder visibleLog = new StringBuilder("NFC:\n");
        for (String log : finalLogs) visibleLog.append(log).append("\n");

        consoleTextView.setText(visibleLog.toString());
        consoleScrollView.post(() -> consoleScrollView.fullScroll(View.FOCUS_DOWN));
    }

    private void showFilterDialog() {
        boolean[] checkedItems = new boolean[techOptions.length];
        for (int i = 0; i < techOptions.length; i++)
            checkedItems[i] = selectedTechFilters.contains(techOptions[i]);

        new AlertDialog.Builder(requireContext())
                .setTitle("Filtrar por tecnologías")
                .setMultiChoiceItems(techOptions, checkedItems, (dialog, which, isChecked) -> {
                    String tech = techOptions[which];
                    if (isChecked) selectedTechFilters.add(tech); else selectedTechFilters.remove(tech);
                })
                .setPositiveButton("Aplicar", (dialog, which) -> applyFilters())
                .setNegativeButton("Cancelar", null)
                .setNeutralButton("Deseleccionar todo", (dialog, which) -> {
                    selectedTechFilters.clear();
                    applyFilters();
                    Toast.makeText(requireContext(), "Filtros retirados", Toast.LENGTH_SHORT).show();
                }).show();
    }

    private void guardarLog() {
        try {
            File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            File appDir = new File(documentsDir, "NFCDroid");
            if (!appDir.exists()) appDir.mkdirs();

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File file = new File(appDir, "log_nfc_" + timeStamp + ".txt");

            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(fullLog.toString().getBytes());
                Toast.makeText(getContext(), "Log guardado en Documents/NFCDroid", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error al guardar el log: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02X ", b));
        return sb.toString().trim();
    }
}

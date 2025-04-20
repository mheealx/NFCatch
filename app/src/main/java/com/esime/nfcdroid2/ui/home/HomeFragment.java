package com.esime.nfcdroid2.ui.home;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.esime.nfcdroid2.databinding.FragmentHomeBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private TextView consoleTextView;
    private Button saveButton;
    private final StringBuilder logContent = new StringBuilder();

    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        consoleTextView = binding.consoleTextView;
        saveButton = binding.saveButton;

        logContent.append(formatLogLine("I", "NfcLog", "Consola NFC iniciada."));

        nfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());
        if (nfcAdapter == null) {
            String msg = "NFC no está disponible en este dispositivo.";
            consoleTextView.setText(msg);
            logContent.append(formatLogLine("E", "NfcLog", msg));
        }

        pendingIntent = PendingIntent.getActivity(
                requireActivity(),
                0,
                new Intent(requireActivity(), requireActivity().getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE
        );

        saveButton.setOnClickListener(v -> {
            guardarLogAutomaticamente();
        });

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (nfcAdapter != null) {
            nfcAdapter.enableForegroundDispatch(getActivity(), pendingIntent, null, null);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(getActivity());
        }
    }

    public void handleNfcIntent(Intent intent) {
        String action = intent.getAction();
        StringBuilder eventInfo = new StringBuilder();
        eventInfo.append(formatLogLine("I", "NfcDispatcher", "Intent NFC recibido: " + action));

        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (rawMsgs != null) {
            for (Parcelable rawMsg : rawMsgs) {
                NdefMessage msg = (NdefMessage) rawMsg;
                for (NdefRecord record : msg.getRecords()) {
                    if (record.getTnf() == NdefRecord.TNF_WELL_KNOWN &&
                            Arrays.equals(record.getType(), NdefRecord.RTD_TEXT)) {
                        String text = parseTextRecord(record);
                        eventInfo.append(formatLogLine("I", "NfcDispatcher", "Texto: " + text));
                    }
                }
            }
        } else {
            eventInfo.append(formatLogLine("W", "NfcDispatcher", "No se encontraron mensajes NDEF."));
        }

        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag != null) {
            eventInfo.append(formatLogLine("I", "NfcDispatcher", "ID: " + bytesToHex(tag.getId())));
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                NdefMessage cachedMsg = ndef.getCachedNdefMessage();
                if (cachedMsg != null) {
                    for (NdefRecord record : cachedMsg.getRecords()) {
                        eventInfo.append(formatLogLine("I", "NDEF", "Payload: " + new String(record.getPayload())));
                    }
                }
            } else {
                NdefFormatable formatable = NdefFormatable.get(tag);
                if (formatable != null) {
                    eventInfo.append(formatLogLine("I", "NdefFormatable", "La etiqueta es formateable a NDEF."));
                } else {
                    eventInfo.append(formatLogLine("W", "NFC", "La etiqueta no es NDEF ni formateable."));
                }
            }
        }

        logContent.append(eventInfo);
        consoleTextView.append(eventInfo);
    }

    private String formatLogLine(String level, String tag, String message) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
                .format(new Date());
        return String.format(
                "%s  %-10s  %-12s  %s\n",
                timestamp, level, tag, message
        );
    }

    private void guardarLogAutomaticamente() {
        try {
            // Ruta base a la carpeta Documents/NFCDroid
            File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            File nfcdroidDir = new File(documentsDir, "NFCDroid");

            // Crear la carpeta si no existe
            if (!nfcdroidDir.exists()) {
                if (!nfcdroidDir.mkdirs()) {
                    Toast.makeText(getActivity(), "No se pudo crear la carpeta NFCDroid.", Toast.LENGTH_LONG).show();
                    return;
                }
            }

            // Crear el nombre del archivo con la fecha y hora
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File logFile = new File(nfcdroidDir, "log_nfc_" + timeStamp + ".txt");

            // Escribir el log
            try (FileOutputStream fos = new FileOutputStream(logFile)) {
                fos.write(logContent.toString().getBytes());
                Toast.makeText(getActivity(), "Log guardado en: Documents/NFCDroid", Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            Toast.makeText(getActivity(), "Error al guardar el log: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private String parseTextRecord(NdefRecord record) {
        try {
            byte[] payload = record.getPayload();
            String textEncoding = ((payload[0] & 0x80) == 0) ? "UTF-8" : "UTF-16";
            int languageCodeLength = payload[0] & 0x3F;
            return new String(payload, languageCodeLength + 1,
                    payload.length - languageCodeLength - 1, textEncoding);
        } catch (Exception e) {
            return "Error al parsear texto: " + e.getMessage();
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

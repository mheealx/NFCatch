package com.esime.nfcdroid2;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Parcelable;


import java.nio.charset.StandardCharsets;

import com.esime.nfcdroid2.services.ServicioSegundoPlano;

public class NfcReaderActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleIntent(getIntent());
        finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
        finish();
    }

    private void handleIntent(Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction()) ||
                NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction()) ||
                NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {

            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

            Intent serviceIntent = new Intent(this, ServicioSegundoPlano.class);
            serviceIntent.setAction("com.esime.nfcdroid2.ACTION_HANDLE_TAG");
            if (tag != null) {
                serviceIntent.putExtra(NfcAdapter.EXTRA_TAG, tag);
            }

            // Intentamos extraer texto NDEF si existe
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMsgs != null && rawMsgs.length > 0) {
                try {
                    NdefMessage message = (NdefMessage) rawMsgs[0];
                    NdefRecord[] records = message.getRecords();
                    if (records != null && records.length > 0) {
                        NdefRecord record = records[0];
                        byte[] payload = record.getPayload();
                        String text = new String(payload, "UTF-8");

                        serviceIntent.putExtra("NFC_TEXT", text);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // 🚀 Siempre iniciamos el servicio, con o sin texto
            startService(serviceIntent);
        }
    }
}

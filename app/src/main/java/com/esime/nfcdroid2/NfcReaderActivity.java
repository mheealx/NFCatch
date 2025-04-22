package com.esime.nfcdroid2;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;

import com.esime.nfcdroid2.services.ServicioSegundoPlano;

public class NfcReaderActivity extends Activity {

    private static final String TAG = "NfcReader";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleIntent(getIntent());
        finish(); // No muestra UI
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
        finish(); // Igual cerrar
    }

    private void handleIntent(Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag != null) {
            Log.i(TAG, "Tag recibido, enviando al servicio en segundo plano...");
            Intent serviceIntent = new Intent(this, ServicioSegundoPlano.class);
            serviceIntent.setAction("com.esime.nfcdroid2.ACTION_HANDLE_TAG");
            serviceIntent.putExtra(NfcAdapter.EXTRA_TAG, tag);
            startService(serviceIntent); // Sin abrir UI
        }
    }
}

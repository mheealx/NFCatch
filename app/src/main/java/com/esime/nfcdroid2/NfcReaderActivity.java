
package com.esime.nfcdroid2;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;


import com.esime.nfcdroid2.services.ServicioSegundoPlano;

// Actividad encargada de capturar Tags NFC y enviarlos al servicio en segundo plano
public class NfcReaderActivity extends Activity {


    // Inicialización de la actividad
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

    // Procesa el intento y envía el Tag al servicio en segundo plano
    private void handleIntent(Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag != null) {
            Intent serviceIntent = new Intent(this, ServicioSegundoPlano.class);
            serviceIntent.setAction("com.esime.nfcdroid2.ACTION_HANDLE_TAG");
            serviceIntent.putExtra(NfcAdapter.EXTRA_TAG, tag);
            startService(serviceIntent);
        }
    }
}

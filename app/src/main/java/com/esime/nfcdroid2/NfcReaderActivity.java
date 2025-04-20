package com.esime.nfcdroid2;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;

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
        if (intent == null || intent.getAction() == null) {
            Log.e("NfcReader", "Intent NFC nulo o sin acción.");
            return;
        }

        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag == null) {
            Log.e("NfcReader", "No se pudo obtener el Tag del intent.");
            return;
        }

        Log.i("NfcReader", "Tag detectado, redirigiendo a MainActivity...");

        // Redirige el intent con el tag a MainActivity
        Intent redirect = new Intent(this, MainActivity.class);
        redirect.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        redirect.putExtra(NfcAdapter.EXTRA_TAG, tag);
        startActivity(redirect);
    }
}

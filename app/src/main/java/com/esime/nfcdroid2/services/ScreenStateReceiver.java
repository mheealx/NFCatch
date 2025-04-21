package com.esime.nfcdroid2.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.util.Log;

public class ScreenStateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(context);

        String estadoNfc = (adapter != null && adapter.isEnabled()) ? "NFC ACTIVO" : "NFC DESACTIVADO";

        if (Intent.ACTION_SCREEN_ON.equals(action)) {
            Log.i("NFC_SCREEN", "Pantalla ENCENDIDA - Estado del chip: " + estadoNfc);
        } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
            Log.i("NFC_SCREEN", "Pantalla APAGADA - Estado del chip: " + estadoNfc);
        }
    }
}

package com.esime.nfcdroid2.utils;

import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;

//Lector de tecnología Mifare Ultralight
public class NfcMifareUltralightHelper {

    public static void read(Tag tag, LogCallback logger) {
        try {
            MifareUltralight ultra = MifareUltralight.get(tag);
            if (ultra != null) {
                int type = ultra.getType();
                String typeStr;
                if (type == MifareUltralight.TYPE_ULTRALIGHT) {
                    typeStr = "UL";
                } else if (type == MifareUltralight.TYPE_ULTRALIGHT_C) {
                    typeStr = "UL-C";
                } else {
                    typeStr = "Desconocido";
                }

                logger.log("D", "MifareUltralight", "Mifare Ultralight detectado - Tipo: " + typeStr);
            }
        } catch (Exception e) {
            logger.log("E", "MifareUltralight", "Error: " + e.getMessage());
        }
    }
}

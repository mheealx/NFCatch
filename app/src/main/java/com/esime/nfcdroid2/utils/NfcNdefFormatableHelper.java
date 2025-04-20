package com.esime.nfcdroid2.utils;

import android.nfc.Tag;
import android.nfc.tech.NdefFormatable;

public class NfcNdefFormatableHelper {

    public static void read(Tag tag, LogCallback logger) {
        try {
            NdefFormatable formatable = NdefFormatable.get(tag);
            if (formatable != null) {
                logger.log("D", "NdefFormatable", "Este tag es NDEF Formateable pero no contiene mensajes NDEF aún.");
            } else {
                logger.log("W", "NdefFormatable", "Este tag no puede ser formateado como NDEF.");
            }
        } catch (Exception e) {
            logger.log("E", "NdefFormatable", "Error evaluando NDEF Formateable: " + e.getMessage());
        }
    }
}

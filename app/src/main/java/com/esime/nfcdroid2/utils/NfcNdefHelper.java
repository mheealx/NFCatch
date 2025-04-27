package com.esime.nfcdroid2.utils;

import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.Tag;
import android.nfc.tech.Ndef;

import java.nio.charset.StandardCharsets;

//Lector de tecnología NFC NDEF

public class NfcNdefHelper {

    public static void read(Tag tag, LogCallback logger) {
        Ndef ndef = Ndef.get(tag);
        if (ndef == null) {
            logger.log("W", "NDEF", "Ndef.get() devolvió null. Puede que el tag no esté formateado como NDEF.");
            return;
        }

        try {
            ndef.connect();
            logger.log("D", "NDEF", "NDEF conectado correctamente");
            logger.log("D", "NDEF", "Tipo: " + ndef.getType());
            logger.log("D", "NDEF", "Tamaño máximo: " + ndef.getMaxSize() + " bytes");
            logger.log("D", "NDEF", "Escribible: " + ndef.isWritable());

            NdefMessage message = ndef.getCachedNdefMessage();
            if (message != null) {
                NdefRecord[] records = message.getRecords();
                logger.log("D", "NDEF", "Cantidad de records: " + records.length);
                for (int i = 0; i < records.length; i++) {
                    NdefRecord record = records[i];
                    String type = new String(record.getType(), StandardCharsets.UTF_8);
                    String payload = new String(record.getPayload(), StandardCharsets.UTF_8);
                    logger.log("D", "NDEF", "Record #" + (i + 1));
                    logger.log("D", "NDEF", "Tipo: " + type + " | Payload: " + payload);
                }
            } else {
                logger.log("W", "NDEF", "No se encontraron mensajes NDEF.");
            }

            ndef.close();
        } catch (Exception e) {
            logger.log("E", "NDEF", "Error accediendo a NDEF: " + e.getMessage());
        }
    }
}

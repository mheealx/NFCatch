package com.esime.nfcdroid2.utils;

import android.nfc.Tag;
import android.nfc.tech.MifareClassic;

//Lector de tecnología Mifare Classic

public class NfcMifareClassicHelper {

    public static void read(Tag tag, LogCallback logger) {
        try {
            MifareClassic mifare = MifareClassic.get(tag);
            if (mifare != null) {
                logger.log("D", "MifareClassic", "MIFARE Classic detectado");
                logger.log("D", "MifareClassic", "Tipo: " + mifare.getType());
                logger.log("D", "MifareClassic", "Tamaño: " + mifare.getSize() + " bytes");
                logger.log("D", "MifareClassic", "Sectores: " + mifare.getSectorCount());
                logger.log("D", "MifareClassic", "Bloques: " + mifare.getBlockCount());
            }
        } catch (Exception e) {
            logger.log("E", "MifareClassic", "Error: " + e.getMessage());
        }
    }
}

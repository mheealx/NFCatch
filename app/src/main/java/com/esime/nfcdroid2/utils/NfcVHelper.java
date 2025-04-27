package com.esime.nfcdroid2.utils;

import android.nfc.Tag;
import android.nfc.tech.NfcV;

//Lector de tecnología NFC V

public class NfcVHelper {

    public static void read(Tag tag, LogCallback logger) {
        try {
            NfcV nfcv = NfcV.get(tag);
            if (nfcv != null) {
                nfcv.connect();
                logger.log("D", "NfcV", "NFC-V detectado");
                logger.log("D", "NfcV", "DSF ID: " + String.format("0x%02X", nfcv.getDsfId()));
                logger.log("D", "NfcV", "Response Flags: " + String.format("0x%02X", nfcv.getResponseFlags()));
                nfcv.close();
            }
        } catch (Exception e) {
            logger.log("E", "NfcV", "Error: " + e.getMessage());
        }
    }
}

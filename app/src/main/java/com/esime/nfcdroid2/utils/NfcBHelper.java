package com.esime.nfcdroid2.utils;

import android.nfc.Tag;
import android.nfc.tech.NfcB;

public class NfcBHelper {

    public static void read(Tag tag, LogCallback logger) {
        try {
            NfcB nfcB = NfcB.get(tag);
            if (nfcB != null) {
                nfcB.connect();
                logger.log("D", "NfcB", "NFC-B detectado");
                logger.log("D", "NfcB", "Application Data: " + bytesToHex(nfcB.getApplicationData()));
                logger.log("D", "NfcB", "Protocol Info: " + bytesToHex(nfcB.getProtocolInfo()));
                nfcB.close();
            }
        } catch (Exception e) {
            logger.log("E", "NfcB", "Error: " + e.getMessage());
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02X ", b));
        return sb.toString().trim();
    }
}

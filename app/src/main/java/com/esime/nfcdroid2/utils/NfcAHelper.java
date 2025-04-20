package com.esime.nfcdroid2.utils;

import android.nfc.Tag;
import android.nfc.tech.NfcA;

public class NfcAHelper {

    public static void read(Tag tag, LogCallback logger) {
        try {
            NfcA nfcA = NfcA.get(tag);
            if (nfcA != null) {
                nfcA.connect();
                logger.log("D", "NfcA", "NfcA detectado");
                logger.log("D", "NfcA", "SAK: " + nfcA.getSak());
                logger.log("D", "NfcA", "ATQA: " + bytesToHex(nfcA.getAtqa()));
                logger.log("D", "NfcA", "Timeout: " + nfcA.getTimeout() + " ms");
                nfcA.close();
            }
        } catch (Exception e) {
            logger.log("E", "NfcA", "Error: " + e.getMessage());
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }
}

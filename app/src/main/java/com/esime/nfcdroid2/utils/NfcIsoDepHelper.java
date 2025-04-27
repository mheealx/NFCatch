package com.esime.nfcdroid2.utils;

import android.nfc.Tag;
import android.nfc.tech.IsoDep;

//Lector de tecnología NFC IsoDep
public class NfcIsoDepHelper {

    public static void read(Tag tag, LogCallback logger) {
        try {
            IsoDep iso = IsoDep.get(tag);
            if (iso != null) {
                iso.connect();
                logger.log("D", "IsoDep", "ISO-DEP conectado");
                logger.log("D", "IsoDep", "Timeout: " + iso.getTimeout() + " ms");
                logger.log("D", "IsoDep", "MaxTransceiveLength: " + iso.getMaxTransceiveLength());
                logger.log("D", "IsoDep", "Extended Length Supported: " + iso.isExtendedLengthApduSupported());

                byte[] historical = iso.getHistoricalBytes();
                if (historical != null) {
                    logger.log("D", "IsoDep", "Historical Bytes: " + bytesToHex(historical));
                }

                byte[] hiLayerResp = iso.getHiLayerResponse();
                if (hiLayerResp != null) {
                    logger.log("D", "IsoDep", "Hi Layer Response: " + bytesToHex(hiLayerResp));
                }

                iso.close();
            }
        } catch (Exception e) {
            logger.log("E", "IsoDep", "Error leyendo ISO-DEP: " + e.getMessage());
        }
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02X ", b));
        return sb.toString().trim();
    }
}

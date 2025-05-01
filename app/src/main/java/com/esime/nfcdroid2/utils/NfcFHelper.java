package com.esime.nfcdroid2.utils;

import android.nfc.Tag;
import android.nfc.tech.NfcF;

public class NfcFHelper {

    public static void read(Tag tag, LogCallback logger) {
        NfcF nfcF = null;

        try {
            // Obtener el objeto NfcF desde el tag
            nfcF = NfcF.get(tag);
            if (nfcF != null) {
                nfcF.connect();
                logger.log("D", "NfcF", "NFC-F detectado");

                // Enviar un comando para leer datos, por ejemplo, el comando REQA (Request for Answer to Request)
                byte[] command = new byte[]{(byte) 0x26}; // REQA comando
                byte[] response = nfcF.transceive(command);

                // Mostrar la respuesta obtenida del tag
                if (response != null) {
                    logger.log("D", "NfcF", "Respuesta del tag: " + bytesToHex(response));
                }

                // Consultar el identificador único (UID) de la etiqueta
                byte[] tagId = tag.getId();
                logger.log("D", "NfcF", "Tag ID: " + bytesToHex(tagId));

            } else {
                logger.log("E", "NfcF", "No se pudo obtener NfcF.");
            }

        } catch (Exception e) {
            logger.log("E", "NfcF", "Error: " + e.getMessage());
        } finally {
            try {
                // Asegurarse de cerrar la conexión con el tag correctamente
                if (nfcF != null && nfcF.isConnected()) {
                    nfcF.close();
                    logger.log("D", "NfcF", "Conexión con NFC-F cerrada correctamente.");
                }
            } catch (Exception e) {
                logger.log("E", "NfcF", "Error cerrando la conexión con el tag: " + e.getMessage());
            }
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02X ", b));
        return sb.toString().trim();
    }
}

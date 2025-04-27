package com.esime.nfcdroid2.ui.informacion.componentes;

import android.util.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//Identificador del chip NFC del dispositivo
public class NfcChipIdentifier {

    // Metodo principal para detectar el chip NFC
    public static String detect() {
        List<ChipGuess> guesses = new ArrayList<>();

        detectBroadcom(guesses);
        detectNxp(guesses);
        detectSamsung(guesses);
        detectSt(guesses);
        detectNxpOppo(guesses);

        ChipGuess best = null;
        for (ChipGuess guess : guesses) {
            if (best == null) {
                best = guess;
            }
        }

        return best != null ? best.toString() : "Desconocido -- No se reconoce el chip";
    }

    // Detecta dispositivos con chip Broadcom
    private static void detectBroadcom(List<ChipGuess> guesses) {
        for (String path : searchPaths("libnfc-brcm.conf")) {
            readLines(path, line -> {
                if (line.contains("TRANSPORT_DRIVER")) {
                    String val = splitConfigLine(line).second;
                    if (val != null && exists(val)) {
                        String chip = "Broadcom Device " + val.replace("/dev/", "");
                        guesses.add(new ChipGuess(chip, 0.9f));
                        return false; // Ya encontrado
                    }
                }
                return true;
            });
        }
    }

    // Detecta los chips en Samsung
    private static void detectSamsung(List<ChipGuess> guesses) {
        for (String path : searchPaths("libnfc-sec-vendor.conf")) {
            readLines(path, line -> {
                if (line.startsWith("FW_FILE_NAME") || line.startsWith("RF_FILE_NAME")) {
                    String val = splitConfigLine(line).second;
                    if (val != null) {
                        String[] tokens = val.replace("\"", "").split("_");
                        if (tokens.length > 0) {
                            guesses.add(new ChipGuess("Samsung " + tokens[0].toUpperCase(), 0.9f));
                        }
                    }
                }
                return true;
            });
        }
    }

    // Detecta chips STMicroelectronics leyendo nombres de firmware
    private static void detectSt(List<ChipGuess> guesses) {
        for (String path : searchPaths("libnfc-hal-st.conf")) {
            readLines(path, line -> {
                if (line.contains("STNFC_FW_BIN_NAME") || line.contains("STNFC_FW_CONF_NAME")) {
                    String val = splitConfigLine(line).second;
                    if (val != null) {
                        Matcher m = Pattern.compile(".*/(\\w+)_").matcher(val);
                        if (m.lookingAt()) {
                            guesses.add(new ChipGuess("ST " + m.group(1).toUpperCase(), 0.9f));
                        }
                    }
                }
                return true;
            });
        }
    }

    // Detecta chips NXP consultando archivos comunes o configuración directa del sistema
    private static void detectNxp(List<ChipGuess> guesses) {
        List<String> filenames = new ArrayList<>(Arrays.asList("libnfc-nxp.conf"));

        String sku = getSystemProp("ro.boot.product.hardware.sku");
        if (!sku.isEmpty()) {
            filenames.add("libnfc-" + sku + ".conf");
            filenames.add("libnfc-nxp-" + sku + ".conf");
        }

        String directConf = getSystemProp("persist.vendor.nfc.config_file_name");
        if (!directConf.isEmpty()) filenames.add(directConf);

        final Map<String, String> chipMap = getNxpChipMap();
        for (String path : searchPaths(filenames)) {
            readLines(path, line -> {
                if (line.contains("NXP_NFC_CHIP") || line.contains("NXP_NFC_CHIP_TYPE")) {
                    String val = splitConfigLine(line).second;
                    if (val != null) {
                        String chipName = chipMap.getOrDefault(val, "Desconocido -- No se reconoce el chip");
                        guesses.add(new ChipGuess("NXP " + chipName, 0.8f));
                    }
                }
                return true;
            });
        }
    }

    // Detecta chips NXP en dispositivos Oppo, OnePlus, Realme
    private static void detectNxpOppo(List<ChipGuess> guesses) {
        List<String> suffixSources = Arrays.asList(
                getSystemProp("ro.separate.soft"),
                getSystemProp("ro.build.product"),
                readFile("/proc/oplusVersion/prjName"),
                readFile("/proc/oppoVersion/prjName"),
                readFile("/proc/oplusVersion/prjVersion"),
                readFile("/proc/oppoVersion/prjVersion")
        );

        String refPath = findFile("nfc_fw_ref");
        if (refPath == null) return;

        String suffix = findSuffix(refPath, suffixSources);
        if (suffix != null) {
            String[] parts = suffix.split("_");
            if (parts.length > 0) {
                guesses.add(new ChipGuess(parts[0].toUpperCase(), 0.91f));
            }
        }
    }

    // ─────────────────────────────────────────────
    // Métodos de utilidad
    // ─────────────────────────────────────────────

    // Retorna directorios comunes donde buscar configuraciones NFC
    private static List<String> getSearchDirs() {
        return Arrays.asList("/vendor/etc/", "/odm/etc/", "/product/etc/", "/system/etc/", "/etc/");
    }

    // Busca archivos en los directorios disponibles
    private static List<String> searchPaths(String filename) {
        return searchPaths(Collections.singletonList(filename));
    }

    // Busca múltiples archivos en los directorios disponibles
    private static List<String> searchPaths(List<String> filenames) {
        List<String> found = new ArrayList<>();
        for (String dir : getSearchDirs()) {
            for (String file : filenames) {
                String path = dir + file;
                if (exists(path)) found.add(path);
            }
        }
        return found;
    }

    // Encuentra un archivo específico en la estructura de NFC
    private static String findFile(String filename) {
        for (String dir : getSearchDirs()) {
            String path = dir + "nfc/" + filename;
            if (exists(path)) return path;
        }
        return null;
    }

    // Verifica si un archivo existe
    private static boolean exists(String path) {
        return new File(path).exists();
    }

    // Lee un archivo línea por línea y ejecuta una acción por cada línea
    private static void readLines(String path, LineHandler handler) {
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!handler.handle(line.trim())) break;
            }
        } catch (Exception ignored) {}
    }

    // Lee el contenido completo de un archivo
    private static String readFile(String path) {
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            StringBuilder b = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                b.append(line);
            }
            return b.toString().trim();
        } catch (Exception e) {
            return "";
        }
    }

    // Obtiene una propiedad del sistema Android
    private static String getSystemProp(String prop) {
        try {
            Process process = new ProcessBuilder("getprop", prop).start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            return reader.readLine().trim();
        } catch (Exception e) {
            return "";
        }
    }

    // Busca un sufijo de versión basado en un archivo de referencia
    private static String findSuffix(String refPath, List<String> projectNames) {
        try (BufferedReader reader = new BufferedReader(new FileReader(refPath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#") || line.trim().isEmpty()) continue;

                String[] split = line.split(":", 2);
                if (split.length != 2) continue;

                String target = split[0].trim();
                String[] names = split[1].trim().split("\\s+");
                for (String candidate : projectNames) {
                    if (candidate != null && Arrays.asList(names).contains(candidate.trim())) {
                        return target;
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    // Divide una línea de configuración clave=valor
    private static Pair<String, String> splitConfigLine(String line) {
        String[] parts = line.split("=", 2);
        if (parts.length == 2) {
            return new Pair<>(parts[0].trim(), parts[1].trim().replaceAll("(^\")|(\"$)", ""));
        }
        return null;
    }

    // Mapa que traduce identificadores de chips NXP a nombres amigables
    private static Map<String, String> getNxpChipMap() {
        Map<String, String> map = new HashMap<>();
        map.put("0x01", "PN547C2"); map.put("0x02", "PN65T");
        map.put("0x03", "PN548AD"); map.put("0x04", "PN66T");
        map.put("0x05", "PN551");   map.put("0x06", "PN67T");
        map.put("0x07", "PN553");   map.put("0x08", "PN80T");
        map.put("0x09", "PN557");   map.put("0x0A", "PN81T");
        map.put("0x0B", "SN1X0");   map.put("0x0C", "SN2X0");
        return map;
    }


    private interface LineHandler {
        boolean handle(String line);
    }
}

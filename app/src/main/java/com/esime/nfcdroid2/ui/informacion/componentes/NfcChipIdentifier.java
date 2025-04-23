package com.esime.nfcdroid2.ui.informacion.componentes;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Basado en conceptos de detección de chip de proyectos como NfcGate.
 * Implementación propia adaptada para este proyecto.
 */

public class NfcChipIdentifier {

    public static String detect() {
        List<ChipGuess> guesses = new ArrayList<>();

        detectBroadcom(guesses);
        detectNxp(guesses);
        detectSamsung(guesses);
        detectSt(guesses);
        detectNxpOppo(guesses);

        // Elegimos la de mayor confianza
        ChipGuess best = null;
        for (ChipGuess guess : guesses) {
            if (best == null) {
                best = guess;
            }
        }

        return best != null ? best.toString() : "Unknown";
    }

    // ─────────────────────────────────────────────
    // 🔍 Detección de cada tipo
    // ─────────────────────────────────────────────

    private static void detectBroadcom(List<ChipGuess> results) {
        for (String path : searchPaths("libnfc-brcm.conf")) {
            readLines(path, line -> {
                if (line.contains("TRANSPORT_DRIVER")) {
                    String val = splitValue(line);
                    if (val != null && exists(val)) {
                        String chip = "Broadcom Device " + val.replace("/dev/", "");
                        results.add(new ChipGuess(chip, 0.9f));
                        return false;
                    }
                }
                return true;
            });
        }
    }

    private static void detectSamsung(List<ChipGuess> results) {
        for (String path : searchPaths("libnfc-sec-vendor.conf")) {
            readLines(path, line -> {
                if (line.startsWith("FW_FILE_NAME") || line.startsWith("RF_FILE_NAME")) {
                    String val = splitValue(line);
                    if (val != null) {
                        String[] tokens = val.replace("\"", "").split("_");
                        if (tokens.length > 0) {
                            results.add(new ChipGuess("Samsung " + tokens[0].toUpperCase(), 0.9f));
                        }
                    }
                }
                return true;
            });
        }
    }

    private static void detectSt(List<ChipGuess> results) {
        for (String path : searchPaths("libnfc-hal-st.conf")) {
            readLines(path, line -> {
                if (line.contains("STNFC_FW_BIN_NAME") || line.contains("STNFC_FW_CONF_NAME")) {
                    String val = splitValue(line);
                    if (val != null) {
                        Matcher m = Pattern.compile(".*/(\\w+)_").matcher(val);
                        if (m.lookingAt()) {
                            results.add(new ChipGuess("ST " + m.group(1).toUpperCase(), 0.9f));
                        }
                    }
                }
                return true;
            });
        }
    }

    private static void detectNxp(List<ChipGuess> results) {
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
                    String val = splitValue(line);
                    if (val != null) {
                        String chipName = chipMap.getOrDefault(val, "Unknown");
                        results.add(new ChipGuess("NXP " + chipName, 0.8f));
                    }
                }
                return true;
            });
        }
    }

    private static void detectNxpOppo(List<ChipGuess> results) {
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
                results.add(new ChipGuess(parts[0].toUpperCase(), 0.91f));
            }
        }
    }

    // ─────────────────────────────────────────────
    // 🔧 Utilidades
    // ─────────────────────────────────────────────

    private static List<String> getSearchDirs() {
        return Arrays.asList("/vendor/etc/", "/odm/etc/", "/product/etc/", "/system/etc/", "/etc/");
    }

    private static List<String> searchPaths(String filename) {
        return searchPaths(Collections.singletonList(filename));
    }

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

    private static String findFile(String filename) {
        for (String dir : getSearchDirs()) {
            String path = dir + "nfc/" + filename;
            if (exists(path)) return path;
        }
        return null;
    }

    private static String readFile(String path) {
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            StringBuilder b = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) b.append(line);
            return b.toString().trim();
        } catch (Exception e) {
            return "";
        }
    }

    private static boolean exists(String path) {
        return new File(path).exists();
    }

    private static void readLines(String path, LineHandler handler) {
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!handler.handle(line.trim())) break;
            }
        } catch (Exception ignored) {}
    }

    private static String splitValue(String line) {
        String[] parts = line.split("=", 2);
        return parts.length == 2 ? parts[1].replace("\"", "").trim() : null;
    }

    private interface LineHandler {
        boolean handle(String line);
    }

    private static String getSystemProp(String prop) {
        try {
            Process process = new ProcessBuilder("getprop", prop).start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            return reader.readLine().trim();
        } catch (Exception e) {
            return "";
        }
    }

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
}

package com.esime.nfcdroid2.ui.informacion.componentes;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

// Inspirado en técnicas comunes de obtención de nombre de dispositivos Android
// Adaptado para funcionar con un archivo JSON local propio

public class DeviceNames {
    private final String json;

    public DeviceNames(Context context) {
        this.json = loadJson(context);
    }

    private String loadJson(Context context) {
        try (InputStream is = context.getResources().openRawResource(
                context.getResources().getIdentifier("devices", "raw", context.getPackageName())
        )) {
            return new Scanner(is, StandardCharsets.UTF_8.name()).useDelimiter("\\A").next();
        } catch (Exception e) {
            e.printStackTrace();
            return "[]"; // fallback vacío
        }
    }

    public String formatCurrentDeviceName() {
        DeviceInfo info = getDeviceInfo(Build.DEVICE);
        String model = Build.MODEL;
        String device = Build.DEVICE;

        if (info != null && info.name != null && info.brand != null) {
            return String.format("%s %s (%s) [%s]",
                    capitalize(info.brand),
                    info.name,
                    model,
                    device);
        } else {
            return String.format("%s [%s]", model, device);
        }
    }

    private DeviceInfo getDeviceInfo(String deviceCode) {
        try {
            JSONArray array = new JSONArray(json);
            String targetDevice = deviceCode.trim().toLowerCase();

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String jsonDevice = obj.optString("device", "").trim().toLowerCase();
                String name = obj.optString("name", "").trim();
                String brand = obj.optString("brand", "").trim();

                if (!jsonDevice.isEmpty() && jsonDevice.equals(targetDevice)) {
                    return new DeviceInfo(brand, name);
                }
            }

        } catch (Exception e) {
            Log.e("DeviceNames", "Error parsing JSON", e);
        }

        return null;
    }

    private static class DeviceInfo {
        String brand;
        String name;

        DeviceInfo(String brand, String name) {
            this.brand = brand;
            this.name = name;
        }
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }
}

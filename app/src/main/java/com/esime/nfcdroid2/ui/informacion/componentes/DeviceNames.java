package com.esime.nfcdroid2.ui.informacion.componentes;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

// Clase que permite obtener el nombre de un dispositivo Android desde un archivo JSON local
public class DeviceNames {

    private final JSONArray devicesArray;

    public DeviceNames(Context context) {
        this.devicesArray = cargarJSONArray(context);
    }

    // Carga el archivo JSON como un JSONArray
    private JSONArray cargarJSONArray(Context context) {
        try (@SuppressLint("DiscouragedApi") InputStream is = context.getResources().openRawResource(
                context.getResources().getIdentifier("devices", "raw", context.getPackageName())
        )) {
            String jsonText = new Scanner(is, StandardCharsets.UTF_8.name()).useDelimiter("\\A").next();
            return new JSONArray(jsonText);
        } catch (Exception e) {
            e.printStackTrace();
            return new JSONArray();
        }
    }

    // Busca información del dispositivo en el JSONArray
    private DeviceInfo buscarInfoDispositivo(String deviceCode) {
        try {
            String targetDevice = deviceCode.trim().toLowerCase();

            for (int i = 0; i < devicesArray.length(); i++) {
                JSONObject obj = devicesArray.getJSONObject(i);
                String jsonDevice = obj.optString("device", "").trim().toLowerCase();
                String name = obj.optString("name", "").trim();
                String brand = obj.optString("brand", "").trim();

                if (!jsonDevice.isEmpty() && jsonDevice.equals(targetDevice)) {
                    return new DeviceInfo(brand, name);
                }
            }

        } catch (Exception e) {
            Log.e("DeviceNames", "Error buscando dispositivo", e);
        }

        return null;
    }

    // Devuelve el nombre formateado del dispositivo actual
    public String obtenerNombreDispositivoActual() {
        DeviceInfo info = buscarInfoDispositivo(Build.DEVICE);
        String model = Build.MODEL;
        String device = Build.DEVICE;

        if (info != null && info.name != null && info.brand != null) {
            return String.format("%s %s (%s) [%s]",
                    info.brand,
                    info.name,
                    model,
                    device);
        } else {
            return String.format("%s [%s]", model, device);
        }
    }

    // Clase interna para almacenar marca y nombre del dispositivo
    private static class DeviceInfo {
        String brand;
        String name;

        DeviceInfo(String brand, String name) {
            this.brand = brand;
            this.name = name;
        }
    }

}

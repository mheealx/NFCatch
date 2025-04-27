package com.esime.nfcdroid2.utils;

//Clase que obtiene la versión y el nombre de la app para el Acerca de y el servicio en Segundo plano
public class AppInfo {

    public static String getVersionName() {
        try {
            Class<?> buildConfig = Class.forName("com.mheealx.nfcdroid.BuildConfig");
            return (String) buildConfig.getField("VERSION_NAME").get(null);
        } catch (Exception e) {
            return "unknown";
        }
    }

    public static String getCleanVersionName() {
        return getVersionName().replaceAll("[^a-zA-Z0-9]", "_");
    }

    public static String getDynamicChannelId() {
        return "nfc_event_channel_v" + getCleanVersionName();
    }
}

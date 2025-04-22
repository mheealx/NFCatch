package com.esime.nfcdroid2.utils;

import java.util.ArrayList;
import java.util.List;

public class LogRegistry {
    private static final List<String> logEvents = new ArrayList<>();
    private static LogUpdateListener listener;

    private static String ultimoUid = "";
    private static long ultimoTimestamp = 0;

    public interface LogUpdateListener {
        void onLogUpdated(String newLog);
    }

    public static void add(String line) {
        logEvents.add(line);
        if (listener != null) {
            listener.onLogUpdated(line);
        }
    }

    public static List<String> getLogEvents() {
        return new ArrayList<>(logEvents);
    }

    public static void clear() {
        logEvents.clear();
    }

    public static void setListener(LogUpdateListener l) {
        listener = l;
    }

    public static void removeListener() {
        listener = null;
    }

    // ✅ Prevención de lecturas duplicadas por UID y tiempo
    public static boolean yaFueLeido(String uid) {
        long ahora = System.currentTimeMillis();
        boolean reciente = uid.equals(ultimoUid) && (ahora - ultimoTimestamp < 2000); // 2 segundos
        if (!reciente) {
            ultimoUid = uid;
            ultimoTimestamp = ahora;
        }
        return reciente;
    }
}

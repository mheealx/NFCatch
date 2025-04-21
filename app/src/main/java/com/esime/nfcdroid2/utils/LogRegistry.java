package com.esime.nfcdroid2.utils;

import java.util.ArrayList;
import java.util.List;

public class LogRegistry {
    private static final List<String> logEvents = new ArrayList<>();
    private static LogUpdateListener listener;

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
}

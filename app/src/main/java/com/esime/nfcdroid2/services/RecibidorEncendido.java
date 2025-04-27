package com.esime.nfcdroid2.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

// Receptor que se activa al reiniciar el dispositivo
public class RecibidorEncendido extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d("BootReceiver", "Dispositivo reiniciado");

            // Lee configuración de inicio automático
            SharedPreferences preferences = context.getSharedPreferences("config_preferences", Context.MODE_PRIVATE);
            boolean isAutoStartEnabled = preferences.getBoolean("auto_start_service", true);

            // Iniciar servicio si el inicio automático está habilitado
            if (isAutoStartEnabled) {
                Intent serviceIntent = new Intent(context, ServicioSegundoPlano.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                    Log.d("BootReceiver", "Iniciando app");
                } else {
                    context.startService(serviceIntent);
                }
            } else {
                Log.d("BootReceiver", "Inicio automático desactivado.");
            }
        }
    }
}

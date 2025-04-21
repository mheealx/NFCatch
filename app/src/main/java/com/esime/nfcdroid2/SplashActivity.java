package com.esime.nfcdroid2;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.content.SharedPreferences;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.esime.nfcdroid2.services.NfcBackgroundService;
import com.google.android.material.button.MaterialButton;

public class SplashActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("nfcdroid_prefs", MODE_PRIVATE);
        boolean splashMostrado = prefs.getBoolean("splash_mostrado", false);

        // Si ya se mostró el splash anteriormente, ir directo a MainActivity
        if (splashMostrado) {
            lanzarMainActivity();
            return;
        }

        // Mostrar splash solo la primera vez
        setContentView(R.layout.activity_splash);
        MaterialButton startButton = findViewById(R.id.startButton);

        startButton.setOnClickListener(v -> {
            startButton.setEnabled(false);
            startButton.setText("Verificando permisos…");

            new android.os.Handler().postDelayed(() -> {
                if (!tienePermisosRequeridos()) {
                    solicitarPermisos();
                } else {
                    prefs.edit().putBoolean("splash_mostrado", true).apply(); // 👈 Se guarda aquí
                    lanzarMainActivity();
                }
            }, 600);
        });
    }

    private void lanzarMainActivity() {
        // Iniciar servicio de fondo NFC
        Intent serviceIntent = new Intent(this, com.esime.nfcdroid2.services.NfcBackgroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        // Abrir MainActivity y cerrar splash
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }





    private boolean tienePermisosRequeridos() {
        boolean postNotifications = true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            postNotifications = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }

        return postNotifications;
    }

    private void solicitarPermisos() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        continuar(); // Continúa aunque los permisos sean negados
    }

    private void continuar() {
        // 🔹 Inicia el servicio en segundo plano
        Intent serviceIntent = new Intent(this, NfcBackgroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        // 🔹 Lanza la actividad principal
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void guardarYContinuar(SharedPreferences prefs) {
        prefs.edit().putBoolean("splash_mostrado", true).apply();

        Intent serviceIntent = new Intent(this, com.esime.nfcdroid2.services.NfcBackgroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

}

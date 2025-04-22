package com.esime.nfcdroid2;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.esime.nfcdroid2.services.ServicioSegundoPlano;
import com.google.android.material.button.MaterialButton;

public class SplashActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS = 101;
    private SharedPreferences prefs;

    private final ActivityResultLauncher<Intent> batteryPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                // Cuando el usuario vuelve de la pantalla de optimización
                lanzarMainActivity();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("nfcdroid_prefs", MODE_PRIVATE);
        boolean splashMostrado = prefs.getBoolean("splash_mostrado", false);

        if (splashMostrado) {
            verificarExclusionDeBateria();
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
                    prefs.edit().putBoolean("splash_mostrado", true).apply();
                    verificarExclusionDeBateria();
                }
            }, 600);
        });
    }

    private void verificarExclusionDeBateria() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            String paquete = getPackageName();

            if (!pm.isIgnoringBatteryOptimizations(paquete)) {
                mostrarDialogoExplicacion();
                return;
            }
        }

        // Si ya está excluida o no es necesario, lanzamos MainActivity
        lanzarMainActivity();
    }

    private void mostrarDialogoExplicacion() {
        new AlertDialog.Builder(this)
                .setTitle("Permiso de batería")
                .setMessage("Para que NFCDroid funcione correctamente en segundo plano y tras reiniciar el dispositivo, necesitamos que excluyas la app de las optimizaciones de batería.")
                .setCancelable(false)
                .setPositiveButton("Aceptar", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    batteryPermissionLauncher.launch(intent);
                })
                .setNegativeButton("Cancelar", (dialog, which) -> {
                    // Usuario canceló, aún así puede continuar
                    lanzarMainActivity();
                })
                .show();
    }

    private void lanzarMainActivity() {
        // Inicia el servicio en segundo plano
        Intent serviceIntent = new Intent(this, ServicioSegundoPlano.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        // Ir a MainActivity
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
        prefs.edit().putBoolean("splash_mostrado", true).apply();
        verificarExclusionDeBateria();
    }
}

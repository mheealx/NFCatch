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
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.esime.nfcdroid2.services.ServicioSegundoPlano;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class SplashActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS = 101;
    private SharedPreferences prefs;
    private List<String> permisosNecesarios;

    private final ActivityResultLauncher<Intent> batteryPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
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

        // Mostrar layout inicial
        setContentView(R.layout.actividad_splash_inicial);
        MaterialButton startButton = findViewById(R.id.startButton);

        startButton.setOnClickListener(v -> {
            startButton.setEnabled(false);
            startButton.setText("Verificando permisos…");

            new android.os.Handler().postDelayed(() -> {
                if (!tienePermisosRequeridos()) {
                    mostrarExplicacionPermisos();
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
                mostrarDialogoExplicacionBateria();
                return;
            }
        }

        lanzarMainActivity();
    }

    private void mostrarDialogoExplicacionBateria() {
        new AlertDialog.Builder(this)
                .setTitle("Permiso de batería")
                .setMessage("Para que NFCDroid funcione correctamente en segundo plano y tras reiniciar el dispositivo, necesitamos que excluyas la app de las optimizaciones de batería.")
                .setCancelable(false)
                .setPositiveButton("Aceptar", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    batteryPermissionLauncher.launch(intent);
                })
                .setNegativeButton("Cancelar", (dialog, which) -> lanzarMainActivity())
                .show();
    }

    private void lanzarMainActivity() {
        Intent serviceIntent = new Intent(this, ServicioSegundoPlano.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private boolean tienePermisosRequeridos() {
        permisosNecesarios = new ArrayList<>();

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permisosNecesarios.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permisosNecesarios.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permisosNecesarios.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        return permisosNecesarios.isEmpty();
    }

    private void mostrarExplicacionPermisos() {
        new AlertDialog.Builder(this)
                .setTitle("Permisos necesarios")
                .setMessage("NFCDroid necesita permisos de almacenamiento para respaldar y restaurar configuraciones, y permiso de notificaciones para avisos importantes.\n\n¿Deseas otorgarlos ahora?")
                .setCancelable(false)
                .setPositiveButton("Aceptar", (dialog, which) -> solicitarPermisos())
                .setNegativeButton("Cancelar", (dialog, which) -> {
                    // Si cancela, igual seguimos pero avisamos
                    Toast.makeText(this, "Algunos permisos no fueron otorgados", Toast.LENGTH_LONG).show();
                    prefs.edit().putBoolean("splash_mostrado", true).apply();
                    verificarExclusionDeBateria();
                })
                .show();
    }

    private void solicitarPermisos() {
        if (permisosNecesarios != null && !permisosNecesarios.isEmpty()) {
            ActivityCompat.requestPermissions(this, permisosNecesarios.toArray(new String[0]), REQUEST_PERMISSIONS);
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

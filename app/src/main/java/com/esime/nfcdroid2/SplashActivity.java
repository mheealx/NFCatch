package com.esime.nfcdroid2;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.esime.nfcdroid2.services.ServicioSegundoPlano;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

// Pantalla inicial que solicita permisos de Notificaciones y Almacenamiento (Android 8 e inferior)
public class SplashActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS = 101;
    private SharedPreferences prefs;
    private List<String> permisosNecesarios;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("nfcdroid_prefs", MODE_PRIVATE);
        boolean splashMostrado = prefs.getBoolean("splash_mostrado", false);

        if (splashMostrado) {
            lanzarMainActivity();
            return;
        }

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
                    lanzarMainActivity();
                }
            }, 600);
        });
    }

    // Inicia MainActivity después de obtenidos los permisos
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

    // Verifica si los permisos requeridos ya fueron otorgados
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

    // Pantalla de solicitud de permisos explicada
    private void mostrarExplicacionPermisos() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Permisos requeridos")
                .setMessage("Se requiere tu permiso a notificaciones para alertar por un evento NFC y tu permiso a almacenamiento (Android 8 e inferior) para poder guardar los logs.\n\n¿Deseas otorgarlos ahora?")
                .setCancelable(false)
                .setPositiveButton("Aceptar", (dialog, which) -> solicitarPermisos())
                .setNegativeButton("Cancelar", (dialog, which) -> {
                    Toast.makeText(this, "Algunos permisos no fueron otorgados", Toast.LENGTH_LONG).show();
                    prefs.edit().putBoolean("splash_mostrado", true).apply();
                    lanzarMainActivity();
                })
                .show();
    }

    // Solicita los permisos necesarios
    private void solicitarPermisos() {
        if (permisosNecesarios != null && !permisosNecesarios.isEmpty()) {
            ActivityCompat.requestPermissions(this, permisosNecesarios.toArray(new String[0]), REQUEST_PERMISSIONS);
        }
    }

    // Maneja el resultado de la solicitud de permisos
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        prefs.edit().putBoolean("splash_mostrado", true).apply();
        lanzarMainActivity();
    }
}

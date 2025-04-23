package com.esime.nfcdroid2.services;

import static com.esime.nfcdroid2.utils.NfcIsoDepHelper.bytesToHex;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.esime.nfcdroid2.MainActivity;
import com.esime.nfcdroid2.R;
import com.esime.nfcdroid2.utils.*;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class ServicioSegundoPlano extends Service {

    private static final String CHANNEL_ID = "nfc_background_channel";
    private static final String NFC_EVENT_CHANNEL_ID = "nfc_event_channel";
    private static final String TAG = "NFC_SERVICE";
    private static final String ACTION_HANDLE_TAG = "com.esime.nfcdroid2.ACTION_HANDLE_TAG";

    private BroadcastReceiver screenReceiver;
    private boolean pantallaEncendida = true;

    private static boolean ultimaFueLecturaNfc = false;
    private static long ultimaLecturaTimestamp = 0;

    public static void marcarLecturaNfc() {
        ultimaFueLecturaNfc = true;
        ultimaLecturaTimestamp = System.currentTimeMillis();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        crearCanalNotificacion();

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("NFCDroid activo")
                .setContentText("Escuchando etiquetas NFC en segundo plano")
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();

        startForeground(1, notification);

        screenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long ahora = System.currentTimeMillis();
                if (ultimaFueLecturaNfc && (ahora - ultimaLecturaTimestamp) < 1000) {
                    ultimaFueLecturaNfc = false;
                    return;
                }

                NfcAdapter adapter = NfcAdapter.getDefaultAdapter(context);
                boolean isEnabled = adapter != null && adapter.isEnabled();
                String estadoNfc = isEnabled ? "NFC ACTIVO" : "NFC DESACTIVADO";

                if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                    if (!pantallaEncendida) {
                        pantallaEncendida = true;
                        registrarEvento("Pantalla ENCENDIDA", estadoNfc);
                    }
                } else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                    if (pantallaEncendida) {
                        pantallaEncendida = false;
                        registrarEvento("Pantalla APAGADA", estadoNfc);
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenReceiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);

        if (adapter == null) {
            Log.w(TAG, "Dispositivo sin NFC. Deteniendo servicio y evitando reinicio automático.");
            stopSelf(); // Detenemos el servicio de inmediato
            return START_NOT_STICKY; // El sistema NO reiniciará el servicio automáticamente
        }

        if (intent != null && ACTION_HANDLE_TAG.equals(intent.getAction())) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag != null) {
                procesarTag(tag);
            }
        }

        return START_STICKY; // Si hay NFC, se comporta normalmente (se reinicia si es necesario)
    }


    private void procesarTag(Tag tag) {
        marcarLecturaNfc();
        List<String> logs = new ArrayList<>();

        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(new Date());
        int pid = android.os.Process.myPid();
        int tid = android.os.Process.myTid();
        String pkg = getPackageName();

        logs.add(formatoLog(timestamp, pid, tid, TAG, pkg, "D", "Tag detectado"));
        logs.add(formatoLog(timestamp, pid, tid, TAG, pkg, "D", "UID: " + bytesToHex(tag.getId())));
        logs.add(formatoLog(timestamp, pid, tid, TAG, pkg, "D", "Techs: " + Arrays.toString(tag.getTechList())));

        LogCallback logger = (level, t, msg) -> logs.add(formatoLog(timestamp, pid, tid, t, pkg, level, msg));

        for (String tech : tag.getTechList()) {
            switch (tech) {
                case "android.nfc.tech.Ndef": NfcNdefHelper.read(tag, logger); break;
                case "android.nfc.tech.NdefFormatable": NfcNdefFormatableHelper.read(tag, logger); break;
                case "android.nfc.tech.MifareClassic": NfcMifareClassicHelper.read(tag, logger); break;
                case "android.nfc.tech.MifareUltralight": NfcMifareUltralightHelper.read(tag, logger); break;
                case "android.nfc.tech.NfcA": NfcAHelper.read(tag, logger); break;
                case "android.nfc.tech.NfcB": NfcBHelper.read(tag, logger); break;
                case "android.nfc.tech.NfcV": NfcVHelper.read(tag, logger); break;
                case "android.nfc.tech.IsoDep": NfcIsoDepHelper.read(tag, logger); break;
            }
        }

        StringBuilder contenido = new StringBuilder();
        for (String log : logs) {
            LogRegistry.add(log);
            contenido.append(log).append("\n");
        }

        guardarLog(contenido.toString());
        enviarNotificacion();
    }

    private String formatoLog(String timestamp, int pid, int tid, String tag, String pkg, String lvl, String msg) {
        return String.format(Locale.getDefault(), "%s %5d-%5d %-25s %-35s %-1s  %s",
                timestamp, pid, tid, tag, pkg, lvl, msg);
    }

    private void guardarLog(String contenido) {
        try {
            File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "NFCDroid");
            if (!dir.exists()) dir.mkdirs();

            String nombre = "log_nfc_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".txt";
            File archivo = new File(dir, nombre);

            try (FileOutputStream fos = new FileOutputStream(archivo)) {
                fos.write(contenido.getBytes());
            }

            Log.i(TAG, "Archivo guardado: " + archivo.getAbsolutePath());

        } catch (Exception e) {
            Log.e(TAG, "Error al guardar log NFC: " + e.getMessage());
        }
    }

    private void enviarNotificacion() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("abrir_fragmento", "historial");

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification noti = new NotificationCompat.Builder(this, NFC_EVENT_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("HUBO UN EVENTO NFC")
                .setContentText("Revisa el log en el historial de la app")
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_SOUND | NotificationCompat.DEFAULT_VIBRATE)
                .build();

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(2, noti);
    }

    private void registrarEvento(String estadoPantalla, String estadoNfc) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(new Date());
        int pid = android.os.Process.myPid();
        int tid = android.os.Process.myTid();
        String pkg = getPackageName();
        String log = formatoLog(timestamp, pid, tid, TAG, pkg, "I", "[" + estadoNfc + "] " + estadoPantalla);
        Log.i(TAG, log);
        LogRegistry.add(log);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
        if (screenReceiver != null) unregisterReceiver(screenReceiver);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);

            // Canal silencioso para servicio en segundo plano
            NotificationChannel canalSilencioso = new NotificationChannel(
                    CHANNEL_ID,
                    "Servicio NFC en segundo plano",
                    NotificationManager.IMPORTANCE_LOW
            );
            canalSilencioso.setDescription("NFCDroid registra actividad del chip NFC");
            canalSilencioso.setShowBadge(false);
            canalSilencioso.setSound(null, null);
            canalSilencioso.enableVibration(false);
            manager.createNotificationChannel(canalSilencioso);

            // Canal con sonido para eventos NFC
            NotificationChannel canalSonido = new NotificationChannel(
                    NFC_EVENT_CHANNEL_ID,
                    "Eventos NFC detectados",
                    NotificationManager.IMPORTANCE_HIGH
            );
            canalSonido.setDescription("Notificaciones con sonido cuando se detectan etiquetas NFC");
            canalSonido.enableVibration(true);

            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            canalSonido.setSound(Settings.System.DEFAULT_NOTIFICATION_URI, attrs);
            canalSonido.setShowBadge(true);

            manager.createNotificationChannel(canalSonido);
        }
    }
}

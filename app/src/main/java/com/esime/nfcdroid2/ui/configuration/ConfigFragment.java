package com.esime.nfcdroid2.ui.configuration;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TimePicker;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.esime.nfcdroid2.databinding.FragmentConfigBinding;

import java.util.Calendar;

public class ConfigFragment extends Fragment {

    private FragmentConfigBinding binding;
    private SharedPreferences preferences;
    private ActivityResultLauncher<Intent> ringtonePickerLauncher;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentConfigBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        preferences = getActivity().getSharedPreferences("config_preferences", Context.MODE_PRIVATE);

        // Inicializar el launcher del picker de tonos
        ringtonePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                        if (uri != null) {
                            preferences.edit().putString("custom_sound_uri", uri.toString()).apply();
                            Toast.makeText(getContext(), "Tono personalizado guardado", Toast.LENGTH_SHORT).show();
                            recrearCanalSonidoPersonalizado();
                        }
                    }
                }
        );

        // Switch de inicio automático
        Switch inicioAutoSwitch = binding.inicioautoSwitch;
        inicioAutoSwitch.setChecked(preferences.getBoolean("auto_start_service", true));
        inicioAutoSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean("auto_start_service", isChecked).apply();
            Toast.makeText(getContext(),
                    isChecked ? "Inicio automático activado" : "Inicio automático desactivado",
                    Toast.LENGTH_SHORT).show();
        });

        // Switch de silencio programado
        Switch scheduledSilenceSwitch = binding.scheduledSilenceSwitch;
        scheduledSilenceSwitch.setChecked(preferences.getBoolean("scheduled_silence_enabled", false));
        scheduledSilenceSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean("scheduled_silence_enabled", isChecked).apply();
            Toast.makeText(getContext(),
                    isChecked ? "Silencio programado activado" : "Silencio programado desactivado",
                    Toast.LENGTH_SHORT).show();
        });

        // Botones de hora de inicio/fin
        Button silenceStartButton = binding.silenceStartButton;
        Button silenceEndButton = binding.silenceEndButton;
        int startHour = preferences.getInt("silence_start_hour", 22);
        int startMinute = preferences.getInt("silence_start_minute", 0);
        int endHour = preferences.getInt("silence_end_hour", 6);
        int endMinute = preferences.getInt("silence_end_minute", 0);

        silenceStartButton.setText(String.format("Inicio: %02d:%02d", startHour, startMinute));
        silenceEndButton.setText(String.format("Fin: %02d:%02d", endHour, endMinute));

        silenceStartButton.setOnClickListener(v -> mostrarSelectorHora(true, silenceStartButton));
        silenceEndButton.setOnClickListener(v -> mostrarSelectorHora(false, silenceEndButton));

        // Botones y TextViews de sonido
        Button selectSoundModeButton = binding.selectSoundModeButton;
        Button playSoundButton = binding.playSoundButton;
        TextView notaAudioTextView = binding.notaAudio;
        TextView currentModeTextView = binding.currentModeTextView;

        actualizarVisibilidadBotonSonido(playSoundButton, notaAudioTextView);
        actualizarTextoModoActual(currentModeTextView);

        selectSoundModeButton.setOnClickListener(v -> mostrarDialogoSeleccionModo(playSoundButton, notaAudioTextView, currentModeTextView));
        playSoundButton.setOnClickListener(v -> seleccionarSonidoPersonalizado());

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void mostrarSelectorHora(boolean esInicio, Button targetButton) {
        Calendar calendario = Calendar.getInstance();
        int horaActual = calendario.get(Calendar.HOUR_OF_DAY);
        int minutoActual = calendario.get(Calendar.MINUTE);

        TimePickerDialog picker = new TimePickerDialog(getContext(), (TimePicker view, int hourOfDay, int minute) -> {
            SharedPreferences.Editor editor = preferences.edit();
            if (esInicio) {
                editor.putInt("silence_start_hour", hourOfDay);
                editor.putInt("silence_start_minute", minute);
            } else {
                editor.putInt("silence_end_hour", hourOfDay);
                editor.putInt("silence_end_minute", minute);
            }
            editor.apply();

            String formato = esInicio ? "Inicio: %02d:%02d" : "Fin: %02d:%02d";
            targetButton.setText(String.format(formato, hourOfDay, minute));

            Toast.makeText(getContext(),
                    (esInicio ? "Hora de inicio" : "Hora de término") +
                            " establecida: " + String.format("%02d:%02d", hourOfDay, minute),
                    Toast.LENGTH_SHORT).show();
        }, horaActual, minutoActual, true);

        picker.show();
    }

    private void mostrarDialogoSeleccionModo(Button playSoundButton, TextView notaAudioTextView, TextView currentModeTextView) {
        final String[] modos = {"Silencioso", "Predeterminado", "Personalizado"};
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Seleccionar modo de notificación");
        builder.setItems(modos, (dialog, which) -> {
            SharedPreferences.Editor editor = preferences.edit();
            switch (which) {
                case 0:
                    editor.putString("notification_mode", "silencio");
                    Toast.makeText(getContext(), "Modo silencioso activado", Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    editor.putString("notification_mode", "predeterminado");
                    Toast.makeText(getContext(), "Modo predeterminado activado", Toast.LENGTH_SHORT).show();
                    break;
                case 2:
                    editor.putString("notification_mode", "personalizado");
                    Toast.makeText(getContext(), "Modo personalizado activado", Toast.LENGTH_SHORT).show();
                    break;
            }
            editor.apply();
            actualizarVisibilidadBotonSonido(playSoundButton, notaAudioTextView);
            actualizarTextoModoActual(currentModeTextView);
        });
        builder.show();
    }

    private void actualizarVisibilidadBotonSonido(Button playSoundButton, TextView notaAudioTextView) {
        String modo = preferences.getString("notification_mode", "predeterminado");
        boolean esPersonalizado = "personalizado".equals(modo);

        playSoundButton.setVisibility(esPersonalizado ? View.VISIBLE : View.GONE);
        notaAudioTextView.setVisibility(esPersonalizado ? View.VISIBLE : View.GONE);
    }

    private void actualizarTextoModoActual(TextView currentModeTextView) {
        String modo = preferences.getString("notification_mode", "predeterminado");
        String texto;

        switch (modo) {
            case "silencio":
                texto = "Modo actual: Silencioso";
                break;
            case "personalizado":
                texto = "Modo actual: Personalizado";
                break;
            default:
                texto = "Modo actual: Predeterminado";
                break;
        }

        currentModeTextView.setText(texto);
    }

    private void seleccionarSonidoPersonalizado() {
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Seleccionar tono de notificación");
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) null);
        ringtonePickerLauncher.launch(intent);
    }

    private void recrearCanalSonidoPersonalizado() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);

            if (manager.getNotificationChannel("nfc_custom_sound_channel") != null) {
                manager.deleteNotificationChannel("nfc_custom_sound_channel");
            }

            String uriStr = preferences.getString("custom_sound_uri", null);
            if (uriStr == null) return;

            Uri sonidoUri = Uri.parse(uriStr);

            NotificationChannel canal = new NotificationChannel(
                    "nfc_custom_sound_channel",
                    "Canal Personalizado",
                    NotificationManager.IMPORTANCE_HIGH
            );
            canal.setDescription("Canal con sonido personalizado elegido por el usuario");

            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            canal.setSound(sonidoUri, attrs);
            canal.enableVibration(true);

            manager.createNotificationChannel(canal);
        }
    }
}

package com.esime.nfcdroid2.ui.acercade;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.esime.nfcdroid2.R;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class LicenseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_licencia);

        // Hacer un TextView para mostrar la licencia
        TextView licenseTextView = findViewById(R.id.licenseTextView);

        // Cargar el archivo de licencia
        try {
            InputStream inputStream = getResources().openRawResource(R.raw.licencia);  // Ruta del archivo en raw
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            inputStream.close();

            // Mostrar licencia en el TextView
            licenseTextView.setText(stringBuilder.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

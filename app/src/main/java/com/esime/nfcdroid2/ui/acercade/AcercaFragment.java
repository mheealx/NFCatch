package com.esime.nfcdroid2.ui.acercade;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.esime.nfcdroid2.R;

//Pantalla de Acerca del proyecto
public class AcercaFragment extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_acercade, container, false);

        configurarBotones(root);
        updateAppInfo(root);

        return root;
    }

    // Configura las acciones de los botones
    private void configurarBotones(View root) {
        //Botón licencia
        Button licenseButton = root.findViewById(R.id.licenseButton);
        licenseButton.setOnClickListener(v -> openLicenseActivity());
        //Botón ESIME CU
        Button link1Button = root.findViewById(R.id.link1);
        link1Button.setOnClickListener(v -> openLink("https://www.esimecu.ipn.mx/"));
        //Botón CISEG-CIC
        Button link2Button = root.findViewById(R.id.link2);
        link2Button.setOnClickListener(v -> openLink("https://www.ciseg.cic.ipn.mx/"));
    }

    // Obtención del nombre y versión de la aplicación
    private void updateAppInfo(View root) {
        TextView appNameTextView = root.findViewById(R.id.appName);
        TextView appVersionTextView = root.findViewById(R.id.appVersion);

        try {
            PackageManager packageManager = getActivity().getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(getActivity().getPackageName(), 0);

            String appName = getActivity().getApplicationInfo().loadLabel(packageManager).toString();
            String appVersion = packageInfo.versionName;

            appNameTextView.setText(appName);
            appVersionTextView.setText("Versión " + appVersion);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    // Abre la actividad que muestra la licencia
    private void openLicenseActivity() {
        Intent intent = new Intent(getActivity(), LicenseActivity.class);
        startActivity(intent);
    }

    // Abre un enlace en el navegador
    private void openLink(String url) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(browserIntent);
    }
}

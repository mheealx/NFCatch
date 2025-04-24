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

public class AcercaFragment extends Fragment {

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_acercade, container, false);

        // Botón de Licencias
        Button licenseButton = root.findViewById(R.id.licenseButton);
        licenseButton.setOnClickListener(v -> openLicenseActivity());

        // Botón ESIME CULHUACAN
        Button link1Button = root.findViewById(R.id.link1);
        link1Button.setOnClickListener(v -> openLink("https://www.esimecu.ipn.mx/"));

        // Botón CIC
        Button link2Button = root.findViewById(R.id.link2);
        link2Button.setOnClickListener(v -> openLink("https://www.ciseg.cic.ipn.mx/"));

        // Actualizar el nombre de la app y la versión
        updateAppInfo(root);

        return root;
    }

    private void updateAppInfo(View root) {
        TextView appNameTextView = root.findViewById(R.id.appName);
        TextView appVersionTextView = root.findViewById(R.id.appVersion);

        try {
            // Obtener el nombre de la aplicación y la versión
            PackageManager packageManager = getActivity().getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(getActivity().getPackageName(), 0);

            // Obtener el nombre de la aplicación desde los recursos
            String appName = getActivity().getApplicationInfo().loadLabel(packageManager).toString();
            // Obtener la versión desde el packageInfo
            String appVersion = packageInfo.versionName;

            // Establecer el nombre y la versión en los TextViews
            appNameTextView.setText(appName);
            appVersionTextView.setText("Versión " + appVersion);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    // Método para abrir la actividad de la licencia
    private void openLicenseActivity() {
        Intent intent = new Intent(getActivity(), LicenseActivity.class);
        startActivity(intent);
    }

    // Método para abrir un enlace en el navegador
    private void openLink(String url) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(browserIntent);
    }
}

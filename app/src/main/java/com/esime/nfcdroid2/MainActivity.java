package com.esime.nfcdroid2;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.esime.nfcdroid2.services.ServicioSegundoPlano;
import com.esime.nfcdroid2.ui.home.HomeFragment;
import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.esime.nfcdroid2.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow, R.id.nav_informacion, R.id.nav_acerca_de)
                .setOpenableLayout(drawer)
                .build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // Ocultar titulos personalizados, mostrar título estático en Header
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() == R.id.nav_gallery || destination.getId() == R.id.nav_home || destination.getId() == R.id.nav_slideshow || destination.getId() == R.id.nav_informacion || destination.getId() == R.id.nav_acerca_de) {
                getSupportActionBar().setTitle("");
            }
        });

        // Asignar el listener para manejar los ítems del menú
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    //Lanza HomeFragment (consola) como principal
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_content_main);

        if (navHostFragment != null) {
            Fragment currentFragment = navHostFragment.getChildFragmentManager()
                    .getPrimaryNavigationFragment();

            if (currentFragment instanceof HomeFragment) {
                ((HomeFragment) currentFragment).handleNfcIntent(intent);
            }
        }
    }

    //Botón de salir de la aplicación
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.salir) {
            Intent serviceIntent = new Intent(this, ServicioSegundoPlano.class); //Inicia el servicio de segundo plano
            ContextCompat.startForegroundService(this, serviceIntent);
            finish(); // Cierre de la app
        } else {
            NavigationUI.onNavDestinationSelected(item, Navigation.findNavController(this, R.id.nav_host_fragment_content_main)); //Función de los demás botones
        }
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer != null) {
            drawer.closeDrawer(GravityCompat.START);
        }

        return true;
    }

}

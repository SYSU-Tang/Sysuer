package com.sysu.edu;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.NavInflater;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;

import com.google.android.material.navigation.NavigationBarView;
import com.sysu.edu.databinding.ActivityMainBinding;
import com.sysu.edu.preference.Language;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
//        new AppBarConfiguration.Builder(
//                R.id.navigation_activity, R.id.navigation_service, R.id.navigation_account)
//                .build();
        NavController navController = ((NavHostFragment) Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_activity_main))).getNavController();
        //getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        NavGraph g = new NavInflater(this,navController.getNavigatorProvider()).inflate(R.navigation.main_navigation);
        if(savedInstanceState==null){
            g.setStartDestination(new int[]{R.id.navigation_activity,R.id.navigation_service,R.id.navigation_account}[Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString("home","0"))]);
        }
        navController.setGraph(g);
        NavigationUI.setupWithNavController((NavigationBarView) binding.navView, navController);
        Language.setLanguage(this);
        //new Theme(this).setTheme();
    }
}
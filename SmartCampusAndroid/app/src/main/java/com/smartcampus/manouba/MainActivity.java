package com.smartcampus.manouba;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.smartcampus.manouba.activities.AuthActivity;
import com.smartcampus.manouba.utils.SharedPrefManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
            NavHostFragment navHostFragment = (NavHostFragment)
                    getSupportFragmentManager().findFragmentById(R.id.main_nav_host);

            if (navHostFragment != null && bottomNav != null) {
                NavController navController = navHostFragment.getNavController();
                NavigationUI.setupWithNavController(bottomNav, navController);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void logout() {
        SharedPrefManager.getInstance(this).logout();
        Intent intent = new Intent(this, AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

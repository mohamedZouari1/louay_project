package com.smartcampus.manouba.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.fragment.NavHostFragment;
import com.smartcampus.manouba.MainActivity;
import com.smartcampus.manouba.R;
import com.smartcampus.manouba.utils.SharedPrefManager;

public class AuthActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPrefManager prefManager = SharedPrefManager.getInstance(this);

        // Skip to main if already logged in
        if (prefManager.isLoggedIn()) {
            navigateToMain();
            return;
        }

        setContentView(R.layout.activity_auth);

        // Set up navigation graph programmatically
        NavHostFragment navHostFragment = (NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.auth_nav_host);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            NavGraph navGraph = navController.getNavInflater().inflate(R.navigation.auth_nav_graph);

            // Skip onboarding if already completed
            if (!prefManager.isFirstLaunch()) {
                navGraph.setStartDestination(R.id.loginFragment);
            }

            navController.setGraph(navGraph);
        }
    }

    public void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

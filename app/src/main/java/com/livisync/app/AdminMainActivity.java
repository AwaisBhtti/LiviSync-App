package com.livisync.app;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class AdminMainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_main);

        bottomNav = findViewById(R.id.adminBottomNavigationView);

        // Set default fragment
        loadFragment(new AdminUsersFragment());

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();

            if (id == R.id.admin_users) {
                selectedFragment = new AdminUsersFragment();
            } else if (id == R.id.admin_matches) {
                selectedFragment = new AdminMatchesFragment();
            } else if (id == R.id.admin_reports) {
                selectedFragment = new AdminReportsFragment();
            } else if (id == R.id.admin_profile) {
                selectedFragment = new AdminProfileFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
            }
            return true;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.adminFragmentContainer, fragment)
                .commit();
    }
}
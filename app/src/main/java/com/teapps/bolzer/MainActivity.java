package com.teapps.bolzer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.teapps.bolzer.fragments.MapFragment;
import com.teapps.bolzer.fragments.MyBolzersFragment;
import com.teapps.bolzer.fragments.BolzerTicketsFragment;
import com.teapps.bolzer.fragments.StatisticFragment;
import com.teapps.bolzer.logreg.LogRegActivity;

import java.util.Calendar;

import static com.teapps.bolzer.helper.Constants.INTERVALL;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, BottomNavigationView.OnNavigationItemSelectedListener {

    private FirebaseFirestore database;
    private FirebaseAuth firebaseAuth;

    private ActionBar actionBar;
    private ActionBarDrawerToggle drawerToggle;
    private DrawerLayout drawerLayout;

    private Fragment mapFragment = new MapFragment();
    private Fragment myBolzersFragment = new MyBolzersFragment();
    private Fragment otherBolzersFragment = new BolzerTicketsFragment();
    private Fragment statisticFragment = new StatisticFragment();
    private FragmentManager fm = getSupportFragmentManager();
    private Fragment active = mapFragment;

    private FloatingActionButton fab;

    private BottomNavigationView bnv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        actionBar = getSupportActionBar();

        showPermissionDialog();
        initializeFab();
        initFragments();
        initializeFirebase();
        initializeNavDrawer();

    }

    private void initFragments() {
        fm.beginTransaction().add(R.id.main_container, statisticFragment, "4").hide(statisticFragment).commit();
        fm.beginTransaction().add(R.id.main_container, otherBolzersFragment, "3").hide(otherBolzersFragment).commit();
        fm.beginTransaction().add(R.id.main_container, myBolzersFragment, "2").hide(myBolzersFragment).commit();
        fm.beginTransaction().add(R.id.main_container, mapFragment, "1").commit();
    }

    private void initializeFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
        database = FirebaseFirestore.getInstance();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initializeNavDrawer() {
        bnv = findViewById(R.id.navigation);
        bnv.setOnNavigationItemSelectedListener(this);
        drawerLayout = findViewById(R.id.drawer_layout);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setItemTextColor(ColorStateList.valueOf(Color.BLACK));
        navigationView.setNavigationItemSelectedListener(this);
    }

    @SuppressLint("RestrictedApi")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.nav_proile:
                return true;

            case R.id.nav_logout:
                firebaseAuth.signOut();
                startActivity(new Intent(MainActivity.this, LogRegActivity.class));
                return true;

            case R.id.action_map:
                fm.beginTransaction().hide(active).show(mapFragment).commit();
                active = mapFragment;
                actionBar.setTitle("Bolzer");
                fab.setVisibility(View.VISIBLE);
                return true;

            case R.id.action_my_bolzer:
                fm.beginTransaction().hide(active).show(myBolzersFragment).commit();
                active = myBolzersFragment;
                actionBar.setTitle("Meine Bolzer");
                fab.setVisibility(View.VISIBLE);
                return true;

            case R.id.action_other_bolzer:
                fm.beginTransaction().hide(active).show(otherBolzersFragment).commit();
                active = otherBolzersFragment;
                actionBar.setTitle("Bolzer Tickets");
                fab.setVisibility(View.INVISIBLE);
                return true;

            case R.id.action_statistics:
                fm.beginTransaction().hide(active).show(statisticFragment).commit();
                active = statisticFragment;
                actionBar.setTitle("Punkte");
                fab.setVisibility(View.INVISIBLE);
                return true;
        }
        return false;
    }

    private void initializeFab() {
        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, NewBolzerActivity.class);
                startActivity(intent);
            }
        });
    }

    private void showPermissionDialog() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION
                                , Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.INTERNET
                                , Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE
                                , Manifest.permission.RECEIVE_BOOT_COMPLETED}
                                , 1240);
        } else {
            // Permission has already been granted
        }
    }

}

package com.teapps.bolzer.logreg;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.TabLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.teapps.bolzer.MainActivity;
import com.teapps.bolzer.R;

public class LogRegActivity extends AppCompatActivity {

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    private FirebaseAuth firebaseAuth;

    private Button startScreenLogin, startScreenRegister;
    private TextView btnTabLogin, btnTabRegister;

    private ConstraintLayout layoutStartView, layoutContainerView;

    private ImageView closeLogReg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_reg);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        firebaseAuth = FirebaseAuth.getInstance();
        initializeObjects();
        setOnClickEvents();
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {

            }

            @Override
            public void onPageSelected(int i) {
                switch (i) {
                    case 0:
                        btnTabLogin.setTextColor(Color.parseColor("#ffffff"));
                        btnTabRegister.setTextColor(Color.parseColor("#666666"));
                        break;
                    case 1:
                        btnTabLogin.setTextColor(Color.parseColor("#666666"));
                        btnTabRegister.setTextColor(Color.parseColor("#ffffff"));
                        break;
                }
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });

    }

    private void setOnClickEvents() {
        startScreenLogin.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("ResourceType")
            @Override
            public void onClick(View v) {
                layoutStartView.setVisibility(View.INVISIBLE);
                layoutContainerView.setVisibility(View.VISIBLE);
                mViewPager.setCurrentItem(0);
                btnTabLogin.setTextColor(Color.parseColor("#ffffff"));
                btnTabRegister.setTextColor(Color.parseColor("#666666"));
            }
        });
        startScreenRegister.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("ResourceType")
            @Override
            public void onClick(View v) {
                layoutStartView.setVisibility(View.INVISIBLE);
                layoutContainerView.setVisibility(View.VISIBLE);
                mViewPager.setCurrentItem(1);
                btnTabLogin.setTextColor(Color.parseColor("#666666"));
                btnTabRegister.setTextColor(Color.parseColor("#ffffff"));
            }
        });
        btnTabLogin.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("ResourceType")
            @Override
            public void onClick(View v) {
                mViewPager.setCurrentItem(0);
                btnTabLogin.setTextColor(Color.parseColor("#ffffff"));
                btnTabRegister.setTextColor(Color.parseColor("#666666"));
            }
        });
        btnTabRegister.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("ResourceType")
            @Override
            public void onClick(View v) {
                mViewPager.setCurrentItem(1);
                btnTabLogin.setTextColor(Color.parseColor("#666666"));
                btnTabRegister.setTextColor(Color.parseColor("#ffffff"));
            }
        });

        closeLogReg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layoutStartView.setVisibility(View.VISIBLE);
                layoutContainerView.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void initializeObjects() {
        startScreenLogin = findViewById(R.id.btnStartLogin);
        startScreenRegister = findViewById(R.id.btnStartRegister);

        btnTabLogin = findViewById(R.id.btnTabLogin);
        btnTabRegister = findViewById(R.id.btnTabRegister);

        layoutStartView = findViewById(R.id.logregStartScreen);
        layoutContainerView = findViewById(R.id.logregViewScreen);

        closeLogReg = findViewById(R.id.closeLogReg);
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            RegisterFragment registerFragment = new RegisterFragment();
            LoginFragment loginFragment = new LoginFragment();
            switch (position) {
                case 0:
                    return loginFragment;
                case 1:
                    return registerFragment;
            }
            return null;
        }

        @Override
        public int getCount() {
            return 2;
        }
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        if (fragment instanceof LoginFragment) {
            LoginFragment logFrag = (LoginFragment) fragment;
        } else if (fragment instanceof RegisterFragment) {
            RegisterFragment regFrag = (RegisterFragment) fragment;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            startActivity(new Intent(LogRegActivity.this, MainActivity.class));
        }
    }

}

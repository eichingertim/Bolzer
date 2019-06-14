package com.teapps.bolzer.nav_activities;

import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.teapps.bolzer.R;


public class ProfileAcivity extends AppCompatActivity implements View.OnClickListener {

    private TextView tvForeName, tvLastName, tvEmail, tvPassword;
    private ImageView imgProfilePic;
    private ImageButton btnBack, btnEdit;
    private Button btn_standard_points, btnBolzerCreatedPoints, btnBolzerTicketPoints;

    private FirebaseFirestore database;
    private FirebaseAuth auth;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        initFirebase();
        initObjects();

        getDataFromDatabaseAndFillObjects();

    }

    private void getDataFromDatabaseAndFillObjects() {
        database.collection(getString(R.string.COLLECTION_USERS)).document(user.getUid())
                .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                DocumentSnapshot ds = task.getResult();
                assert ds != null;
                fillObjects(ds);
            }
        });
    }

    private void fillObjects(DocumentSnapshot ds) {
        tvForeName.setText(ds.getString(getString(R.string.KEY_FORENAME)));
        tvLastName.setText(ds.getString(getString(R.string.KEY_LASTNAME)));
        tvEmail.setText(ds.getString(getString(R.string.KEY_EMAIL)));
        tvPassword.setText("...............");

        btn_standard_points.setText(String.valueOf(ds.getLong("standard_points")));
        btnBolzerCreatedPoints.setText(String.valueOf(ds.getLong("bolzers_created")));
        btnBolzerTicketPoints.setText(String.valueOf(ds.getLong("bolzers_completed_as_member")));
    }

    private void initFirebase() {
        database = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
    }

    private void initObjects() {
        tvForeName = findViewById(R.id.tv_forename);
        tvLastName = findViewById(R.id.tv_lastname);
        tvEmail = findViewById(R.id.tv_email);
        tvPassword = findViewById(R.id.tv_password);
        imgProfilePic = findViewById(R.id.img_profile_picture);
        btnBack = findViewById(R.id.btn_profile_back);
        btnEdit = findViewById(R.id.btn_profile_edit);
        btn_standard_points = findViewById(R.id.btn_standard_points);
        btnBolzerCreatedPoints = findViewById(R.id.btn_bolzer_created);
        btnBolzerTicketPoints = findViewById(R.id.btn_bolzer_tickets);

        btnEdit.setOnClickListener(this);
        btnBack.setOnClickListener(this);
        imgProfilePic.setOnClickListener(this);
        btn_standard_points.setOnClickListener(this);
        btnBolzerTicketPoints.setOnClickListener(this);
        btnBolzerCreatedPoints.setOnClickListener(this);

    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.btn_profile_back:
                finish();
                break;
            case R.id.btn_profile_edit:
                tvForeName.setFocusable(true);
                tvLastName.setFocusable(true);
                tvEmail.setFocusable(true);
                break;
            case R.id.img_profile_picture:
                setNewProfilePicture();
                break;
            case R.id.btn_standard_points:
                Toast.makeText(getApplicationContext(), "Punkte insgesamt", Toast.LENGTH_SHORT).show();
                break;
            case R.id.btn_bolzer_created:
                Toast.makeText(getApplicationContext(), "Anzahl Bolzer erstellt", Toast.LENGTH_SHORT).show();
                break;
            case R.id.btn_bolzer_tickets:
                Toast.makeText(getApplicationContext(), "Anzahl an Bolzer teilgenommen", Toast.LENGTH_SHORT).show();
                break;

        }
    }

    private void setNewProfilePicture() {

    }
}

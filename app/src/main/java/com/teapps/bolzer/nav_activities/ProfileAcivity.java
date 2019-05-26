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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.teapps.bolzer.R;

import static com.teapps.bolzer.helper.Constants.COLLECTION_USERS;
import static com.teapps.bolzer.helper.Constants.KEY_EMAIL;
import static com.teapps.bolzer.helper.Constants.KEY_FORENAME;
import static com.teapps.bolzer.helper.Constants.KEY_LASTNAME;

public class ProfileAcivity extends AppCompatActivity implements View.OnClickListener {

    private TextView tvForeName, tvLastName, tvEmail, tvPassword;
    private ImageView imgProfilePic;
    private ImageButton btnBack, btnEdit;
    private Button btn_standard_points;

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
        database.collection(COLLECTION_USERS).document(user.getUid())
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
        tvForeName.setText(ds.getString(KEY_FORENAME));
        tvLastName.setText(ds.getString(KEY_LASTNAME));
        tvEmail.setText(ds.getString(KEY_EMAIL));
        tvPassword.setText("...............");

        btn_standard_points.setText(String.valueOf(ds.getLong("standard_points")));
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

        btnEdit.setOnClickListener(this);
        btnBack.setOnClickListener(this);
        imgProfilePic.setOnClickListener(this);

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

        }
    }

    private void setNewProfilePicture() {

    }
}

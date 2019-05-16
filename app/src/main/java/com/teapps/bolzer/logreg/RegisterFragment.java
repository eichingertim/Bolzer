package com.teapps.bolzer.logreg;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.teapps.bolzer.MainActivity;
import com.teapps.bolzer.R;

import java.util.HashMap;
import java.util.Map;

import static android.support.constraint.Constraints.TAG;

public class RegisterFragment extends Fragment {

    private EditText txtForeName, txtLastName, txtEmail, txtPassword, txtPasswordConfirm;
    private Button btnRegister;

    private FirebaseAuth mAuth;
    private FirebaseFirestore database;

    private static final String regexEmail = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-z"+
            "A-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeFirebase();
        initializeObjects(view);
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signUpUser();
            }
        });



    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        database = FirebaseFirestore.getInstance();
    }

    private void signUpUser() {
        String email = txtEmail.getText().toString();
        String password = txtPassword.getText().toString();
        String password_confrim = txtPasswordConfirm.getText().toString();
        String forename = txtForeName.getText().toString();
        String lastname = txtLastName.getText().toString();
        String fullname = forename + " " + lastname;
        if (isValidRegistration(email, password, password_confrim, forename, lastname, fullname)) {
            signUp(email, password, forename, lastname, fullname);
        }
    }

    private boolean isValidRegistration(String email, String passwordConfrim, String password
            , String forename, String lastname, String fullname) {

        if (email.isEmpty() || passwordConfrim.isEmpty() || password.isEmpty() || forename.isEmpty()
                || lastname.isEmpty() || fullname.isEmpty()) {
            Toast.makeText(getActivity(), "Fülle alle Felder aus", Toast.LENGTH_LONG).show();
            return false;
        } else if (!email.matches(regexEmail)) {
            Toast.makeText(getActivity(), "E-Mail Adresse entspricht nicht der richtigen Form"
                    , Toast.LENGTH_LONG).show();
            return false;
        } else if (password.length() < 8){
            Toast.makeText(getActivity(), "Passwort muss 8 Stellen lang sein"
                    , Toast.LENGTH_LONG).show();
            return false;

        } else if (!password.equals(passwordConfrim)) {
            Toast.makeText(getActivity(), "Passwörter stimmen nicht überein"
                    , Toast.LENGTH_LONG).show();
            return false;
        } else {
            return true;
        }
    }

    private void signUp(final String email, String password, final String forename
            , final String lastname, final String fullname) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateDatabase(user, email, forename, lastname, fullname);
                            startActivity(new Intent(getActivity(), MainActivity.class));
                        } else {
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Toast.makeText(getActivity(), "Registrierung fehlgeschlagen",
                                    Toast.LENGTH_SHORT).show();
                        }

                        // ...
                    }
                });
    }

    private void updateDatabase(FirebaseUser user, String email, String forename, String lastname, String fullname) {
        Map<String, Object> map = new HashMap<>();
        map.put("forename", forename);
        map.put("lastname", lastname);
        map.put("fullname", fullname);
        map.put("email", email);
        map.put("id", user.getUid());
        map.put("standard_points", 0);
        database.collection("users").document(user.getUid()).set(map).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                Toast.makeText(getActivity(), "Registrierung erfolgreich", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void initializeObjects(View view) {

        txtForeName = view.findViewById(R.id.txt_register_forename);
        txtLastName = view.findViewById(R.id.txt_register_lastname);
        txtEmail = view.findViewById(R.id.txt_register_email);
        txtPassword = view.findViewById(R.id.txt_register_password);
        txtPasswordConfirm = view.findViewById(R.id.txt_register_password_confirm);

        btnRegister = view.findViewById(R.id.btn_register);

    }


}

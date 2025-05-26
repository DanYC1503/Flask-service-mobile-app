package com.example.ui_filter_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private EditText usernameField, emailField, passwordField, displayNameField, bioField;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        usernameField = findViewById(R.id.usernameRegister);
        emailField = findViewById(R.id.emailRegister);
        passwordField = findViewById(R.id.passwordRegister); // nuevo
        displayNameField = findViewById(R.id.displayNameRegister);
        bioField = findViewById(R.id.bioRegister);

        firebaseAuth = FirebaseAuth.getInstance();

        Button registerButton = findViewById(R.id.registerButton);
        TextView goToLogin = findViewById(R.id.goToLogin);

        registerButton.setOnClickListener(view -> {
            String username = usernameField.getText().toString().trim();
            String email = emailField.getText().toString().trim();
            String password = passwordField.getText().toString().trim();
            String displayName = displayNameField.getText().toString().trim();
            String bio = bioField.getText().toString().trim();

            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || displayName.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos obligatorios", Toast.LENGTH_SHORT).show();
                return;
            }

            // Registro con Firebase
            firebaseAuth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        FirebaseUser firebaseUser = authResult.getUser();
                        if (firebaseUser != null) {
                            firebaseUser.getIdToken(true)
                                    .addOnSuccessListener(result -> {
                                        String jwt = result.getToken();
                                        // Ahora registramos al usuario en el backend
                                        String uid = firebaseUser.getUid();
                                        UserProfileDTO user = new UserProfileDTO(uid, email, username, displayName, bio);

                                        UserServiceAPI api = RetrofitClient.getClient().create(UserServiceAPI.class);
                                        Call<UserProfileDTO> call = api.registerUser(user); // Si tu backend requiere token, deberías pasarlo en headers

                                        call.enqueue(new Callback<UserProfileDTO>() {
                                            @Override
                                            public void onResponse(Call<UserProfileDTO> call, Response<UserProfileDTO> response) {
                                                if (response.isSuccessful()) {
                                                    Toast.makeText(RegisterActivity.this, "Usuario registrado correctamente", Toast.LENGTH_SHORT).show();
                                                    // Puedes redirigir al login o main
                                                } else {
                                                    Toast.makeText(RegisterActivity.this, "Error al registrar en backend", Toast.LENGTH_SHORT).show();
                                                }
                                            }

                                            @Override
                                            public void onFailure(Call<UserProfileDTO> call, Throwable t) {
                                                Toast.makeText(RegisterActivity.this, "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });

                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error con Firebase: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        goToLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });

    }
}

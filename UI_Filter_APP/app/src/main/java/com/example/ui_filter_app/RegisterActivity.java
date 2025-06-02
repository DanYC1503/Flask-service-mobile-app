package com.example.ui_filter_app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private EditText usernameField, emailField, passwordField, displayNameField, bioField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Inicializar vistas
        usernameField = findViewById(R.id.usernameRegister);
        emailField = findViewById(R.id.emailRegister);
        passwordField = findViewById(R.id.passwordRegister);
        displayNameField = findViewById(R.id.displayNameRegister);
        bioField = findViewById(R.id.bioRegister);

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

            // ✅ Construimos el objeto para el backend
            UserProfileDTO user = new UserProfileDTO(null, email, username, displayName, bio, password);

            // ✅ Llamada al backend
            UserServiceAPI api = RetrofitClient.getClient(this).create(UserServiceAPI.class);
            Call<UserProfileDTO> call = api.registerUser(user);

            call.enqueue(new Callback<UserProfileDTO>() {
                @Override
                public void onResponse(Call<UserProfileDTO> call, Response<UserProfileDTO> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(RegisterActivity.this, "Registro exitoso", Toast.LENGTH_SHORT).show();
                        Log.d("Register", "Usuario creado en backend");

                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(RegisterActivity.this, "Error al registrar en backend", Toast.LENGTH_SHORT).show();
                        Log.e("BackendError", "Código: " + response.code());
                        try {
                            Log.e("BackendErrorBody", response.errorBody().string());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onFailure(Call<UserProfileDTO> call, Throwable t) {
                    Toast.makeText(RegisterActivity.this, "Fallo de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("RetrofitFailure", t.getMessage(), t);
                }
            });
        });

        goToLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }
}



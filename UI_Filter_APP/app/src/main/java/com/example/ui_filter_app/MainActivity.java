package com.example.ui_filter_app;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {


    private EditText emailField, usernameField, displayNameField, bioField;
    private Button btnUpdate, btnDelete, btnLogout;

    private RecyclerView recyclerView;
    private ScrollView profileScrollView;

    private UserServiceAPI api;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d("MainActivity", "UID del usuario: " + userId);

        // Obtener UID del usuario autenticado con Firebase
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
            Log.d("MainActivity", "UID del usuario: " + userId);
        } else {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }



        // Views perfil
        emailField = findViewById(R.id.emailField);
        usernameField = findViewById(R.id.usernameField);
        displayNameField = findViewById(R.id.displayNameField);
        bioField = findViewById(R.id.bioField);

        btnUpdate = findViewById(R.id.btnUpdate);
        btnDelete = findViewById(R.id.btnDelete);
        btnLogout = findViewById(R.id.btnLogout);

        // Views generales
        recyclerView = findViewById(R.id.postRecyclerView);
        profileScrollView = findViewById(R.id.profileScrollView);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_profile) {
                showUserProfile(); // Mostrar formulario
            } else {
                showPostsList();   // Ocultar formulario en todas las demás secciones
            }

            return true;
        });

        api = RetrofitClient.getClient(MainActivity.this).create(UserServiceAPI.class);

        // Por defecto mostramos perfil (puedes cambiarlo si quieres mostrar lista primero)
        showPostsList();

        btnUpdate.setOnClickListener(v -> updateUserProfile());
        btnDelete.setOnClickListener(v -> deleteUserProfile());

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut(); // Cierra sesión en Firebase
            Toast.makeText(MainActivity.this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
            finish(); // Finaliza actividad, o redirige al login si lo tienes
        });
    }

    private void showUserProfile() {
        recyclerView.setVisibility(View.GONE);
        profileScrollView.setVisibility(View.VISIBLE);
        loadUserProfile();
    }

    private void showPostsList() {
        profileScrollView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    private void loadUserProfile() {
        api.getUser(userId).enqueue(new Callback<UserProfileDTO>() {
            @Override
            public void onResponse(Call<UserProfileDTO> call, Response<UserProfileDTO> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserProfileDTO user = response.body();
                    Toast.makeText(MainActivity.this, "Usuario cargado: " + user.getUserName(), Toast.LENGTH_SHORT).show();
                    emailField.setText(user.getEmail());
                    usernameField.setText(user.getUserName());
                    displayNameField.setText(user.getDisplayName());
                    bioField.setText(user.getBio());
                } else {
                    Toast.makeText(MainActivity.this, "Error al cargar perfil: Código " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }


            @Override
            public void onFailure(Call<UserProfileDTO> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Fallo conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUserProfile() {

        String email = emailField.getText().toString().trim();
        String username = usernameField.getText().toString().trim();
        String displayName = displayNameField.getText().toString().trim();
        String bio = bioField.getText().toString().trim();
        String password = bioField.getText().toString().trim();

        UserProfileDTO user = new UserProfileDTO(userId, email, username, displayName, bio, password);

        api.updateUser(userId, user).enqueue(new Callback<UserProfileDTO>() {
            @Override
            public void onResponse(Call<UserProfileDTO> call, Response<UserProfileDTO> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "Perfil actualizado", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Error al actualizar perfil", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UserProfileDTO> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Fallo conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void deleteUserProfile() {
        api.deleteUser(userId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "Perfil eliminado", Toast.LENGTH_SHORT).show();
                    finish(); // O ir a pantalla login
                } else {
                    Toast.makeText(MainActivity.this, "Error al eliminar perfil", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Fallo conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

}

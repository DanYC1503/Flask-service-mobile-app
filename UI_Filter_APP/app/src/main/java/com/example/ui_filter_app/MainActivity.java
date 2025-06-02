package com.example.ui_filter_app;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
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
    private FrameLayout uploadLayout;

    private UserServiceAPI api;
    private String userId;

    // Variables para manejo de imágenes
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_PICK = 2;
    private static final int PERMISSION_REQUEST_CODE = 100;

    private ImageView imagePreview;
    private Button btnTakePhoto, btnSelectPhoto, btnUploadPhoto;
    private ProgressBar uploadProgress;
    private Bitmap currentBitmap;
    private Uri currentImageUri;

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
        uploadLayout = findViewById(R.id.uploadLayout);

        // Views para subida de fotos
        imagePreview = findViewById(R.id.imagePreview);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnSelectPhoto = findViewById(R.id.btnSelectPhoto);
        btnUploadPhoto = findViewById(R.id.btnUploadPhoto);
        uploadProgress = findViewById(R.id.uploadProgress);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_profile) {
                showUserProfile();
            } else if (itemId == R.id.nav_upload) {
                showUploadSection();
            } else {
                showPostsList();
            }
            return true;
        });

        api = RetrofitClient.getClient(MainActivity.this).create(UserServiceAPI.class);

        // Por defecto mostramos lista de posts
        showPostsList();

        // Configurar listeners
        btnUpdate.setOnClickListener(v -> updateUserProfile());
        btnDelete.setOnClickListener(v -> deleteUserProfile());
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(MainActivity.this, "Sesión cerrada", Toast.LENGTH_SHORT).show();

            // Redirigir a LoginActivity y cerrar MainActivity
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK); // Limpiar el stack de actividades
            startActivity(intent);
            finish(); // Cierra MainActivity para evitar volver atrás con el botón "Back"
        });

        // Listeners para subida de fotos
        btnTakePhoto.setOnClickListener(v -> dispatchTakePictureIntent());
        btnSelectPhoto.setOnClickListener(v -> openImagePicker());
        btnUploadPhoto.setOnClickListener(v -> uploadImage());

        // Verificar permisos
        checkPermissions();
    }

    private void showUserProfile() {
        recyclerView.setVisibility(View.GONE);
        profileScrollView.setVisibility(View.VISIBLE);
        uploadLayout.setVisibility(View.GONE);
        loadUserProfile();
    }

    private void showPostsList() {
        profileScrollView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        uploadLayout.setVisibility(View.GONE);

        SharedPreferences prefs = getSharedPreferences("posts", MODE_PRIVATE);
        Set<String> ids = prefs.getStringSet("postIds", new HashSet<>());
        List<String> postIds = new ArrayList<>(ids);

        List<PostImage> postImages = new ArrayList<>();
        PostImageAdapter adapter = new PostImageAdapter(this, postImages);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        for (String postId : postIds) {
            api.getImageUrl(postId).enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        postImages.add(new PostImage(postId, response.body()));
                        adapter.notifyItemInserted(postImages.size() - 1);
                    }
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    Log.e("ImageLoad", "Error al obtener imagen: " + t.getMessage());
                }
            });
        }
    }




    private void showUploadSection() {
        recyclerView.setVisibility(View.GONE);
        profileScrollView.setVisibility(View.GONE);
        uploadLayout.setVisibility(View.VISIBLE);
        btnUploadPhoto.setVisibility(View.GONE);
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
                    finish();
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

    // Métodos para manejo de imágenes
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE && data != null) {
                Bundle extras = data.getExtras();
                currentBitmap = (Bitmap) extras.get("data");
                imagePreview.setImageBitmap(currentBitmap);
                btnUploadPhoto.setVisibility(View.VISIBLE);
            } else if (requestCode == REQUEST_IMAGE_PICK && data != null) {
                currentImageUri = data.getData();
                try {
                    currentBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), currentImageUri);
                    imagePreview.setImageBitmap(currentBitmap);
                    btnUploadPhoto.setVisibility(View.VISIBLE);
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Error al cargar la imagen", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void uploadImage() {
        if (currentBitmap == null) {
            Toast.makeText(this, "No hay imagen para subir", Toast.LENGTH_SHORT).show();
            return;
        }

        uploadProgress.setVisibility(View.VISIBLE);
        btnUploadPhoto.setEnabled(false);

        File file = bitmapToFile(currentBitmap);
        if (file == null) {
            Toast.makeText(this, "Error al preparar la imagen", Toast.LENGTH_SHORT).show();
            uploadProgress.setVisibility(View.GONE);
            btnUploadPhoto.setEnabled(true);
            return;
        }

        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

        String postId = "post_" + System.currentTimeMillis();

        api.uploadImage(postId, body).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                uploadProgress.setVisibility(View.GONE);
                btnUploadPhoto.setEnabled(true);

                if (response.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "Imagen subida exitosamente", Toast.LENGTH_SHORT).show();

                    savePostIdLocally(postId); // ✅ Guarda postId

                    imagePreview.setImageBitmap(null);
                    btnUploadPhoto.setVisibility(View.GONE);
                    currentBitmap = null;
                    currentImageUri = null;

                } else {
                    Toast.makeText(MainActivity.this, "Error al subir la imagen", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                uploadProgress.setVisibility(View.GONE);
                btnUploadPhoto.setEnabled(true);
                Toast.makeText(MainActivity.this, "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void savePostIdLocally(String postId) {
        SharedPreferences prefs = getSharedPreferences("posts", MODE_PRIVATE);
        Set<String> ids = new HashSet<>(prefs.getStringSet("postIds", new HashSet<>()));
        ids.add(postId);
        prefs.edit().putStringSet("postIds", ids).apply();
    }



    private File bitmapToFile(Bitmap bitmap) {
        File file = new File(getCacheDir(), "temp_image.jpg");
        try {
            file.createNewFile();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bos);
            byte[] bitmapData = bos.toByteArray();

            FileOutputStream fos = new FileOutputStream(file);
            fos.write(bitmapData);
            fos.flush();
            fos.close();
            return file;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.CAMERA,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },
                    PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (!allGranted) {
                Toast.makeText(this, "Los permisos son necesarios para usar esta función", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
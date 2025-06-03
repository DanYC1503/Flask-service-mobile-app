package com.example.ui_filter_app;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Toast;
import android.widget.ImageButton;


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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.ui_filter_app.FilterApi;

import org.json.JSONObject;


public class MainActivity extends AppCompatActivity {

    private EditText emailField, usernameField, displayNameField, bioField;
    private Button btnUpdate, btnDelete, btnLogout;
    private boolean isImageLoaded = false;

    private RecyclerView recyclerView;
    private ScrollView profileScrollView;
    private FrameLayout uploadLayout;

    private LinearLayout myPostsContainer;
    private FilterController filterController;

    private final Map<Integer, String> filterMap = new HashMap<Integer, String>() {{
        put(R.id.filterNone, "none");
        put(R.id.filterMotion, "motion");
        put(R.id.filterDog, "dog");
        put(R.id.filterBack, "back");
        put(R.id.filterNegative, "negative");
        put(R.id.filterInk, "ink");
    }};
    private UserServiceAPI api;
    private String userId;

    // Variables para manejo de imágenes
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_PICK = 2;
    private static final int PERMISSION_REQUEST_CODE = 100;

    private ImageButton selectedFilter = null;

    private ImageView imagePreview;
    private Button btnTakePhoto, btnSelectPhoto, btnUploadPhoto;
    private ProgressBar uploadProgress;
    private Bitmap currentBitmap;
    private Uri currentImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myPostsContainer = findViewById(R.id.myPostsContainer);

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
            } else if (itemId == R.id.nav_home) {  // <- Este sería el ítem del feed general
                showPostsList();  // Muestra todas las publicaciones
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

        filterController = new FilterController();

        for (Integer id : filterMap.keySet()) {
            ImageButton button = findViewById(id);
            button.setOnClickListener(view -> {
                // Only apply filter if image is loaded
                if (!isImageLoaded) {
                    Toast.makeText(MainActivity.this, "Carga una imagen primero", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Unselect previous filter button if any
                if (selectedFilter != null) selectedFilter.setSelected(false);

                // Select the clicked filter button
                view.setSelected(true);
                selectedFilter = (ImageButton) view;

                // Get the filter method string
                String method = filterMap.get(view.getId());

                // Update selected filter method in controller
                filterController.applyFilter(method);

                // 🔥 Automatically process image with selected filter
                processImage();
            });
        }

            // Verificar permisos
        checkPermissions();
    }

    private void showUserProfile() {
        recyclerView.setVisibility(View.GONE);
        profileScrollView.setVisibility(View.VISIBLE);
        uploadLayout.setVisibility(View.GONE);

        loadUserProfile(); // Ahora showMyPostsInProfile se invoca dentro de loadUserProfile
    }

    private void loadUserProfile() {
        api.getUser(userId).enqueue(new Callback<UserProfileDTO>() {
            @Override
            public void onResponse(Call<UserProfileDTO> call, Response<UserProfileDTO> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserProfileDTO user = response.body();
                    emailField.setText(user.getEmail());
                    usernameField.setText(user.getUserName());
                    displayNameField.setText(user.getDisplayName());
                    bioField.setText(user.getBio());

                    showMyPostsInProfile(); // Solo aquí, tras cargar los datos
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

    private void showMyPostsInProfile() {
        myPostsContainer.removeAllViews();

        SharedPreferences prefs = getSharedPreferences("posts", MODE_PRIVATE);
        Set<String> raw = prefs.getStringSet("postData", new HashSet<>());
        String myName = displayNameField.getText().toString().trim();
        if (myName.isEmpty()) {
            myName = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        }

        for (String entry : raw) {
            String[] parts = entry.split("::");
            if (parts.length == 2 && parts[1].equals(myName)) {
                String postId = parts[0];
                String userName = parts[1];

                api.getImageUrl(postId).enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            ImageView imageView = new ImageView(MainActivity.this);
                            imageView.setLayoutParams(new LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT, 400));
                            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            imageView.setPadding(0, 8, 0, 8);

                            Glide.with(MainActivity.this)
                                    .load(response.body())
                                    .centerCrop()
                                    .placeholder(R.drawable.ic_launcher_background)
                                    .into(imageView);

                            myPostsContainer.addView(imageView);
                        }
                    }

                    @Override
                    public void onFailure(Call<String> call, Throwable t) {
                        Log.e("ProfileImageLoad", "Error al cargar imagen: " + t.getMessage());
                    }
                });
            }
        }
    }

    // ... (el resto de los métodos permanecen igual)



    private void showPostsList() {
        profileScrollView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        uploadLayout.setVisibility(View.GONE);

        SharedPreferences prefs = getSharedPreferences("posts", MODE_PRIVATE);
        Set<String> raw = prefs.getStringSet("postData", new HashSet<>());
        List<PostImage> postImages = new ArrayList<>();
        PostImageAdapter adapter = new PostImageAdapter(this, postImages);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Mostrar TODAS las publicaciones (propias y de otros)
        for (String entry : raw) {
            String[] parts = entry.split("::");
            if (parts.length == 2) {  // Eliminamos cualquier filtro que hubiera
                String postId = parts[0];
                String userName = parts[1];

                api.getImageUrl(postId).enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            postImages.add(new PostImage(postId, response.body(), userName));
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
    }

    private void showUploadSection() {
        recyclerView.setVisibility(View.GONE);
        profileScrollView.setVisibility(View.GONE);
        uploadLayout.setVisibility(View.VISIBLE);
        btnUploadPhoto.setVisibility(View.GONE);
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

                // Mark image as loaded
                isImageLoaded = true;

            } else if (requestCode == REQUEST_IMAGE_PICK && data != null) {
                currentImageUri = data.getData();
                try {
                    currentBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), currentImageUri);
                    imagePreview.setImageBitmap(currentBitmap);
                    btnUploadPhoto.setVisibility(View.VISIBLE);

                    // Mark image as loaded
                    isImageLoaded = true;

                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Error al cargar la imagen", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    private void processImage() {
        if (currentBitmap == null) {
            Toast.makeText(this, "No hay imagen para procesar", Toast.LENGTH_SHORT).show();
            return;
        }

        uploadProgress.setVisibility(View.VISIBLE);
        btnUploadPhoto.setEnabled(false);

        FilterController.sendFilterRequest(this, currentBitmap, FilterController.selectedMethod, new FilterController.FilterCallback() {
            @Override
            public void onSuccess(String jsonResponse) {
                runOnUiThread(() -> {
                    try {
                        JSONObject response = new JSONObject(jsonResponse);
                        String base64Image = response.getString("output_image_base64");

                        byte[] imageBytes = Base64.decode(base64Image, Base64.DEFAULT);
                        Bitmap filteredBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

                        ImageView filteredImageView = findViewById(R.id.filteredImageView);
                        LinearLayout filteredImageContainer = findViewById(R.id.filteredImageContainer);

                        filteredImageView.setImageBitmap(filteredBitmap);
                        filteredImageContainer.setVisibility(View.VISIBLE);

                        Toast.makeText(MainActivity.this, "Imagen procesada con éxito", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "Error al procesar la imagen", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    } finally {
                        uploadProgress.setVisibility(View.GONE);
                        btnUploadPhoto.setEnabled(true);
                    }
                });
            }


            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                    uploadProgress.setVisibility(View.GONE);
                    btnUploadPhoto.setEnabled(true);
                });
            }
        });
    }


    private void uploadImage() {

}

    private void savePostIdLocally(String postId) {
        SharedPreferences prefs = getSharedPreferences("posts", MODE_PRIVATE);
        Set<String> raw = prefs.getStringSet("postData", new HashSet<>());

        String userName = displayNameField.getText().toString().trim();
        if (userName.isEmpty()) {
            userName = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        }

        raw.add(postId + "::" + userName);

        prefs.edit().putStringSet("postData", raw).apply();
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

    private void showMyPosts() {
        profileScrollView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        uploadLayout.setVisibility(View.GONE);

        SharedPreferences prefs = getSharedPreferences("posts", MODE_PRIVATE);
        Set<String> raw = prefs.getStringSet("postData", new HashSet<>());
        List<PostImage> myImages = new ArrayList<>();
        PostImageAdapter adapter = new PostImageAdapter(this, myImages);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        String myName = displayNameField.getText().toString().trim();
        if (myName.isEmpty()) {
            myName = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        }

        for (String entry : raw) {
            String[] parts = entry.split("::");
            if (parts.length == 2 && parts[1].equals(myName)) {
                String postId = parts[0];
                String userName = parts[1];

                api.getImageUrl(postId).enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            myImages.add(new PostImage(postId, response.body(), userName));
                            adapter.notifyItemInserted(myImages.size() - 1);
                        }
                    }

                    @Override
                    public void onFailure(Call<String> call, Throwable t) {
                        Log.e("ImageLoad", "Error al obtener imagen: " + t.getMessage());
                    }
                });
            }
        }
    }

    private void showOtherPosts() {
        profileScrollView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        uploadLayout.setVisibility(View.GONE);

        SharedPreferences prefs = getSharedPreferences("posts", MODE_PRIVATE);
        Set<String> raw = prefs.getStringSet("postData", new HashSet<>());
        List<PostImage> otherImages = new ArrayList<>();
        PostImageAdapter adapter = new PostImageAdapter(this, otherImages);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        String myName = displayNameField.getText().toString().trim();
        if (myName.isEmpty()) {
            myName = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        }

        for (String entry : raw) {
            String[] parts = entry.split("::");
            if (parts.length == 2 && !parts[1].equals(myName)) {
                String postId = parts[0];
                String userName = parts[1];

                api.getImageUrl(postId).enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            otherImages.add(new PostImage(postId, response.body(), userName));
                            adapter.notifyItemInserted(otherImages.size() - 1);
                        }
                    }

                    @Override
                    public void onFailure(Call<String> call, Throwable t) {
                        Log.e("ImageLoad", "Error al obtener imagen: " + t.getMessage());
                    }
                });
            }
        }
    }
}
package com.example.ui_filter_app;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
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
import java.net.HttpURLConnection;
import java.net.URL;
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

    private RecyclerView myPostsContainer;
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
                showOtherPosts();  // Muestra todas las publicaciones
            }
            return true;
        });

        api = RetrofitClient.getClient(MainActivity.this).create(UserServiceAPI.class);

        // Por defecto mostramos lista de posts
        showOtherPosts();

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

                    showMyPosts(); // Solo aquí, tras cargar los datos
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
        ImageView filteredImageView = findViewById(R.id.filteredImageView);
        filteredImageView.setDrawingCacheEnabled(true);
        filteredImageView.buildDrawingCache();
        Bitmap currentBitmap = ((BitmapDrawable) filteredImageView.getDrawable()).getBitmap();

        if (currentBitmap == null) {
            Toast.makeText(this, "No hay imagen para subir", Toast.LENGTH_SHORT).show();
            return;
        }

        uploadProgress.setVisibility(View.VISIBLE);
        btnUploadPhoto.setEnabled(false);

        filterController.uploadImageToBucket(this, currentBitmap, new FilterController.FilterCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                runOnUiThread(() -> {
                    uploadProgress.setVisibility(View.GONE);
                    btnUploadPhoto.setEnabled(true);
                    Toast.makeText(MainActivity.this, "Imagen subida con éxito", Toast.LENGTH_SHORT).show();
                    Toast.makeText(MainActivity.this, "Image URL: " + imageUrl, Toast.LENGTH_SHORT).show();
                    createPost(imageUrl);

                    // Optional verification
                    new Thread(() -> {
                        boolean uploadVerified = verifyImageUpload(imageUrl);
                        runOnUiThread(() -> {
                            if (uploadVerified) {
                                Toast.makeText(MainActivity.this, "Imagen verificada", Toast.LENGTH_SHORT).show();
                                refreshViews();
                            } else {
                                Toast.makeText(MainActivity.this, "La imagen no está accesible", Toast.LENGTH_LONG).show();
                            }
                        });
                    }).start();
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    uploadProgress.setVisibility(View.GONE);
                    btnUploadPhoto.setEnabled(true);
                    Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    Log.e("Upload", errorMessage);
                });
            }
        });
    }



    // Helper method to verify image exists
    private boolean verifyImageUpload(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            int responseCode = connection.getResponseCode();
            return responseCode == HttpURLConnection.HTTP_OK;
        } catch (Exception e) {
            Log.e("UploadVerify", "Verification failed", e);
            return false;
        }
    }

    private void createPost(String imageUrl) {
        String authorId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Log.d("createPost", "AuthorId: " + authorId);

        // First get user info
        UserServiceAPI userServiceAPI = RetrofitClient.getClient(MainActivity.this).create(UserServiceAPI.class);

        Call<UserProfileDTO> userCall = userServiceAPI.getUser(authorId);

        userCall.enqueue(new Callback<UserProfileDTO>() {
            @Override
            public void onResponse(Call<UserProfileDTO> call, Response<UserProfileDTO> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserProfileDTO user = response.body();
                    Log.d("createPost", "User fetched: uid=" + user.getUid() + ", userName=" + user.getUserName());

                    String userName = user.getUserName(); // Title will be the username

                    // Now create the PostDTO
                    PostDTO postDTO = new PostDTO(userName, imageUrl, authorId);
                    Log.d("createPost", "PostDTO created: title=" + postDTO.getTitle() + ", content=" + postDTO.getContent() + ", authorId=" + postDTO.getAuthorId());

                    PostServiceAPI postServiceAPI = RetrofitClient.getClient(MainActivity.this).create(PostServiceAPI.class);

                    Call<Post> postCall = postServiceAPI.uploadPostToFeed(postDTO);

                    postCall.enqueue(new Callback<Post>() {
                        @Override
                        public void onResponse(Call<Post> call, Response<Post> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                Post post = response.body();
                                Log.d("createPost", "Post created: id=" + post.getId());
                                Toast.makeText(MainActivity.this, "Post creado: " + post.getId(), Toast.LENGTH_SHORT).show();
                            } else {
                                // Log full error response body
                                try {
                                    String errorBody = response.errorBody().string();
                                    Log.e("createPost", "Error creating post: " + errorBody);
                                    Toast.makeText(MainActivity.this, "Error en servidor:\n" + errorBody, Toast.LENGTH_LONG).show();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    Toast.makeText(MainActivity.this, "Error al leer el error", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }

                        @Override
                        public void onFailure(Call<Post> call, Throwable t) {
                            Log.e("createPost", "Failure creating post", t);
                            Toast.makeText(MainActivity.this, "Fallo en la conexión (Post): " + t.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });

                } else {
                    Log.e("createPost", "Error fetching user: " + response.message());
                    Toast.makeText(MainActivity.this, "No se pudo obtener el usuario: " + response.message(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<UserProfileDTO> call, Throwable t) {
                Log.e("createPost", "Failure fetching user", t);
                Toast.makeText(MainActivity.this, "Fallo en la conexión (User): " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }


    private void refreshViews() {
        if (profileScrollView.getVisibility() == View.VISIBLE) {
            showMyPosts();
        }
        if (recyclerView.getVisibility() == View.VISIBLE) {
            showOtherPosts();
        }
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
        profileScrollView.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        uploadLayout.setVisibility(View.GONE);

        myPostsContainer.setVisibility(View.VISIBLE);

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        PostServiceAPI postServiceAPI = RetrofitClient.getClient(MainActivity.this).create(PostServiceAPI.class);
        UserServiceAPI userServiceAPI = RetrofitClient.getClient(MainActivity.this).create(UserServiceAPI.class);

        List<PostImage> myPostImages = new ArrayList<>();
        PostImageAdapter adapter = new PostImageAdapter(this, myPostImages, postServiceAPI, userId, userServiceAPI);

        myPostsContainer.setLayoutManager(new LinearLayoutManager(this));
        myPostsContainer.setAdapter(adapter);

        userServiceAPI.getUser(userId).enqueue(new Callback<UserProfileDTO>() {
            @Override
            public void onResponse(Call<UserProfileDTO> call, Response<UserProfileDTO> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String userName = response.body().getUserName();

                    postServiceAPI.getAllPosts().enqueue(new Callback<List<Post>>() {
                        @Override
                        public void onResponse(Call<List<Post>> call, Response<List<Post>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                List<Post> allPosts = response.body();

                                for (Post post : allPosts) {
                                    if (userId.equals(post.getAuthorId())) {
                                        String imageUrl = post.getContent();
                                        if (imageUrl != null && !imageUrl.isEmpty()) {
                                            // Add to adapter's data list
                                            myPostImages.add(new PostImage(post.getId(), imageUrl, userName));
                                        }
                                    }
                                }

                                // Notify adapter of data change to update RecyclerView
                                adapter.notifyDataSetChanged();
                            } else {
                                Toast.makeText(MainActivity.this, "No se pudo cargar posts", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<List<Post>> call, Throwable t) {
                            Toast.makeText(MainActivity.this, "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

                } else {
                    Toast.makeText(MainActivity.this, "No se pudo obtener el usuario para mostrar posts", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<UserProfileDTO> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Fallo en la conexión al obtener usuario: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showOtherPosts() {
        profileScrollView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        uploadLayout.setVisibility(View.GONE);

        // Initialize APIs first
        PostServiceAPI postServiceAPI = RetrofitClient.getClient(MainActivity.this).create(PostServiceAPI.class);
        UserServiceAPI userServiceAPI = RetrofitClient.getClient(MainActivity.this).create(UserServiceAPI.class);

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        List<PostImage> postImages = new ArrayList<>();

        // Pass APIs and userId to adapter
        PostImageAdapter adapter = new PostImageAdapter(this, postImages, postServiceAPI, currentUserId, userServiceAPI);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        postServiceAPI.getAllPosts().enqueue(new Callback<List<Post>>() {
            @Override
            public void onResponse(Call<List<Post>> call, Response<List<Post>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Post> allPosts = response.body();
                    LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
                    for (Post post : allPosts) {
                        String imageUrl = post.getContent();
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            View postView = inflater.inflate(R.layout.item_post, recyclerView, false);
                            userServiceAPI.getUser(post.getAuthorId()).enqueue(new Callback<UserProfileDTO>() {
                                @Override
                                public void onResponse(Call<UserProfileDTO> call, Response<UserProfileDTO> userResponse) {
                                    if (userResponse.isSuccessful() && userResponse.body() != null) {
                                        String userName = userResponse.body().getUserName();

                                        PostImage item = new PostImage(post.getId(), imageUrl, userName);
                                        postImages.add(item);
                                        adapter.notifyItemInserted(postImages.size() - 1);
                                    } else {
                                        Log.e("UserFetch", "No se pudo obtener nombre para el autor ID: " + post.getAuthorId());
                                    }
                                }

                                @Override
                                public void onFailure(Call<UserProfileDTO> call, Throwable t) {
                                    Log.e("UserFetch", "Error al obtener usuario: " + t.getMessage());
                                }
                            });
                        }
                    }
                } else {
                    Toast.makeText(MainActivity.this, "No se pudo cargar posts", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Post>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

}
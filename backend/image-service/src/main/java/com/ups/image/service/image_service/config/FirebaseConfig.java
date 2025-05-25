package com.ups.image.service.image_service.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @Bean
    public FirebaseApp initializeFirebase() {
        try {
            InputStream serviceAccount = getClass().getClassLoader().getResourceAsStream("firebase/serviceAccountKey.json");

            FirebaseOptions firebaseOptions = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setStorageBucket("upsglam.firebasestorage.app")
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                return FirebaseApp.initializeApp(firebaseOptions);
            } else {
                return FirebaseApp.getApps().get(0);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error initializing Firebase", e);
        }
    }

    @Bean
    public Storage storage() {
        try {
            InputStream serviceAccount = getClass().getClassLoader().getResourceAsStream("firebase/serviceAccountKey.json");

            GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
            return StorageOptions.newBuilder()
                    .setCredentials(credentials)
                    .build()
                    .getService();
        } catch (Exception e) {
            throw new RuntimeException("Error initializing Storage", e);
        }
    }

}

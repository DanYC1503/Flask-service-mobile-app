package com.ups.user.service.user_service.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;


@Configuration
public class FirebaseConfig {

    @Bean
    public Firestore getFirestore() throws IOException {
        InputStream serviceAccount = getClass().getClassLoader().getResourceAsStream("firebase/serviceAccountKey.json");

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseOptions firebaseOptions = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
            FirebaseApp.initializeApp(firebaseOptions);
        }

        InputStream firestoreServiceAccount = getClass().getClassLoader().getResourceAsStream("firebase/serviceAccountKey.json");

        FirestoreOptions firestoreOptions = FirestoreOptions.newBuilder()
                .setCredentials(GoogleCredentials.fromStream(firestoreServiceAccount))
                .setProjectId("upsglam")
                .setDatabaseId("upsglamdb")
                .build();

        return firestoreOptions.getService();
    }
}
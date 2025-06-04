package com.example.ui_filter_app;

public class FireStoreTimestamp {
    private long seconds;
    private int nanos;

    public FireStoreTimestamp() {}

    public long getSeconds() {
        return seconds;
    }

    public int getNanos() {
        return nanos;
    }

    // Optional: convenience method to get as Java Date
    public java.util.Date toDate() {
        return new java.util.Date(seconds * 1000 + nanos / 1000000);
    }
}

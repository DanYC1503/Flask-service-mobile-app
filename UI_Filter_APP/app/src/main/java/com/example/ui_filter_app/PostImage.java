    package com.example.ui_filter_app;


    public class PostImage {
        private String postId;
        private String imageUrl;
        private String userName;

        public PostImage(String postId, String imageUrl, String userName) {
            this.postId = postId;
            this.imageUrl = imageUrl;
            this.userName = userName;
        }

        public String getPostId() {
            return postId;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public String getUserName() {
            return userName;
        }
    }

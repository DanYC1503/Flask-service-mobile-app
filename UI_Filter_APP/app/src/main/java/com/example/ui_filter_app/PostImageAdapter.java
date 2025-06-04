package com.example.ui_filter_app;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PostImageAdapter extends RecyclerView.Adapter<PostImageAdapter.ImageViewHolder> {

    private final List<PostImage> images;
    private final Context context;
    private final PostServiceAPI postServiceAPI;
    private final UserServiceAPI userServiceAPI;
    private final String currentUserId;

    public PostImageAdapter(Context context, List<PostImage> images, PostServiceAPI postServiceAPI, String currentUserId, UserServiceAPI userServiceAPI) {
        this.context = context;
        this.images = images;
        this.postServiceAPI = postServiceAPI;
        this.userServiceAPI = userServiceAPI;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        PostImage image = images.get(position);
        String postId = image.getPostId();

        // Show username
        holder.userNameText.setText("Publicado por: " + image.getUserName());

        // Load image with Glide
        Glide.with(context)
                .load(image.getImageUrl())
                .centerCrop()
                .placeholder(R.drawable.ic_launcher_background)
                .into(holder.imageView);

        // Load like count from API
        postServiceAPI.getLikeCount(postId).enqueue(new retrofit2.Callback<Long>() {
            @Override
            public void onResponse(retrofit2.Call<Long> call, retrofit2.Response<Long> response) {
                if (response.isSuccessful() && response.body() != null) {
                    holder.likeCount.setText(String.valueOf(response.body()));
                } else {
                    holder.likeCount.setText("0");
                }
            }

            @Override
            public void onFailure(retrofit2.Call<Long> call, Throwable t) {
                holder.likeCount.setText("0");
            }
        });

        // Like button click handler
        holder.likeButton.setOnClickListener(v -> {
            holder.likeButton.setEnabled(false);

            postServiceAPI.addLike(postId, currentUserId).enqueue(new retrofit2.Callback<Void>() {
                @Override
                public void onResponse(retrofit2.Call<Void> call, retrofit2.Response<Void> response) {
                    if (response.isSuccessful()) {
                        // Refresh like count after adding like
                        postServiceAPI.getLikeCount(postId).enqueue(new retrofit2.Callback<Long>() {
                            @Override
                            public void onResponse(retrofit2.Call<Long> call, retrofit2.Response<Long> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    holder.likeCount.setText(String.valueOf(response.body()));
                                }
                                holder.likeButton.setEnabled(true);
                            }

                            @Override
                            public void onFailure(retrofit2.Call<Long> call, Throwable t) {
                                holder.likeButton.setEnabled(true);
                            }
                        });
                    } else {
                        holder.likeButton.setEnabled(true);
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<Void> call, Throwable t) {
                    holder.likeButton.setEnabled(true);
                }
            });
        });

        holder.commentButton.setOnClickListener(v -> {
            if (holder.commentsSection.getVisibility() == View.VISIBLE) {
                holder.commentsSection.setVisibility(View.GONE);
            } else {
                holder.commentsSection.setVisibility(View.VISIBLE);
                loadComments(postId, holder);
            }
        });

        // Post comment handler
        holder.postCommentButton.setOnClickListener(v -> {
            String commentText = holder.commentInput.getText().toString().trim();
            if (!commentText.isEmpty()) {
                CommentDTO commentDTO = new CommentDTO();
                commentDTO.setUserId(currentUserId);
                commentDTO.setText(commentText);

                postServiceAPI.addComment(postId, commentDTO).enqueue(new Callback<Comment>() {
                    @Override
                    public void onResponse(Call<Comment> call, Response<Comment> response) {
                        if (response.isSuccessful()) {
                            holder.commentInput.setText("");
                            loadComments(postId, holder);
                        }
                    }

                    @Override
                    public void onFailure(Call<Comment> call, Throwable t) {
                        // Handle error
                    }
                });
            }
        });
    }
    private void loadComments(String postId, ImageViewHolder holder) {
        holder.commentsContainer.removeAllViews();

        postServiceAPI.getComments(postId).enqueue(new Callback<List<Comment>>() {
            @Override
            public void onResponse(Call<List<Comment>> call, Response<List<Comment>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Comment> comments = response.body();
                    if (comments.isEmpty()) {
                        holder.commentsTitle.setText("No comments yet");
                    } else {
                        holder.commentsTitle.setText("Comments (" + comments.size() + ")");

                        for (Comment comment : comments) {
                            View commentView = LayoutInflater.from(context)
                                    .inflate(R.layout.item_comment, holder.commentsContainer, false);

                            TextView commentUser = commentView.findViewById(R.id.commentUser);
                            TextView commentText = commentView.findViewById(R.id.commentText);
                            ImageView deleteComment = commentView.findViewById(R.id.deleteComment);

                            commentText.setText(comment.getText());

                            String commenterUserId = comment.getUserId();

                            // Fetch username for this user ID from your userServiceAPI or Firebase
                            userServiceAPI.getUser(commenterUserId).enqueue(new Callback<UserProfileDTO>() {
                                @Override
                                public void onResponse(Call<UserProfileDTO> call, Response<UserProfileDTO> response) {
                                    if (response.isSuccessful() && response.body() != null) {
                                        commentUser.setText(response.body().getUserName());
                                    } else {
                                        commentUser.setText("Unknown user");
                                    }
                                    setupDeleteIfOwner(deleteComment, commenterUserId, comment, postId, holder);
                                    holder.commentsContainer.addView(commentView);
                                }

                                @Override
                                public void onFailure(Call<UserProfileDTO> call, Throwable t) {
                                    commentUser.setText("Unknown user");
                                    setupDeleteIfOwner(deleteComment, commenterUserId, comment, postId, holder);
                                    holder.commentsContainer.addView(commentView);
                                }
                            });
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Comment>> call, Throwable t) {
                holder.commentsTitle.setText("Error loading comments");
            }
        });
    }

    private void setupDeleteIfOwner(ImageView deleteComment, String userId, Comment comment, String postId, ImageViewHolder holder) {
        if (userId.equals(currentUserId)) {
            deleteComment.setVisibility(View.VISIBLE);
            deleteComment.setOnClickListener(v -> {
                postServiceAPI.deleteComment(postId, comment.getId()).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            loadComments(postId, holder);
                        }
                    }
                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        // Handle error
                    }
                });
            });
        } else {
            deleteComment.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView userNameText;
        ImageView likeButton;
        TextView likeCount;
        ImageView commentButton;
        LinearLayout commentsSection;
        LinearLayout commentsContainer;
        EditText commentInput;
        Button postCommentButton;
        TextView commentsTitle;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.postImageView);
            userNameText = itemView.findViewById(R.id.userNameText);
            likeButton = itemView.findViewById(R.id.likeButton);
            likeCount = itemView.findViewById(R.id.likeCount);
            commentButton = itemView.findViewById(R.id.commentButton);
            commentsSection = itemView.findViewById(R.id.commentsSection);
            commentsContainer = itemView.findViewById(R.id.commentsContainer);
            commentInput = itemView.findViewById(R.id.commentInput);
            postCommentButton = itemView.findViewById(R.id.postCommentButton);
            commentsTitle = itemView.findViewById(R.id.commentsTitle);
        }
    }
}

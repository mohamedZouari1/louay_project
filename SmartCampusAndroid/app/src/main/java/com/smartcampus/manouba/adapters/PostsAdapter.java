package com.smartcampus.manouba.adapters;

import android.content.Context;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.gson.JsonObject;
import com.smartcampus.manouba.R;
import com.smartcampus.manouba.fragments.SocialHubFragment;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.smartcampus.manouba.utils.SharedPrefManager;
import com.smartcampus.manouba.network.RetrofitClient;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class PostsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER      = 0;
    private static final int TYPE_POST        = 1;
    private static final int TYPE_SUGGESTIONS = 2;

    public interface OnLikeClickListener {
        void onLikeClick(int postId, boolean currentlyLiked, int position);
    }

    public interface OnCommentClickListener {
        void onCommentClick(int postId, int position);
    }

    public interface OnRepostClickListener {
        void onRepostClick(int postId, int position);
    }

    /** Callback so FeedFragment can navigate to the Compose tab. */
    public interface OnComposeClickListener {
        void onComposeClick();
    }

    private final Context context;
    private final List<JsonObject> posts;
    private List<JsonObject> suggestions;
    private OnLikeClickListener likeListener;
    private OnCommentClickListener commentListener;
    private OnRepostClickListener repostListener;
    private OnComposeClickListener composeClickListener;
    private final boolean showHeader;
    private String myAvatarUrl = null;

    public void setMyAvatarUrl(String url) {
        this.myAvatarUrl = url;
        notifyItemChanged(0);
    }

    public void setSuggestions(List<JsonObject> suggestions) {
        this.suggestions = suggestions;
        notifyDataSetChanged();
    }

    public PostsAdapter(Context context, List<JsonObject> posts, List<JsonObject> suggestions,
                        OnLikeClickListener likeListener, boolean showHeader) {
        this.context = context;
        this.posts = posts;
        this.suggestions = suggestions;
        this.likeListener = likeListener;
        this.showHeader = showHeader;
    }

    /** Alternative constructor used by FeedFragment (no suggestions, with compose callback). */
    public PostsAdapter(List<JsonObject> posts, Context context, OnComposeClickListener composeClickListener) {
        this.context = context;
        this.posts = posts;
        this.suggestions = null;
        this.likeListener = null;
        this.composeClickListener = composeClickListener;
        this.showHeader = true;
    }

    public void setLikeListener(OnLikeClickListener listener)    { this.likeListener = listener; }
    public void setCommentListener(OnCommentClickListener l)     { this.commentListener = l; }
    public void setRepostListener(OnRepostClickListener l)       { this.repostListener = l; }
    public void setComposeClickListener(OnComposeClickListener l){ this.composeClickListener = l; }

    @Override
    public int getItemViewType(int position) {
        if (showHeader && position == 0) return TYPE_HEADER;
        if (suggestions != null && !suggestions.isEmpty() && position == 2) return TYPE_SUGGESTIONS;
        return TYPE_POST;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_feed_header, parent, false);
            return new HeaderViewHolder(view);
        } else if (viewType == TYPE_SUGGESTIONS) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_suggestions_list, parent, false);
            return new SuggestionsViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_post_card, parent, false);
            return new PostViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            bindHeader((HeaderViewHolder) holder);
        } else if (holder instanceof SuggestionsViewHolder) {
            bindSuggestions((SuggestionsViewHolder) holder);
        } else {
            // Calculate correct index in 'posts' list
            int offset = 0;
            if (showHeader) offset++;
            if (suggestions != null && !suggestions.isEmpty() && position > 2) offset++;
            
            int dataPos = position - offset;
            if (dataPos >= 0 && dataPos < posts.size()) {
                bindPost((PostViewHolder) holder, dataPos);
            }
        }
    }

    private void bindHeader(HeaderViewHolder holder) {
        String name = SharedPrefManager.getInstance(context).getUserName();
        
        if (myAvatarUrl != null && !myAvatarUrl.isEmpty()) {
            holder.tvAvatar.setVisibility(View.GONE);
            holder.ivHeaderAvatar.setVisibility(View.VISIBLE);
            Glide.with(context)
                .load(myAvatarUrl)
                .centerCrop()
                .into(holder.ivHeaderAvatar);
        } else {
            holder.tvAvatar.setVisibility(View.VISIBLE);
            holder.ivHeaderAvatar.setVisibility(View.GONE);
            String initials = "";
            String[] parts = name.trim().split(" ");
            if (parts.length > 0 && !parts[0].isEmpty()) initials += parts[0].charAt(0);
            if (parts.length > 1 && !parts[1].isEmpty()) initials += parts[1].charAt(0);
            holder.tvAvatar.setText(initials.toUpperCase());
        }

        View.OnClickListener composeListener = v -> {
            if (composeClickListener != null) {
                composeClickListener.onComposeClick();
            }
        };
        holder.btnStartPost.setOnClickListener(composeListener);
        if (holder.btnHeaderPhoto != null) {
            holder.btnHeaderPhoto.setOnClickListener(composeListener);
        }
        if (holder.btnHeaderVideo != null) {
            holder.btnHeaderVideo.setOnClickListener(composeListener);
        }
        if (holder.btnHeaderArticle != null) {
            holder.btnHeaderArticle.setOnClickListener(composeListener);
        }
    }

    private void bindPost(PostViewHolder holder, int position) {
        JsonObject post  = posts.get(position);
        final JsonObject author = post.has("author") && !post.get("author").isJsonNull()
                ? post.getAsJsonObject("author") : new JsonObject();

        String firstName = getString(author, "first_name");
        String lastName  = getString(author, "last_name");
        String rawName  = (firstName + " " + lastName).trim();
        final String fullName = rawName.isEmpty() ? "Campus Member" : rawName;
        holder.tvAuthorName.setText(fullName);

        String avatarUrl = getString(author, "avatar");
        if (!avatarUrl.isEmpty()) {
            holder.tvAvatar.setVisibility(View.GONE);
            holder.ivAuthorAvatar.setVisibility(View.VISIBLE);
            Glide.with(context)
                .load(avatarUrl)
                .centerCrop()
                .into(holder.ivAuthorAvatar);
        } else {
            holder.tvAvatar.setVisibility(View.VISIBLE);
            holder.ivAuthorAvatar.setVisibility(View.GONE);
            String initials = "";
            if (!firstName.isEmpty()) initials += firstName.charAt(0);
            if (!lastName.isEmpty())  initials += lastName.charAt(0);
            holder.tvAvatar.setText(initials.toUpperCase(Locale.getDefault()));

            String colorHex = getString(author, "avatar_color");
            try {
                Drawable bg = holder.tvAvatar.getBackground().mutate();
                bg.setColorFilter(Color.parseColor(colorHex.isEmpty() ? "#0A66C2" : colorHex),
                        PorterDuff.Mode.SRC_IN);
                holder.tvAvatar.setBackground(bg);
            } catch (Exception ignored) {}
        }

        String uni  = getString(author, "university");
        String role = getString(author, "role");
        if (role.isEmpty()) role = getString(post, "post_type");
        String roleLabel = getRoleLabel(role);
        holder.tvUniversity.setText(uni.isEmpty() ? roleLabel : uni + " · " + roleLabel);

        holder.tvTimestamp.setText(formatRelativeTime(getString(post, "created_at")));
        applyRoleChip(holder.chipRole, role);
        holder.tvContent.setText(getString(post, "content"));

        String imageUrl = getString(post, "image_url");
        String fileUrl  = getString(post, "file_url");
        boolean isRepost = post.has("repost_of_detail") && !post.get("repost_of_detail").isJsonNull();
        if (holder.flPostMediaContainer != null) {
            holder.flPostMediaContainer.setVisibility(View.GONE);
        }
        if (holder.llPostFileContainer != null) {
            holder.llPostFileContainer.setVisibility(View.GONE);
        }

        if (!isRepost) {
            String targetMediaUrl = "";
            boolean isVideo = false;
            boolean isImage = false;
            boolean isDocument = false;

            if (!fileUrl.isEmpty()) {
                targetMediaUrl = fileUrl;
                if (isVideoFile(fileUrl)) {
                    isVideo = true;
                } else if (isImageFile(fileUrl)) {
                    isImage = true;
                } else {
                    isDocument = true;
                }
            } else if (!imageUrl.isEmpty()) {
                targetMediaUrl = imageUrl;
                if (isVideoFile(imageUrl)) {
                    isVideo = true;
                } else if (isImageFile(imageUrl)) {
                    isImage = true;
                } else {
                    if (imageUrl.toLowerCase().endsWith(".pdf") || imageUrl.toLowerCase().endsWith(".doc") || 
                        imageUrl.toLowerCase().endsWith(".docx") || imageUrl.toLowerCase().endsWith(".xls") || 
                        imageUrl.toLowerCase().endsWith(".xlsx")) {
                        isDocument = true;
                    } else {
                        isImage = true;
                    }
                }
            }

            if (!targetMediaUrl.isEmpty()) {
                final String finalUrl = targetMediaUrl;
                if (isImage) {
                    if (holder.flPostMediaContainer != null) {
                        holder.flPostMediaContainer.setVisibility(View.VISIBLE);
                        holder.ivPostImage.setVisibility(View.VISIBLE);
                        if (holder.ivPlayButton != null) {
                            holder.ivPlayButton.setVisibility(View.GONE);
                        }
                        Glide.with(context)
                                .load(targetMediaUrl)
                                .placeholder(android.R.color.darker_gray)
                                .error(android.R.drawable.ic_menu_gallery)
                                .centerCrop()
                                .into(holder.ivPostImage);
                        holder.ivPostImage.setOnClickListener(v -> showFullScreenMedia(finalUrl, false));
                    }
                } else if (isVideo) {
                    if (holder.flPostMediaContainer != null) {
                        holder.flPostMediaContainer.setVisibility(View.VISIBLE);
                        holder.ivPostImage.setVisibility(View.VISIBLE);
                        if (holder.ivPlayButton != null) {
                            holder.ivPlayButton.setVisibility(View.VISIBLE);
                            holder.ivPlayButton.setOnClickListener(v -> showFullScreenMedia(finalUrl, true));
                        }
                        Glide.with(context)
                                .asBitmap()
                                .load(targetMediaUrl)
                                .placeholder(android.R.color.darker_gray)
                                .error(android.R.drawable.ic_menu_gallery)
                                .override(300, 300)
                                .centerCrop()
                                .into(holder.ivPostImage);
                        holder.ivPostImage.setOnClickListener(v -> showFullScreenMedia(finalUrl, true));
                    }
                } else if (isDocument) {
                    if (holder.llPostFileContainer != null) {
                        holder.llPostFileContainer.setVisibility(View.VISIBLE);
                        String fileName = targetMediaUrl.substring(targetMediaUrl.lastIndexOf('/') + 1);
                        try {
                            fileName = java.net.URLDecoder.decode(fileName, "UTF-8");
                        } catch (Exception ignored) {}
                        if (holder.tvPostFileName != null) {
                            holder.tvPostFileName.setText(fileName);
                        }
                        holder.llPostFileContainer.setOnClickListener(v -> {
                            try {
                                android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
                                intent.setData(android.net.Uri.parse(finalUrl));
                                context.startActivity(intent);
                            } catch (Exception e) {
                                Toast.makeText(context, "Cannot open attachment", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }
        }

        int likesCount = post.has("likes_count") ? post.get("likes_count").getAsInt() : 0;
        boolean isLiked = post.has("is_liked_by_me") && post.get("is_liked_by_me").getAsBoolean();
        holder.tvLikesCount.setText(likesCount > 0 ? String.valueOf(likesCount) : "");
        holder.llLikeSummary.setVisibility(likesCount > 0 ? View.VISIBLE : View.GONE);

        updateLikeButton(holder.ibLikeIcon, holder.tvLikeLabel, isLiked);

        View.OnClickListener profileClick = v -> {
            try {
                int authorId = author.has("id") ? author.get("id").getAsInt() : -1;
                String authorName = fullName;
                Bundle args = new Bundle();
                args.putInt("userId", authorId);
                args.putString("userName", authorName);
                androidx.navigation.Navigation.findNavController(v).navigate(R.id.profileDetailFragment, args);
            } catch (Exception ignored) {}
        };

        holder.tvAvatar.setOnClickListener(profileClick);
        holder.tvAuthorName.setOnClickListener(profileClick);

        final int postId = post.has("id") ? post.get("id").getAsInt() : -1;

        holder.btnLikeRow.setOnClickListener(v -> {
            if (likeListener != null) {
                // Re-read current like state from the post object to avoid stale captures
                boolean currentlyLiked = post.has("is_liked_by_me") && post.get("is_liked_by_me").getAsBoolean();
                likeListener.onLikeClick(postId, currentlyLiked, holder.getAdapterPosition());
            }
        });

        // Comment Action
        holder.btnCommentRow.setOnClickListener(v -> {
            if (commentListener != null) {
                commentListener.onCommentClick(postId, holder.getAdapterPosition());
            } else {
                showCommentsDialog(postId);
            }
        });

        // Repost Action
        holder.btnRepostRow.setOnClickListener(v -> {
            if (repostListener != null) {
                repostListener.onRepostClick(postId, holder.getAdapterPosition());
            } else {
                showRepostDialog(postId);
            }
        });

        // Handle Repost Display
        if (post.has("repost_of_detail") && !post.get("repost_of_detail").isJsonNull()) {
            holder.llRepostContainer.setVisibility(View.VISIBLE);
            JsonObject original = post.getAsJsonObject("repost_of_detail");
            JsonObject origAuthor = original.has("author") && !original.get("author").isJsonNull()
                    ? original.getAsJsonObject("author") : new JsonObject();
            holder.tvRepostAuthor.setText(getString(origAuthor, "first_name") + " " + getString(origAuthor, "last_name"));
            holder.tvRepostContent.setText(getString(original, "content"));

            // Show media for original post if exists
            String origImageUrl = getString(original, "image_url");
            String origFileUrl  = getString(original, "file_url");
            if (holder.flRepostMediaContainer != null) {
                String targetOrigUrl = "";
                boolean origIsVideo = false;
                boolean origIsImage = false;
                boolean origIsDocument = false;

                if (!origFileUrl.isEmpty()) {
                    targetOrigUrl = origFileUrl;
                    if (isVideoFile(origFileUrl)) {
                        origIsVideo = true;
                    } else if (isImageFile(origFileUrl)) {
                        origIsImage = true;
                    } else {
                        origIsDocument = true;
                    }
                } else if (!origImageUrl.isEmpty()) {
                    targetOrigUrl = origImageUrl;
                    if (isVideoFile(origImageUrl)) {
                        origIsVideo = true;
                    } else if (isImageFile(origImageUrl)) {
                        origIsImage = true;
                    } else {
                        if (origImageUrl.toLowerCase().endsWith(".pdf") || origImageUrl.toLowerCase().endsWith(".doc") || 
                            origImageUrl.toLowerCase().endsWith(".docx") || origImageUrl.toLowerCase().endsWith(".xls") || 
                            origImageUrl.toLowerCase().endsWith(".xlsx")) {
                            origIsDocument = true;
                        } else {
                            origIsImage = true;
                        }
                    }
                }

                if (!targetOrigUrl.isEmpty()) {
                    holder.flRepostMediaContainer.setVisibility(View.VISIBLE);
                    final String finalOrigUrl = targetOrigUrl;
                    if (origIsImage) {
                        if (holder.ivRepostMedia != null) {
                            holder.ivRepostMedia.setVisibility(View.VISIBLE);
                            Glide.with(context)
                                    .load(targetOrigUrl)
                                    .placeholder(android.R.color.darker_gray)
                                    .error(android.R.drawable.ic_menu_gallery)
                                    .centerCrop()
                                    .into(holder.ivRepostMedia);
                            holder.ivRepostMedia.setOnClickListener(v -> showFullScreenMedia(finalOrigUrl, false));
                        }
                        if (holder.ivRepostPlayButton != null) {
                            holder.ivRepostPlayButton.setVisibility(View.GONE);
                        }
                    } else if (origIsVideo) {
                        if (holder.ivRepostMedia != null) {
                            holder.ivRepostMedia.setVisibility(View.VISIBLE);
                            Glide.with(context)
                                    .asBitmap()
                                    .load(targetOrigUrl)
                                    .placeholder(android.R.color.darker_gray)
                                    .error(android.R.drawable.ic_menu_gallery)
                                    .override(300, 300)
                                    .centerCrop()
                                    .into(holder.ivRepostMedia);
                            holder.ivRepostMedia.setOnClickListener(v -> showFullScreenMedia(finalOrigUrl, true));
                        }
                        if (holder.ivRepostPlayButton != null) {
                            holder.ivRepostPlayButton.setVisibility(View.VISIBLE);
                            holder.ivRepostPlayButton.setOnClickListener(v -> showFullScreenMedia(finalOrigUrl, true));
                        }
                    } else if (origIsDocument) {
                        if (holder.ivRepostMedia != null) {
                            holder.ivRepostMedia.setVisibility(View.VISIBLE);
                            holder.ivRepostMedia.setImageResource(android.R.drawable.ic_menu_save);
                            holder.ivRepostMedia.setOnClickListener(v -> {
                                String fileName = finalOrigUrl.substring(finalOrigUrl.lastIndexOf('/') + 1);
                                Toast.makeText(context, "Attachment: " + fileName, Toast.LENGTH_SHORT).show();
                            });
                        }
                        if (holder.ivRepostPlayButton != null) {
                            holder.ivRepostPlayButton.setVisibility(View.GONE);
                        }
                    }
                } else {
                    holder.flRepostMediaContainer.setVisibility(View.GONE);
                }
            }
        } else {
            holder.llRepostContainer.setVisibility(View.GONE);
            if (holder.flRepostMediaContainer != null) {
                holder.flRepostMediaContainer.setVisibility(View.GONE);
            }
        }

        // Handle Comment Preview
        if (post.has("first_comment") && !post.get("first_comment").isJsonNull()) {
            holder.llCommentPreview.setVisibility(View.VISIBLE);
            JsonObject firstComment = post.getAsJsonObject("first_comment");
            JsonObject commentAuthor = firstComment.getAsJsonObject("author");
            
            String cAuthorName = getString(commentAuthor, "first_name") + " " + getString(commentAuthor, "last_name");
            holder.tvCommentAuthorName.setText(cAuthorName);
            holder.tvCommentContentPreview.setText(getString(firstComment, "content"));
            
            String cAvatarUrl = getString(commentAuthor, "avatar");
            if (!cAvatarUrl.isEmpty()) {
                Glide.with(context).load(cAvatarUrl).centerCrop().into(holder.ivCommentAuthorAvatar);
            } else {
                holder.ivCommentAuthorAvatar.setImageResource(R.drawable.bg_avatar_circle);
            }
            
            int totalComments = post.has("comments_count") ? post.get("comments_count").getAsInt() : 0;
            if (totalComments > 1) {
                holder.tvViewMoreComments.setVisibility(View.VISIBLE);
                holder.tvViewMoreComments.setText("View all " + totalComments + " comments");
            } else {
                holder.tvViewMoreComments.setVisibility(View.GONE);
            }
            
            View.OnClickListener openComments = v -> {
                showCommentsDialog(postId);
            };
            holder.tvViewMoreComments.setOnClickListener(openComments);
            holder.llCommentPreview.setOnClickListener(openComments);
        } else {
            holder.llCommentPreview.setVisibility(View.GONE);
        }
    }

    private void showCommentsDialog(int postId) {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = 
            new com.google.android.material.bottomsheet.BottomSheetDialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_comments, null);
        dialog.setContentView(view);

        RecyclerView rvComments = view.findViewById(R.id.rv_comments);
        android.widget.EditText etComment = view.findViewById(R.id.et_comment);
        android.widget.ImageButton btnSend = view.findViewById(R.id.btn_send_comment);

        java.util.List<JsonObject> commentList = new java.util.ArrayList<>();
        CommentsAdapter adapter = new CommentsAdapter(commentList);
        rvComments.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(context));
        rvComments.setAdapter(adapter);

        String token = SharedPrefManager.getInstance(context).getToken();
        
        // Fetch comments
        RetrofitClient.getInstance(token).getApi().getPostComments(postId)
            .enqueue(new retrofit2.Callback<java.util.List<JsonObject>>() {
                @Override
                public void onResponse(retrofit2.Call<java.util.List<JsonObject>> call, retrofit2.Response<java.util.List<JsonObject>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        commentList.addAll(response.body());
                        adapter.notifyDataSetChanged();
                    }
                }
                @Override
                public void onFailure(retrofit2.Call<java.util.List<JsonObject>> call, Throwable t) {}
            });

        // Send comment
        btnSend.setOnClickListener(v -> {
            String content = etComment.getText().toString().trim();
            if (content.isEmpty()) return;

            JsonObject body = new JsonObject();
            body.addProperty("content", content);

            RetrofitClient.getInstance(token).getApi().addComment(postId, body)
                .enqueue(new retrofit2.Callback<JsonObject>() {
                    @Override
                    public void onResponse(@NonNull retrofit2.Call<JsonObject> call, @NonNull retrofit2.Response<JsonObject> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            commentList.add(response.body());
                            adapter.notifyItemInserted(commentList.size() - 1);
                            rvComments.scrollToPosition(commentList.size() - 1);
                            etComment.setText("");
                            Toast.makeText(context, "Comment posted!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, "Error: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull retrofit2.Call<JsonObject> call, @NonNull Throwable t) {
                        Toast.makeText(context, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
        });

        dialog.show();
    }

    private void showRepostDialog(int postId) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
                .setTitle("Repost this?")
                .setMessage("Share this post with your followers.")
                .setPositiveButton("Repost", (d, w) -> {
                    String token = SharedPrefManager.getInstance(context).getToken();
                    JsonObject body = new JsonObject();
                    body.addProperty("content", "Shared this post.");
                    com.smartcampus.manouba.network.RetrofitClient.getInstance(token).getApi().repost(postId, body)
                            .enqueue(new retrofit2.Callback<JsonObject>() {
                        @Override
                        public void onResponse(retrofit2.Call<JsonObject> call, retrofit2.Response<JsonObject> response) {
                            if (response.isSuccessful()) Toast.makeText(context, "Reposted!", Toast.LENGTH_SHORT).show();
                        }
                        @Override
                        public void onFailure(retrofit2.Call<JsonObject> call, Throwable t) {}
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public void toggleLike(int position, boolean liked, int newCount) {
        int dataPos = showHeader ? position - 1 : position;
        if (dataPos < 0 || dataPos >= posts.size()) return;
        JsonObject post = posts.get(dataPos);
        post.addProperty("is_liked_by_me", liked);
        post.addProperty("likes_count", newCount);
        notifyItemChanged(position);
    }

    private void bindSuggestions(SuggestionsViewHolder holder) {
        SuggestionsAdapter innerAdapter = new SuggestionsAdapter(context, suggestions);
        holder.rvSuggestions.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(context, 
                androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false));
        holder.rvSuggestions.setAdapter(innerAdapter);
    }

    @Override
    public int getItemCount() {
        int count = posts.size();
        if (showHeader) count++;
        if (suggestions != null && !suggestions.isEmpty()) count++;
        return count;
    }

    private static String getString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return "";
        return obj.get(key).getAsString();
    }

    private String getRoleLabel(String role) {
        switch (role) {
            case "org":   return "Organization";
            case "admin": return "Administration";
            default:      return "Student";
        }
    }

    private void applyRoleChip(Chip chip, String role) {
        switch (role) {
            case "org":
                chip.setText("Org");
                chip.setChipBackgroundColorResource(R.color.cat_clubs);
                chip.setTextColor(Color.WHITE);
                break;
            case "admin":
                chip.setText("Admin");
                chip.setChipBackgroundColorResource(R.color.cat_administration);
                chip.setTextColor(Color.WHITE);
                break;
            default:
                chip.setText("Student");
                chip.setChipBackgroundColorResource(R.color.cat_services);
                chip.setTextColor(Color.WHITE);
                break;
        }
    }

    private void updateLikeButton(ImageView icon, TextView label, boolean liked) {
        if (liked) {
            icon.setImageResource(android.R.drawable.btn_star_big_on);
            icon.setColorFilter(Color.parseColor("#0A66C2"));
            label.setTextColor(Color.parseColor("#0A66C2"));
            label.setText(" Like");
        } else {
            icon.setImageResource(android.R.drawable.btn_star_big_off);
            icon.setColorFilter(Color.parseColor("#666666"));
            label.setTextColor(Color.parseColor("#666666"));
            label.setText(" Like");
        }
    }

    private String formatRelativeTime(String isoTime) {
        if (isoTime == null || isoTime.isEmpty()) return "";
        // Try multiple ISO 8601 formats Django might emit
        String[] formats = {
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                "yyyy-MM-dd HH:mm:ss"
        };
        for (String fmt : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(fmt, Locale.US);
                sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                Date date = sdf.parse(isoTime);
                if (date == null) continue;
                long diffMs = System.currentTimeMillis() - date.getTime();
                long mins  = TimeUnit.MILLISECONDS.toMinutes(diffMs);
                long hours = TimeUnit.MILLISECONDS.toHours(diffMs);
                long days  = TimeUnit.MILLISECONDS.toDays(diffMs);
                if (mins  < 1)  return "Just now";
                if (mins  < 60) return mins  + "m ago";
                if (hours < 24) return hours + "h ago";
                if (days  < 7)  return days  + "d ago";
                return new SimpleDateFormat("MMM d", Locale.getDefault()).format(date);
            } catch (ParseException ignored) {}
        }
        return "";
    }

    private boolean isVideoFile(String url) {
        if (url == null || url.isEmpty()) return false;
        String lower = url.toLowerCase(Locale.getDefault());
        return lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".webm") ||
               lower.endsWith(".3gp") || lower.endsWith(".avi") || lower.contains("/video/");
    }

    private boolean isImageFile(String url) {
        if (url == null || url.isEmpty()) return false;
        String lower = url.toLowerCase(Locale.getDefault());
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") ||
               lower.endsWith(".gif") || lower.endsWith(".webp") || lower.endsWith(".bmp") ||
               lower.contains("/posts/post_image_") || lower.contains("/media/posts/");
    }

    private void showFullScreenMedia(String mediaUrl, boolean isVideo) {
        if (mediaUrl == null || mediaUrl.isEmpty()) return;

        android.app.Dialog dialog = new android.app.Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_fullscreen_media);

        ImageView ivFullscreenImage = dialog.findViewById(R.id.iv_fullscreen_image);
        android.widget.VideoView vvFullscreenVideo = dialog.findViewById(R.id.vv_fullscreen_video);
        android.widget.ProgressBar pbLoading = dialog.findViewById(R.id.pb_loading);
        ImageView ivClose = dialog.findViewById(R.id.iv_close_fullscreen);

        ivClose.setOnClickListener(v -> dialog.dismiss());

        if (isVideo) {
            vvFullscreenVideo.setVisibility(View.VISIBLE);
            ivFullscreenImage.setVisibility(View.GONE);
            pbLoading.setVisibility(View.VISIBLE);

            try {
                android.net.Uri uri = android.net.Uri.parse(mediaUrl);
                vvFullscreenVideo.setVideoURI(uri);

                android.widget.MediaController mediaController = new android.widget.MediaController(context);
                mediaController.setAnchorView(vvFullscreenVideo);
                vvFullscreenVideo.setMediaController(mediaController);

                vvFullscreenVideo.setOnPreparedListener(mp -> {
                    pbLoading.setVisibility(View.GONE);
                    vvFullscreenVideo.start();
                });

                vvFullscreenVideo.setOnErrorListener((mp, what, extra) -> {
                    pbLoading.setVisibility(View.GONE);
                    Toast.makeText(context, "Error playing video", Toast.LENGTH_SHORT).show();
                    return true;
                });
            } catch (Exception e) {
                pbLoading.setVisibility(View.GONE);
                Toast.makeText(context, "Invalid video path", Toast.LENGTH_SHORT).show();
            }
        } else {
            vvFullscreenVideo.setVisibility(View.GONE);
            ivFullscreenImage.setVisibility(View.VISIBLE);
            pbLoading.setVisibility(View.VISIBLE);

            Glide.with(context)
                    .load(mediaUrl)
                    .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(@androidx.annotation.Nullable com.bumptech.glide.load.engine.GlideException e,
                                                    Object model,
                                                    com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                                    boolean isFirstResource) {
                            pbLoading.setVisibility(View.GONE);
                            Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show();
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource,
                                                       Object model,
                                                       com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                                       com.bumptech.glide.load.DataSource dataSource,
                                                       boolean isFirstResource) {
                            pbLoading.setVisibility(View.GONE);
                            return false;
                        }
                    })
                    .into(ivFullscreenImage);
        }

        dialog.show();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar;
        ImageView ivHeaderAvatar;
        MaterialButton btnStartPost;
        View btnHeaderPhoto, btnHeaderVideo, btnHeaderArticle;
        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tv_header_avatar);
            ivHeaderAvatar = itemView.findViewById(R.id.iv_header_avatar);
            btnStartPost = itemView.findViewById(R.id.btn_start_post);
            btnHeaderPhoto = itemView.findViewById(R.id.btn_header_photo);
            btnHeaderVideo = itemView.findViewById(R.id.btn_header_video);
            btnHeaderArticle = itemView.findViewById(R.id.btn_header_article);
        }
    }

    static class SuggestionsViewHolder extends RecyclerView.ViewHolder {
        RecyclerView rvSuggestions;
        SuggestionsViewHolder(@NonNull View itemView) {
            super(itemView);
            rvSuggestions = itemView.findViewById(R.id.rv_suggestions);
        }
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView   tvAvatar, tvAuthorName, tvUniversity, tvTimestamp, tvContent, tvLikesCount, tvLikeLabel;
        TextView   tvRepostAuthor, tvRepostContent;
        Chip       chipRole;
        ImageView  ivPostImage, ivAuthorAvatar, ivCommentAuthorAvatar, ivRepostMedia, ibLikeIcon;
        LinearLayout btnLikeRow, btnCommentRow, btnRepostRow, llLikeSummary, llRepostContainer, llCommentPreview;
        TextView   tvCommentAuthorName, tvCommentContentPreview, tvViewMoreComments;
        View       flPostMediaContainer, flRepostMediaContainer;
        ImageView  ivPlayButton, ivRepostPlayButton;
        LinearLayout llPostFileContainer;
        TextView   tvPostFileName;

        PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar       = itemView.findViewById(R.id.tv_avatar);
            ivAuthorAvatar = itemView.findViewById(R.id.iv_author_avatar);
            tvAuthorName   = itemView.findViewById(R.id.tv_author_name);
            tvUniversity   = itemView.findViewById(R.id.tv_university);
            tvTimestamp    = itemView.findViewById(R.id.tv_timestamp);
            tvContent      = itemView.findViewById(R.id.tv_content);
            tvLikesCount   = itemView.findViewById(R.id.tv_likes_count);
            tvLikeLabel    = itemView.findViewById(R.id.tv_like_label);
            chipRole       = itemView.findViewById(R.id.chip_role);
            ivPostImage    = itemView.findViewById(R.id.iv_post_image);
            ibLikeIcon     = itemView.findViewById(R.id.ib_like_icon);
            btnLikeRow     = itemView.findViewById(R.id.btn_like);
            btnCommentRow  = itemView.findViewById(R.id.btn_comment);
            btnRepostRow   = itemView.findViewById(R.id.btn_repost);
            llLikeSummary  = itemView.findViewById(R.id.ll_like_summary);
            
            llRepostContainer = itemView.findViewById(R.id.ll_repost_container);
            tvRepostAuthor    = itemView.findViewById(R.id.tv_repost_author);
            tvRepostContent   = itemView.findViewById(R.id.tv_repost_content);
            ivRepostMedia     = itemView.findViewById(R.id.iv_repost_media);

            llCommentPreview        = itemView.findViewById(R.id.ll_comment_preview);
            ivCommentAuthorAvatar   = itemView.findViewById(R.id.iv_comment_author_avatar);
            tvCommentAuthorName     = itemView.findViewById(R.id.tv_comment_author_name);
            tvCommentContentPreview = itemView.findViewById(R.id.tv_comment_content_preview);
            tvViewMoreComments      = itemView.findViewById(R.id.tv_view_more_comments);

            flPostMediaContainer   = itemView.findViewById(R.id.fl_post_media_container);
            flRepostMediaContainer = itemView.findViewById(R.id.fl_repost_media_container);
            ivPlayButton           = itemView.findViewById(R.id.iv_play_button);
            ivRepostPlayButton     = itemView.findViewById(R.id.iv_repost_play_button);
            llPostFileContainer    = itemView.findViewById(R.id.ll_post_file_container);
            tvPostFileName         = itemView.findViewById(R.id.tv_post_file_name);
        }
    }
}

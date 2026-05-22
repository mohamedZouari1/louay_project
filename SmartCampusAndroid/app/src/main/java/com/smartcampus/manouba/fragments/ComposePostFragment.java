package com.smartcampus.manouba.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonObject;
import com.smartcampus.manouba.R;
import com.smartcampus.manouba.network.RetrofitClient;
import com.smartcampus.manouba.utils.SharedPrefManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ComposePostFragment extends Fragment {

    private TextInputEditText etContent;
    private MaterialButton btnPublish;
    private View btnAttach;
    private View btnAttachFile;
    private View btnAttachVideo;
    private ImageView ivPreview;
    private ImageButton btnRemoveImage;
    private View imagePreviewContainer;
    private TextView tvFileAttached;
    private TextView tvRecordingStatus;
    private View filePreviewContainer;
    private ImageView ivFileIcon;
    private ImageButton btnRemoveFile;

    private Uri selectedImageUri = null;
    private File selectedFileAttachment = null;
    private String selectedFileName = null;
    private boolean isFileAttached = false;

    // Vocal recording
    private MediaRecorder mediaRecorder;
    private File audioFile;
    private boolean isRecording = false;
    private Handler recordingTimerHandler;
    private Runnable recordingTimerRunnable;
    private int recordingSeconds = 0;

    // Image picker
    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    clearFileAttachment();
                    clearAudio();
                    imagePreviewContainer.setVisibility(View.VISIBLE);
                    if (tvFileAttached != null) tvFileAttached.setVisibility(View.GONE);
                    Glide.with(this)
                            .load(selectedImageUri)
                            .placeholder(R.color.surface)
                            .centerCrop()
                            .into(ivPreview);
                }
            });

    // Video picker
    private final ActivityResultLauncher<Intent> videoPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri videoUri = result.getData().getData();
                    if (videoUri != null) {
                        clearFileAttachment();
                        clearAudio();
                        selectedImageUri = null;
                        if (imagePreviewContainer != null) imagePreviewContainer.setVisibility(View.GONE);
                        handleFilePicked(videoUri);
                    }
                }
            });

    // File picker
    private final ActivityResultLauncher<String[]> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    try {
                        // Persist permission
                        requireContext().getContentResolver()
                                .takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (Exception ignored) {}
                    handleFilePicked(uri);
                }
            });

    // Record audio permission
    private final ActivityResultLauncher<String> audioPermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startRecording();
                } else {
                    Toast.makeText(requireContext(),
                            "Microphone permission is required to record audio.", Toast.LENGTH_SHORT).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_compose_post, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etContent             = view.findViewById(R.id.et_content);
        btnPublish            = view.findViewById(R.id.btn_publish);
        btnAttach             = view.findViewById(R.id.btn_attach);
        btnAttachFile         = view.findViewById(R.id.btn_attach_file);
        btnAttachVideo        = view.findViewById(R.id.btn_attach_video);
        ivPreview             = view.findViewById(R.id.iv_preview);
        btnRemoveImage        = view.findViewById(R.id.btn_remove_image);
        imagePreviewContainer = view.findViewById(R.id.image_preview_container);

        filePreviewContainer = view.findViewById(R.id.file_preview_container);
        ivFileIcon           = view.findViewById(R.id.iv_file_icon);
        tvFileAttached       = view.findViewById(R.id.tv_file_attached);
        btnRemoveFile        = view.findViewById(R.id.btn_remove_file);
        tvRecordingStatus    = null;

        if (btnRemoveFile != null) {
            btnRemoveFile.setOnClickListener(v -> {
                clearFileAttachment();
                Toast.makeText(requireContext(), "Attachment removed", Toast.LENGTH_SHORT).show();
            });
        }

        // ── Author info header ──────────────────────────────────────────────
        TextView tvTopAvatar       = view.findViewById(R.id.tv_top_avatar);
        ImageView ivTopAvatar      = view.findViewById(R.id.iv_top_avatar);
        TextView tvComposeAvatar   = view.findViewById(R.id.tv_compose_avatar);
        ImageView ivComposeAvatar  = view.findViewById(R.id.iv_compose_avatar);
        TextView tvComposeAuthorName = view.findViewById(R.id.tv_compose_author_name);

        String name = SharedPrefManager.getInstance(requireContext()).getUserName();
        if (tvComposeAuthorName != null) tvComposeAuthorName.setText(name);

        String initials = "";
        String[] parts = name.trim().split(" ");
        if (parts.length > 0 && !parts[0].isEmpty()) initials += parts[0].charAt(0);
        if (parts.length > 1 && !parts[1].isEmpty()) initials += parts[1].charAt(0);
        String finalInitials = initials.toUpperCase();
        if (tvTopAvatar != null)    tvTopAvatar.setText(finalInitials);
        if (tvComposeAvatar != null) tvComposeAvatar.setText(finalInitials);

        loadMyProfile(tvTopAvatar, ivTopAvatar, tvComposeAvatar, ivComposeAvatar);

        // ── Button listeners ────────────────────────────────────────────────
        if (btnAttach != null) btnAttach.setOnClickListener(v -> openImagePicker());

        if (btnAttachFile != null) btnAttachFile.setOnClickListener(v -> openFilePicker());

        if (btnAttachVideo != null) btnAttachVideo.setOnClickListener(v -> openVideoPicker());

        if (btnRemoveImage != null) {
            btnRemoveImage.setOnClickListener(v -> {
                selectedImageUri = null;
                if (imagePreviewContainer != null) imagePreviewContainer.setVisibility(View.GONE);
                Toast.makeText(requireContext(), getString(R.string.photo_removed), Toast.LENGTH_SHORT).show();
            });
        }

        btnPublish.setOnClickListener(v -> publishPost());
    }

    // ── Profile loading ─────────────────────────────────────────────────────

    private void loadMyProfile(TextView tv1, ImageView iv1, TextView tv2, ImageView iv2) {
        String token = SharedPrefManager.getInstance(requireContext()).getToken();
        RetrofitClient.getInstance(token).getApi().getProfile()
            .enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                    if (!isAdded() || !response.isSuccessful() || response.body() == null) return;
                    JsonObject body = response.body();
                    // getProfile() returns the user object directly (not nested under "user")
                    JsonObject profile = body.has("profile") && !body.get("profile").isJsonNull()
                            ? body.getAsJsonObject("profile") : null;
                    String avatarUrl = null;
                    if (profile != null && profile.has("avatar") && !profile.get("avatar").isJsonNull()) {
                        avatarUrl = profile.get("avatar").getAsString();
                    }
                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        String finalAvatarUrl = avatarUrl;
                        if (tv1 != null) tv1.setVisibility(View.GONE);
                        if (iv1 != null) {
                            iv1.setVisibility(View.VISIBLE);
                            Glide.with(ComposePostFragment.this).load(finalAvatarUrl).centerCrop().into(iv1);
                        }
                        if (tv2 != null) tv2.setVisibility(View.GONE);
                        if (iv2 != null) {
                            iv2.setVisibility(View.VISIBLE);
                            Glide.with(ComposePostFragment.this).load(finalAvatarUrl).centerCrop().into(iv2);
                        }
                    }
                }
                @Override
                public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {}
            });
    }

    // ── Image picker ────────────────────────────────────────────────────────

    private void openImagePicker() {
        clearFileAttachment();
        clearAudio();
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    // ── Video picker ────────────────────────────────────────────────────────

    private void openVideoPicker() {
        clearFileAttachment();
        clearAudio();
        selectedImageUri = null;
        if (imagePreviewContainer != null) imagePreviewContainer.setVisibility(View.GONE);
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        intent.setType("video/*");
        videoPickerLauncher.launch(intent);
    }

    // ── File picker ─────────────────────────────────────────────────────────

    private void openFilePicker() {
        clearFileAttachment();
        clearAudio();
        selectedImageUri = null;
        if (imagePreviewContainer != null) imagePreviewContainer.setVisibility(View.GONE);
        filePickerLauncher.launch(new String[]{"*/*"});
    }

    private void handleFilePicked(Uri uri) {
        selectedImageUri = null;
        if (imagePreviewContainer != null) {
            imagePreviewContainer.setVisibility(View.GONE);
        }
        try {
            // Get display name and size
            String displayName = "attachment";
            long fileSize = -1;
            try (android.database.Cursor cursor = requireContext().getContentResolver().query(
                    uri, new String[]{
                            android.provider.OpenableColumns.DISPLAY_NAME,
                            android.provider.OpenableColumns.SIZE
                    }, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        displayName = cursor.getString(nameIndex);
                    }
                    int sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
                    if (sizeIndex != -1) {
                        fileSize = cursor.getLong(sizeIndex);
                    }
                }
            }

            // Determine if it is a video
            String mimeType = requireContext().getContentResolver().getType(uri);
            boolean isVideo = (mimeType != null && mimeType.startsWith("video/")) ||
                    displayName.toLowerCase().endsWith(".mp4") ||
                    displayName.toLowerCase().endsWith(".mkv") ||
                    displayName.toLowerCase().endsWith(".webm") ||
                    displayName.toLowerCase().endsWith(".3gp") ||
                    displayName.toLowerCase().endsWith(".avi");

            // Enforce size limits:
            // Video limit: 10MB (10 * 1024 * 1024 bytes)
            // Other file limit: 15MB (15 * 1024 * 1024 bytes)
            if (isVideo) {
                if (fileSize > 10 * 1024 * 1024) {
                    Toast.makeText(requireContext(), "Video exceeds 10MB limit. Please choose a shorter/smaller video.", Toast.LENGTH_LONG).show();
                    clearFileAttachment();
                    return;
                }
            } else {
                if (fileSize > 15 * 1024 * 1024) {
                    Toast.makeText(requireContext(), "File exceeds 15MB limit. Please choose a smaller file.", Toast.LENGTH_LONG).show();
                    clearFileAttachment();
                    return;
                }
            }

            selectedFileName = displayName;
            selectedFileAttachment = uriToFile(uri, "attach_");
            isFileAttached = true;

            if (filePreviewContainer != null) {
                filePreviewContainer.setVisibility(View.VISIBLE);
            }
            if (tvFileAttached != null) {
                tvFileAttached.setVisibility(View.VISIBLE);
                tvFileAttached.setText(displayName);
            }
            if (ivFileIcon != null) {
                if (isVideo) {
                    ivFileIcon.setImageResource(android.R.drawable.ic_media_play);
                } else {
                    ivFileIcon.setImageResource(android.R.drawable.ic_menu_save);
                }
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Could not attach file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void clearFileAttachment() {
        selectedFileAttachment = null;
        selectedFileName = null;
        isFileAttached = false;
        if (filePreviewContainer != null) filePreviewContainer.setVisibility(View.GONE);
        if (tvFileAttached != null) tvFileAttached.setVisibility(View.GONE);
    }

    // ── Vocal recording ─────────────────────────────────────────────────────

    private void toggleRecording() {
        if (!isRecording) {
            // Check permission
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                audioPermLauncher.launch(Manifest.permission.RECORD_AUDIO);
            } else {
                startRecording();
            }
        } else {
            stopRecording();
        }
    }

    private void startRecording() {
        clearFileAttachment();
        selectedImageUri = null;
        if (imagePreviewContainer != null) imagePreviewContainer.setVisibility(View.GONE);

        audioFile = new File(requireContext().getCacheDir(),
                "voice_" + System.currentTimeMillis() + ".m4a");
        try {
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioEncodingBitRate(128000);
            mediaRecorder.setAudioSamplingRate(44100);
            mediaRecorder.setOutputFile(audioFile.getAbsolutePath());
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            recordingSeconds = 0;
            updateRecordingUI(true);
            startRecordingTimer();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Could not start recording: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            cleanupRecorder();
        }
    }

    private void stopRecording() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
            } catch (Exception ignored) {}
            cleanupRecorder();
        }
        stopRecordingTimer();
        isRecording = false;

        if (audioFile != null && audioFile.exists() && audioFile.length() > 0) {
            isFileAttached = true;
            selectedFileAttachment = audioFile;
            selectedFileName = audioFile.getName();
            String duration = formatSeconds(recordingSeconds);
            if (filePreviewContainer != null) {
                filePreviewContainer.setVisibility(View.VISIBLE);
            }
            if (ivFileIcon != null) {
                ivFileIcon.setImageResource(android.R.drawable.ic_btn_speak_now);
            }
            if (tvFileAttached != null) {
                tvFileAttached.setVisibility(View.VISIBLE);
                tvFileAttached.setText("Voice message (" + duration + ")");
            }
        }
        updateRecordingUI(false);
    }

    private void cleanupRecorder() {
        if (mediaRecorder != null) {
            try { mediaRecorder.release(); } catch (Exception ignored) {}
            mediaRecorder = null;
        }
    }

    private void startRecordingTimer() {
        recordingTimerHandler = new Handler(Looper.getMainLooper());
        recordingTimerRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isAdded() || !isRecording) return;
                recordingSeconds++;
                if (tvRecordingStatus != null) {
                    tvRecordingStatus.setText("⏺ Recording " + formatSeconds(recordingSeconds));
                }
                recordingTimerHandler.postDelayed(this, 1000);
            }
        };
        recordingTimerHandler.postDelayed(recordingTimerRunnable, 1000);
    }

    private void stopRecordingTimer() {
        if (recordingTimerHandler != null && recordingTimerRunnable != null) {
            recordingTimerHandler.removeCallbacks(recordingTimerRunnable);
        }
    }

    private void updateRecordingUI(boolean recording) {
        if (tvRecordingStatus != null) {
            tvRecordingStatus.setVisibility(recording ? View.VISIBLE : View.GONE);
            if (recording) tvRecordingStatus.setText("⏺ Recording 0:00");
        }
    }

    private void clearAudio() {
        if (isRecording) stopRecording();
        audioFile = null;
        isFileAttached = false;
        selectedFileAttachment = null;
        selectedFileName = null;
        if (tvFileAttached != null) tvFileAttached.setVisibility(View.GONE);
        updateRecordingUI(false);
    }

    private String formatSeconds(int seconds) {
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    // ── Publish post ────────────────────────────────────────────────────────

    private void publishPost() {
        String content = etContent.getText() != null ? etContent.getText().toString().trim() : "";
        boolean hasAttachment = selectedImageUri != null || (isFileAttached && selectedFileAttachment != null);

        if (TextUtils.isEmpty(content) && !hasAttachment) {
            etContent.setError("Please write something or attach a file.");
            return;
        }

        if (TextUtils.isEmpty(content)) {
            if (selectedImageUri != null) {
                content = "Shared a photo";
            } else if (isFileAttached && selectedFileAttachment != null) {
                String fileName = (selectedFileName != null && !selectedFileName.isEmpty()
                        ? selectedFileName : selectedFileAttachment.getName()).toLowerCase();
                if (fileName.endsWith(".mp4") || fileName.endsWith(".mkv") || fileName.endsWith(".webm") || fileName.endsWith(".3gp") || fileName.endsWith(".avi")) {
                    content = "Shared a video";
                } else if (fileName.endsWith(".m4a") || fileName.endsWith(".mp3") || fileName.endsWith(".aac")) {
                    content = "Shared a voice message";
                } else {
                    content = "Shared a file";
                }
            }
        }

        if (isRecording) stopRecording();

        btnPublish.setEnabled(false);
        btnPublish.setText(R.string.publishing);

        String token = SharedPrefManager.getInstance(requireContext()).getToken();

        if (selectedImageUri != null) {
            publishWithImage(content, token);
        } else if (isFileAttached && selectedFileAttachment != null) {
            publishWithFile(content, token);
        } else {
            publishTextOnly(content, token);
        }
    }

    private void publishWithImage(String content, String token) {
        try {
            File imageFile = uriToFile(selectedImageUri, "post_image_");
            // Use actual MIME type from ContentResolver
            String mimeType = requireContext().getContentResolver().getType(selectedImageUri);
            if (mimeType == null || mimeType.isEmpty()) mimeType = "image/jpeg";

            RequestBody contentBody = RequestBody.create(
                    okhttp3.MediaType.parse("text/plain"), content);
            RequestBody reqFile = RequestBody.create(
                    okhttp3.MediaType.parse(mimeType), imageFile);
            MultipartBody.Part imagePart = MultipartBody.Part.createFormData(
                    "image", imageFile.getName(), reqFile);

            RetrofitClient.getInstance(token).getApi()
                    .createPost(contentBody, imagePart)
                    .enqueue(postCallback);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Could not attach image, posting as text.", Toast.LENGTH_SHORT).show();
            publishTextOnly(content, token);
        }
    }

    private void publishWithFile(String content, String token) {
        try {
            File file = selectedFileAttachment;
            String mimeType = null;
            String fileName = file.getName().toLowerCase();
            if (fileName.endsWith(".mp4")) {
                mimeType = "video/mp4";
            } else if (fileName.endsWith(".mkv")) {
                mimeType = "video/x-matroska";
            } else if (fileName.endsWith(".3gp")) {
                mimeType = "video/3gpp";
            } else if (fileName.endsWith(".webm")) {
                mimeType = "video/webm";
            } else if (fileName.endsWith(".avi")) {
                mimeType = "video/x-msvideo";
            } else if (fileName.endsWith(".m4a") || fileName.endsWith(".mp3") || fileName.endsWith(".aac")) {
                mimeType = "audio/mp4";
            } else {
                int dotIndex = fileName.lastIndexOf('.');
                if (dotIndex > 0) {
                    String ext = fileName.substring(dotIndex + 1);
                    mimeType = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
                }
                if (mimeType == null) {
                    mimeType = "application/octet-stream";
                }
            }

            RequestBody contentBody = RequestBody.create(
                    okhttp3.MediaType.parse("text/plain"), content);
            RequestBody reqFile = RequestBody.create(
                    okhttp3.MediaType.parse(mimeType), file);
            MultipartBody.Part filePart = MultipartBody.Part.createFormData(
                    "file", file.getName(), reqFile);

            RetrofitClient.getInstance(token).getApi()
                    .createPostWithFile(contentBody, filePart)
                    .enqueue(postCallback);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Could not attach file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            resetPublishButton();
        }
    }

    private void publishTextOnly(String content, String token) {
        JsonObject body = new JsonObject();
        body.addProperty("content", content);
        RetrofitClient.getInstance(token).getApi()
                .createTextPost(body)
                .enqueue(postCallback);
    }

    private final Callback<JsonObject> postCallback = new Callback<JsonObject>() {
        @Override
        public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
            if (!isAdded()) return;
            resetPublishButton();

            if (response.isSuccessful()) {
                Toast.makeText(requireContext(), getString(R.string.post_published), Toast.LENGTH_SHORT).show();
                // Clear all inputs
                etContent.setText("");
                selectedImageUri = null;
                clearFileAttachment();
                clearAudio();
                if (imagePreviewContainer != null) imagePreviewContainer.setVisibility(View.GONE);
                // Navigate back to Feed tab and trigger instant refresh
                if (getParentFragment() instanceof SocialHubFragment) {
                    View pagerView = getParentFragment().getView();
                    if (pagerView != null) {
                        androidx.viewpager2.widget.ViewPager2 vp = pagerView.findViewById(R.id.view_pager);
                        if (vp != null) vp.setCurrentItem(0, true);
                    }
                    for (androidx.fragment.app.Fragment f : getParentFragment().getChildFragmentManager().getFragments()) {
                        if (f instanceof FeedFragment) {
                            ((FeedFragment) f).loadFeed();
                            break;
                        }
                    }
                }
            } else {
                String errMsg = "Error " + response.code();
                try {
                    if (response.errorBody() != null) errMsg = response.errorBody().string();
                } catch (Exception ignored) {}
                Toast.makeText(requireContext(), errMsg, Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
            if (!isAdded()) return;
            resetPublishButton();
            Toast.makeText(requireContext(), "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
        }
    };

    private void resetPublishButton() {
        btnPublish.setEnabled(true);
        btnPublish.setText(R.string.publish);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private File uriToFile(Uri uri, String prefix) throws Exception {
        android.content.Context ctx = getContext();
        if (ctx == null) throw new Exception("Context is null");
        android.content.ContentResolver resolver = ctx.getContentResolver();
        
        String displayName = null;
        try (android.database.Cursor cursor = resolver.query(
                uri, new String[]{android.provider.OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                displayName = cursor.getString(0);
            }
        }
        if (displayName == null || displayName.isEmpty()) {
            displayName = "attachment_" + System.currentTimeMillis();
        }
        
        // Clean display name of invalid filesystem characters
        displayName = displayName.replaceAll("[\\\\/:*?\"<>|]", "_");

        InputStream inputStream = resolver.openInputStream(uri);
        if (inputStream == null) throw new Exception("Cannot open URI");
        
        File cacheDir = ctx.getCacheDir();
        File tempFile = new File(cacheDir, prefix + System.currentTimeMillis() + "_" + displayName);
        tempFile.deleteOnExit();
        
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = inputStream.read(buf)) > 0) out.write(buf, 0, len);
        }
        inputStream.close();
        return tempFile;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopRecordingTimer();
        cleanupRecorder();
    }
}

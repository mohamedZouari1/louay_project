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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonObject;
import com.smartcampus.manouba.R;
import com.smartcampus.manouba.adapters.MessageAdapter;
import com.smartcampus.manouba.model.ChatMessage;
import com.smartcampus.manouba.network.RetrofitClient;
import com.smartcampus.manouba.utils.Constants;
import com.smartcampus.manouba.utils.SharedPrefManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatMessagesFragment extends Fragment {

    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageButton btnSend, btnAttachImage, btnAttachFile, btnRecord, btnCancelReply;
    private View replyPreviewContainer;
    private TextView tvReplyingTo, tvReplyPreview;

    private MessageAdapter adapter;
    private final List<ChatMessage> messages = new ArrayList<>();

    private int otherUserId = -1;
    private String chatName = "";
    private int myId = -1;
    private ChatMessage replyTarget = null;

    // Real-time polling
    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private Runnable pollRunnable;
    private int lastKnownMessageId = -1;

    // Vocal recording
    private MediaRecorder mediaRecorder;
    private File audioFile;
    private boolean isRecording = false;

    // Image picker
    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) sendImageMessage(uri);
                }
            });

    // File picker
    private final ActivityResultLauncher<String[]> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) sendFileMessage(uri);
            });

    // Audio permission
    private final ActivityResultLauncher<String> audioPerm =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) toggleRecording();
                else Toast.makeText(requireContext(), "Microphone permission required", Toast.LENGTH_SHORT).show();
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat_messages, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            String chatIdStr = getArguments().getString("chatId", "-1");
            try { otherUserId = Integer.parseInt(chatIdStr); } catch (Exception e) { otherUserId = -1; }
            chatName = getArguments().getString("chatName", "");
        }

        myId = SharedPrefManager.getInstance(requireContext()).getUserId();

        rvMessages      = view.findViewById(R.id.rv_messages);
        etMessage       = view.findViewById(R.id.et_message);
        btnSend         = view.findViewById(R.id.btn_send_message);
        btnAttachImage  = null;
        btnAttachFile   = null;
        btnRecord       = null;
        replyPreviewContainer = view.findViewById(R.id.reply_preview_container);
        tvReplyingTo = view.findViewById(R.id.tv_replying_to);
        tvReplyPreview = view.findViewById(R.id.tv_reply_preview);
        btnCancelReply = view.findViewById(R.id.btn_cancel_reply);

        View btnBack = view.findViewById(R.id.btn_chat_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> androidx.navigation.Navigation.findNavController(v).navigateUp());
        }

        ImageButton btnAdd = view.findViewById(R.id.btn_chat_add);
        if (btnAdd != null) {
            btnAdd.setOnClickListener(this::showAttachmentOptions);
        }

        android.widget.TextView tvName = view.findViewById(R.id.tv_chat_detail_name);
        android.widget.TextView tvInitials = view.findViewById(R.id.tv_chat_detail_initials);
        if (tvName != null) {
            tvName.setText(chatName);
        }
        if (tvInitials != null && !TextUtils.isEmpty(chatName)) {
            tvInitials.setText(getInitials(chatName));
        }

        adapter = new MessageAdapter(messages);
        adapter.setReplyClickListener(this::setReplyTarget);
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(adapter);

        btnSend.setOnClickListener(v -> sendTextMessage());
        if (btnCancelReply != null) {
            btnCancelReply.setOnClickListener(v -> clearReplyTarget());
        }

        loadMessages();
    }

    @Override
    public void onResume() {
        super.onResume();
        startPolling();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopPolling();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopPolling();
        cleanupRecorder();
    }

    // ── Polling ─────────────────────────────────────────────────────────────

    private void startPolling() {
        stopPolling();
        pollRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isAdded()) return;
                loadMessagesSilent();
                pollHandler.postDelayed(this, Constants.POLL_CHAT_MS);
            }
        };
        pollHandler.postDelayed(pollRunnable, Constants.POLL_CHAT_MS);
    }

    private void stopPolling() {
        if (pollRunnable != null) {
            pollHandler.removeCallbacks(pollRunnable);
            pollRunnable = null;
        }
    }

    // ── Message loading ──────────────────────────────────────────────────────

    private void loadMessages() {
        if (otherUserId == -1) return;
        fetchMessages(true);
    }

    private void loadMessagesSilent() {
        if (otherUserId == -1) return;
        fetchMessages(false);
    }

    private void fetchMessages(boolean scrollToBottom) {
        String token = SharedPrefManager.getInstance(requireContext()).getToken();
        RetrofitClient.getInstance(token).getApi().getMessages(otherUserId)
                .enqueue(new Callback<List<JsonObject>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<JsonObject>> call,
                                           @NonNull Response<List<JsonObject>> response) {
                        if (!isAdded()) return;
                        if (response.isSuccessful() && response.body() != null) {
                            List<JsonObject> raw = response.body();
                            if (raw.isEmpty()) return;

                            // Check if new messages arrived
                            int newLastId = raw.get(raw.size() - 1).has("id")
                                    ? raw.get(raw.size() - 1).get("id").getAsInt() : -1;

                            if (newLastId != lastKnownMessageId || scrollToBottom) {
                                lastKnownMessageId = newLastId;
                                messages.clear();
                                for (JsonObject msg : raw) {
                                    messages.add(parseMessage(msg));
                                }
                                adapter.notifyDataSetChanged();
                                rvMessages.scrollToPosition(messages.size() - 1);
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<JsonObject>> call, @NonNull Throwable t) {}
                });
    }

    private ChatMessage parseMessage(JsonObject msg) {
        String id = msg.has("id") && !msg.get("id").isJsonNull() ? msg.get("id").getAsString() : null;
        int senderId = msg.has("sender_id") ? msg.get("sender_id").getAsInt()
                : (msg.has("sender") ? msg.get("sender").getAsInt() : -1);
        boolean sentByMe = senderId == myId;

        String content = msg.has("content") && !msg.get("content").isJsonNull()
                ? msg.get("content").getAsString() : "";
        String imageUrl = msg.has("image_url") && !msg.get("image_url").isJsonNull()
                ? msg.get("image_url").getAsString() : null;
        String fileUrl = msg.has("file_url") && !msg.get("file_url").isJsonNull()
                ? msg.get("file_url").getAsString() : null;
        String fileName = msg.has("file_name") && !msg.get("file_name").isJsonNull()
                ? msg.get("file_name").getAsString() : null;
        String fileType = msg.has("file_type") && !msg.get("file_type").isJsonNull()
                ? msg.get("file_type").getAsString() : null;
        String timestamp = msg.has("timestamp") && !msg.get("timestamp").isJsonNull()
                ? msg.get("timestamp").getAsString() : "";
        String replyId = null;
        String replySender = null;
        String replyContent = null;
        if (msg.has("reply_to_detail") && !msg.get("reply_to_detail").isJsonNull()) {
            JsonObject reply = msg.getAsJsonObject("reply_to_detail");
            replyId = reply.has("id") && !reply.get("id").isJsonNull()
                    ? reply.get("id").getAsString() : null;
            replySender = reply.has("sender_name") && !reply.get("sender_name").isJsonNull()
                    ? reply.get("sender_name").getAsString() : null;
            replyContent = reply.has("content") && !reply.get("content").isJsonNull()
                    ? reply.get("content").getAsString() : null;
        }

        return new ChatMessage(id, content, formatTime(timestamp), sentByMe,
                imageUrl, fileUrl, fileName, fileType, replyId, replySender, replyContent);
    }

    // ── Send messages ─────────────────────────────────────────────────────────

    private void sendTextMessage() {
        String text = etMessage.getText() != null ? etMessage.getText().toString().trim() : "";
        if (TextUtils.isEmpty(text)) return;
        if (otherUserId == -1) return;

        // Optimistic UI
        ChatMessage optimistic = new ChatMessage(text, true, null, "Sending...");
        messages.add(optimistic);
        adapter.notifyItemInserted(messages.size() - 1);
        rvMessages.scrollToPosition(messages.size() - 1);
        etMessage.setText("");

        String token = SharedPrefManager.getInstance(requireContext()).getToken();
        JsonObject body = new JsonObject();
        body.addProperty("content", text);
        if (replyTarget != null && replyTarget.getId() != null) {
            body.addProperty("reply_to", replyTarget.getId());
        }
        RetrofitClient.getInstance(token).getApi().sendMessage(otherUserId, body)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                        if (!isAdded()) return;
                        if (response.isSuccessful() && response.body() != null) {
                            // Replace optimistic with real message
                            int lastIdx = messages.size() - 1;
                            messages.set(lastIdx, parseMessage(response.body()));
                            adapter.notifyItemChanged(lastIdx);
                            clearReplyTarget();
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                        if (!isAdded()) return;
                        // Remove optimistic on failure
                        if (!messages.isEmpty()) {
                            messages.remove(messages.size() - 1);
                            adapter.notifyDataSetChanged();
                        }
                        Toast.makeText(requireContext(), "Failed to send message", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showAttachmentOptions(View view) {
        androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(requireContext(), view);
        popup.getMenu().add(0, 1, 0, "📷 Send Image");
        popup.getMenu().add(0, 2, 1, "📎 Send File");
        popup.getMenu().add(0, 3, 2, isRecording ? "⏹️ Stop Recording" : "🎤 Record Voice");
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    openImagePicker();
                    return true;
                case 2:
                    openFilePicker();
                    return true;
                case 3:
                    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                            == PackageManager.PERMISSION_GRANTED) {
                        toggleRecording();
                    } else {
                        audioPerm.launch(Manifest.permission.RECORD_AUDIO);
                    }
                    return true;
            }
            return false;
        });
        popup.show();
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void openFilePicker() {
        filePickerLauncher.launch(new String[]{"*/*"});
    }

    private void sendImageMessage(Uri uri) {
        try {
            String mimeType = requireContext().getContentResolver().getType(uri);
            if (mimeType == null) mimeType = "image/jpeg";
            File file = uriToFile(uri);

            String token = SharedPrefManager.getInstance(requireContext()).getToken();
            RequestBody contentBody = RequestBody.create(MediaType.parse("text/plain"), "");
            RequestBody replyToBody = createReplyToBody();
            RequestBody fileBody = RequestBody.create(MediaType.parse(mimeType), file);
            MultipartBody.Part part = MultipartBody.Part.createFormData("image", file.getName(), fileBody);

            RetrofitClient.getInstance(token).getApi()
                    .sendImageMessage(otherUserId, contentBody, replyToBody, part)
                    .enqueue(messageSentCallback);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Could not attach image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void sendFileMessage(Uri uri) {
        try {
            String mimeType = requireContext().getContentResolver().getType(uri);
            if (mimeType == null) mimeType = "application/octet-stream";
            File file = uriToFile(uri);

            String token = SharedPrefManager.getInstance(requireContext()).getToken();
            RequestBody contentBody = RequestBody.create(MediaType.parse("text/plain"), "");
            RequestBody replyToBody = createReplyToBody();
            RequestBody fileBody = RequestBody.create(MediaType.parse(mimeType), file);
            MultipartBody.Part part = MultipartBody.Part.createFormData("file", file.getName(), fileBody);

            RetrofitClient.getInstance(token).getApi()
                    .sendFileMessage(otherUserId, contentBody, replyToBody, part)
                    .enqueue(messageSentCallback);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Could not attach file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void sendAudioMessage(File audioFile) {
        try {
            String token = SharedPrefManager.getInstance(requireContext()).getToken();
            RequestBody contentBody = RequestBody.create(MediaType.parse("text/plain"), "🎤 Voice message");
            RequestBody replyToBody = createReplyToBody();
            RequestBody fileBody = RequestBody.create(MediaType.parse("audio/mp4"), audioFile);
            MultipartBody.Part part = MultipartBody.Part.createFormData("file", audioFile.getName(), fileBody);

            RetrofitClient.getInstance(token).getApi()
                    .sendFileMessage(otherUserId, contentBody, replyToBody, part)
                    .enqueue(messageSentCallback);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Could not send voice message", Toast.LENGTH_SHORT).show();
        }
    }

    private final Callback<JsonObject> messageSentCallback = new Callback<JsonObject>() {
        @Override
        public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
            if (!isAdded()) return;
            if (response.isSuccessful() && response.body() != null) {
                messages.add(parseMessage(response.body()));
                adapter.notifyItemInserted(messages.size() - 1);
                rvMessages.scrollToPosition(messages.size() - 1);
                clearReplyTarget();
            } else {
                Toast.makeText(requireContext(), "Failed to send: " + response.code(), Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
            if (isAdded()) Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show();
        }
    };

    // ── Vocal recording ──────────────────────────────────────────────────────

    private RequestBody createReplyToBody() {
        String value = replyTarget != null && replyTarget.getId() != null ? replyTarget.getId() : "";
        return RequestBody.create(MediaType.parse("text/plain"), value);
    }

    private void setReplyTarget(ChatMessage message) {
        if (message == null) return;
        replyTarget = message;
        if (replyPreviewContainer != null) {
            replyPreviewContainer.setVisibility(View.VISIBLE);
        }
        if (tvReplyingTo != null) {
            tvReplyingTo.setText("Replying to " + (message.isSentByMe() ? "yourself" : chatName));
        }
        if (tvReplyPreview != null) {
            tvReplyPreview.setText(getMessagePreview(message));
        }
        if (etMessage != null) {
            etMessage.requestFocus();
        }
    }

    private void clearReplyTarget() {
        replyTarget = null;
        if (replyPreviewContainer != null) {
            replyPreviewContainer.setVisibility(View.GONE);
        }
    }

    private String getMessagePreview(ChatMessage message) {
        if (!TextUtils.isEmpty(message.getContent())) return message.getContent();
        if (!TextUtils.isEmpty(message.getFileName())) return message.getFileName();
        if (!TextUtils.isEmpty(message.getImageUrl())) return "Photo";
        if (!TextUtils.isEmpty(message.getFileUrl())) return "Attachment";
        return "Message";
    }

    private void toggleRecording() {
        if (!isRecording) {
            startRecording();
        } else {
            stopRecordingAndSend();
        }
    }

    private void startRecording() {
        audioFile = new File(requireContext().getCacheDir(),
                "voice_msg_" + System.currentTimeMillis() + ".m4a");
        try {
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setOutputFile(audioFile.getAbsolutePath());
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            if (btnRecord != null) btnRecord.setImageResource(android.R.drawable.ic_media_pause);
            Toast.makeText(requireContext(), "🎤 Recording... tap again to send", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Could not start recording", Toast.LENGTH_SHORT).show();
            cleanupRecorder();
        }
    }

    private void stopRecordingAndSend() {
        if (mediaRecorder != null) {
            try { mediaRecorder.stop(); } catch (Exception ignored) {}
            cleanupRecorder();
        }
        isRecording = false;
        if (btnRecord != null) btnRecord.setImageResource(android.R.drawable.ic_btn_speak_now);
        if (audioFile != null && audioFile.exists() && audioFile.length() > 0) {
            sendAudioMessage(audioFile);
        }
    }

    private void cleanupRecorder() {
        if (mediaRecorder != null) {
            try { mediaRecorder.release(); } catch (Exception ignored) {}
            mediaRecorder = null;
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private File uriToFile(Uri uri) throws Exception {
        android.content.ContentResolver resolver = requireContext().getContentResolver();
        String displayName = null;
        try (android.database.Cursor cursor = resolver.query(
                uri, new String[]{android.provider.OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                displayName = cursor.getString(0);
            }
        }
        if (displayName == null || displayName.isEmpty()) {
            displayName = "chat_attachment_" + System.currentTimeMillis();
        }
        
        // Clean display name of invalid filesystem characters
        displayName = displayName.replaceAll("[\\\\/:*?\"<>|]", "_");

        InputStream inputStream = resolver.openInputStream(uri);
        if (inputStream == null) throw new Exception("Cannot open URI");
        
        File cacheDir = requireContext().getCacheDir();
        File tempFile = new File(cacheDir, "chat_" + System.currentTimeMillis() + "_" + displayName);
        tempFile.deleteOnExit();
        
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = inputStream.read(buf)) > 0) out.write(buf, 0, len);
        }
        inputStream.close();
        return tempFile;
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "SC";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            String p1 = parts[0].substring(0, 1).toUpperCase();
            String p2 = parts[1].substring(0, 1).toUpperCase();
            return p1 + p2;
        } else if (parts.length == 1 && !parts[0].isEmpty()) {
            String p = parts[0];
            if (p.length() >= 2) {
                return p.substring(0, 2).toUpperCase();
            } else {
                return p.substring(0, 1).toUpperCase();
            }
        }
        return "SC";
    }

    private String formatTime(String isoTime) {
        if (isoTime == null || isoTime.isEmpty()) return "";
        String[] formats = {
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
        };
        for (String fmt : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(fmt, Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date date = sdf.parse(isoTime);
                if (date == null) continue;
                SimpleDateFormat out = new SimpleDateFormat("HH'h'mm d MMM", Locale.getDefault());
                return out.format(date);
            } catch (ParseException ignored) {}
        }
        return "";
    }
}

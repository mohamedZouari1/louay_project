package com.smartcampus.manouba.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.smartcampus.manouba.R;
import com.smartcampus.manouba.adapters.MessageAdapter;
import com.smartcampus.manouba.model.ChatMessage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.content.Intent;
import android.net.Uri;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class ChatMessagesFragment extends Fragment {

    private RecyclerView rvMessages;
    private MessageAdapter adapter;
    private List<ChatMessage> messageList;
    private EditText etMessage;
    private ImageButton btnSend;
    private ImageButton btnAdd;
    private SwipeRefreshLayout swipeRefresh;
    private ActivityResultLauncher<String> pickMediaLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat_messages, container, false);
    }

    private int otherUserId = -1;
    private String token;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String chatName = getArguments() != null ? getArguments().getString("chatName", "Discussion") : "Discussion";
        String chatIdStr = getArguments() != null ? getArguments().getString("chatId", "-1") : "-1";
        otherUserId = Integer.parseInt(chatIdStr);
        token = com.smartcampus.manouba.utils.SharedPrefManager.getInstance(requireContext()).getToken();

        ((TextView) view.findViewById(R.id.tv_chat_detail_name)).setText(chatName);
        
        // Set initials
        TextView tvInitials = view.findViewById(R.id.tv_chat_detail_initials);
        String initials = "";
        String[] parts = chatName.split(" ");
        if (parts.length > 0 && !parts[0].isEmpty()) initials += parts[0].charAt(0);
        if (parts.length > 1 && !parts[1].isEmpty()) initials += parts[1].charAt(0);
        tvInitials.setText(initials.toUpperCase());

        view.findViewById(R.id.btn_chat_back).setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        rvMessages = view.findViewById(R.id.rv_messages);
        etMessage = view.findViewById(R.id.et_message);
        btnSend = view.findViewById(R.id.btn_send_message);
        btnAdd = view.findViewById(R.id.btn_chat_add);
        swipeRefresh = view.findViewById(R.id.swipe_refresh_messages);

        messageList = new ArrayList<>();
        adapter = new MessageAdapter(messageList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::loadMessages);

        pickMediaLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), this::onMediaPicked);

        loadMessages();
        
        btnSend.setOnClickListener(v -> sendMessage());
        
        btnAdd.setOnClickListener(v -> {
            String text = etMessage.getText().toString().trim();
            if (!TextUtils.isEmpty(text)) {
                sendMessage();
            } else {
                pickMediaLauncher.launch("*/*");
            }
        });

        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });
    }

    private void loadMessages() {
        if (otherUserId == -1) return;
        com.smartcampus.manouba.network.RetrofitClient.getInstance(token).getApi().getMessages(otherUserId)
                .enqueue(new retrofit2.Callback<List<com.google.gson.JsonObject>>() {
            @Override
            public void onResponse(retrofit2.Call<List<com.google.gson.JsonObject>> call, 
                                   retrofit2.Response<List<com.google.gson.JsonObject>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    messageList.clear();
                    for (com.google.gson.JsonObject json : response.body()) {
                        String id = json.get("id").getAsString();
                        String content = json.has("content") && !json.get("content").isJsonNull() ? json.get("content").getAsString() : "";
                        String time = formatTime(json.get("timestamp").getAsString());
                        boolean isMe = json.has("is_me") && json.get("is_me").getAsBoolean();
                        String imageUrl = json.has("image_url") && !json.get("image_url").isJsonNull() ? json.get("image_url").getAsString() : null;
                        String fileUrl = json.has("file_url") && !json.get("file_url").isJsonNull() ? json.get("file_url").getAsString() : null;
                        messageList.add(new ChatMessage(id, content, time, isMe, imageUrl, fileUrl));
                    }
                    adapter.notifyDataSetChanged();
                    rvMessages.scrollToPosition(messageList.size() - 1);
                }
                swipeRefresh.setRefreshing(false);
            }

            @Override
            public void onFailure(retrofit2.Call<List<com.google.gson.JsonObject>> call, Throwable t) {
                if (isAdded()) swipeRefresh.setRefreshing(false);
            }
        });
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text) || otherUserId == -1) return;

        com.google.gson.JsonObject body = new com.google.gson.JsonObject();
        body.addProperty("content", text);

        btnSend.setEnabled(false);
        btnAdd.setEnabled(false);

        com.smartcampus.manouba.network.RetrofitClient.getInstance(token).getApi().sendMessage(otherUserId, body)
                .enqueue(new retrofit2.Callback<com.google.gson.JsonObject>() {
            @Override
            public void onResponse(retrofit2.Call<com.google.gson.JsonObject> call, 
                                   retrofit2.Response<com.google.gson.JsonObject> response) {
                if (!isAdded()) return;
                btnSend.setEnabled(true);
                btnAdd.setEnabled(true);
                
                if (response.isSuccessful()) {
                    etMessage.setText("");
                    loadMessages(); // Refresh to show new message
                } else {
                    String err = "Failed to send";
                    try { if (response.errorBody() != null) err += ": " + response.errorBody().string(); } catch (Exception e) {}
                    Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<com.google.gson.JsonObject> call, Throwable t) {
                if (!isAdded()) return;
                btnSend.setEnabled(true);
                btnAdd.setEnabled(true);
                Toast.makeText(requireContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onMediaPicked(Uri uri) {
        if (uri == null) return;
        sendMedia(uri);
    }

    private void sendMedia(Uri uri) {
        if (otherUserId == -1) return;

        try {
            File file = getFileFromUri(uri);
            if (file == null) return;

            String mimeType = requireContext().getContentResolver().getType(uri);
            RequestBody requestFile = RequestBody.create(MediaType.parse(mimeType != null ? mimeType : "application/octet-stream"), file);
            MultipartBody.Part body;
            
            if (mimeType != null && mimeType.startsWith("image/")) {
                body = MultipartBody.Part.createFormData("image", file.getName(), requestFile);
                sendMultipart(null, body, null);
            } else {
                body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);
                sendMultipart(null, null, body);
            }

        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error processing file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void sendMultipart(MultipartBody.Part content, MultipartBody.Part image, MultipartBody.Part filePart) {
        btnSend.setEnabled(false);
        btnAdd.setEnabled(false);

        RequestBody contentBody = RequestBody.create(MediaType.parse("text/plain"), "");

        com.smartcampus.manouba.network.RetrofitClient.getInstance(token).getApi().sendMediaMessage(otherUserId, contentBody, image, filePart)
                .enqueue(new retrofit2.Callback<com.google.gson.JsonObject>() {
            @Override
            public void onResponse(retrofit2.Call<com.google.gson.JsonObject> call, retrofit2.Response<com.google.gson.JsonObject> response) {
                if (!isAdded()) return;
                btnSend.setEnabled(true);
                btnAdd.setEnabled(true);
                if (response.isSuccessful()) {
                    loadMessages();
                } else {
                    Toast.makeText(requireContext(), "Failed to upload", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<com.google.gson.JsonObject> call, Throwable t) {
                if (!isAdded()) return;
                btnSend.setEnabled(true);
                btnAdd.setEnabled(true);
                Toast.makeText(requireContext(), "Upload error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private File getFileFromUri(Uri uri) throws Exception {
        InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
        if (inputStream == null) return null;
        File file = new File(requireContext().getCacheDir(), "upload_" + System.currentTimeMillis());
        FileOutputStream outputStream = new FileOutputStream(file);
        byte[] buffer = new byte[1024];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, read);
        }
        outputStream.flush();
        outputStream.close();
        inputStream.close();
        return file;
    }

    private String formatTime(String isoTime) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", java.util.Locale.US);
            java.util.Date date = sdf.parse(isoTime);
            return new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(date);
        } catch (Exception e) { return ""; }
    }
}

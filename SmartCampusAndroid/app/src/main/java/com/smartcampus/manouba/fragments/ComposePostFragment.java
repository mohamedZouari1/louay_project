package com.smartcampus.manouba.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
    private MaterialButton btnPublish, btnAttach;
    private ImageView ivPreview;
    private ImageButton btnRemoveImage;
    private View imagePreviewContainer;

    private Uri selectedImageUri = null;

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    imagePreviewContainer.setVisibility(View.VISIBLE);
                    Glide.with(this).load(selectedImageUri).centerCrop().into(ivPreview);
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

        etContent            = view.findViewById(R.id.et_content);
        btnPublish           = view.findViewById(R.id.btn_publish);
        btnAttach            = view.findViewById(R.id.btn_attach);
        ivPreview            = view.findViewById(R.id.iv_preview);
        btnRemoveImage       = view.findViewById(R.id.btn_remove_image);
        imagePreviewContainer = view.findViewById(R.id.image_preview_container);

        btnAttach.setOnClickListener(v -> openImagePicker());
        btnRemoveImage.setOnClickListener(v -> {
            selectedImageUri = null;
            imagePreviewContainer.setVisibility(View.GONE);
            Toast.makeText(requireContext(), getString(R.string.photo_removed), Toast.LENGTH_SHORT).show();
        });

        btnPublish.setOnClickListener(v -> publishPost());
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void publishPost() {
        String content = etContent.getText() != null ? etContent.getText().toString().trim() : "";
        if (TextUtils.isEmpty(content)) {
            etContent.setError("Please write something first.");
            return;
        }

        btnPublish.setEnabled(false);
        btnPublish.setText(R.string.publishing);

        String token = SharedPrefManager.getInstance(requireContext()).getToken();
        RequestBody contentBody = RequestBody.create(MediaType.parse("text/plain"), content);

        MultipartBody.Part imagePart = null;
        if (selectedImageUri != null) {
            try {
                File imageFile = uriToFile(selectedImageUri);
                RequestBody reqFile = RequestBody.create(MediaType.parse("image/*"), imageFile);
                imagePart = MultipartBody.Part.createFormData("image", imageFile.getName(), reqFile);
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Could not attach image, posting without it.", Toast.LENGTH_SHORT).show();
            }
        }

        RetrofitClient.getInstance(token).getApi()
                .createPost(contentBody, imagePart)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(@NonNull Call<JsonObject> call,
                                           @NonNull Response<JsonObject> response) {
                        if (!isAdded()) return;
                        btnPublish.setEnabled(true);
                        btnPublish.setText(R.string.publish);

                        if (response.isSuccessful()) {
                            Toast.makeText(requireContext(), getString(R.string.post_published), Toast.LENGTH_SHORT).show();
                            // Clear form
                            etContent.setText("");
                            selectedImageUri = null;
                            imagePreviewContainer.setVisibility(View.GONE);

                            // Switch back to Feed tab and refresh
                            if (getParentFragment() instanceof SocialHubFragment) {
                                androidx.viewpager2.widget.ViewPager2 vp =
                                        getParentFragment().getView().findViewById(R.id.view_pager);
                                if (vp != null) vp.setCurrentItem(0, true);
                            }
                        } else {
                            Toast.makeText(requireContext(), getString(R.string.post_error), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                        if (!isAdded()) return;
                        btnPublish.setEnabled(true);
                        btnPublish.setText(R.string.publish);
                        Toast.makeText(requireContext(), getString(R.string.error_network), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /** Copy content URI into a temp File so OkHttp can read it. */
    private File uriToFile(Uri uri) throws Exception {
        InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
        File tempFile = File.createTempFile("post_image_", ".jpg", requireContext().getCacheDir());
        tempFile.deleteOnExit();
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            byte[] buf = new byte[4096];
            int len;
            assert inputStream != null;
            while ((len = inputStream.read(buf)) > 0) out.write(buf, 0, len);
        }
        inputStream.close();
        return tempFile;
    }
}

package com.smartcampus.manouba.fragments;

import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.google.android.material.button.MaterialButton;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.smartcampus.manouba.R;
import com.smartcampus.manouba.activities.AuthActivity;
import com.smartcampus.manouba.network.RetrofitClient;
import com.smartcampus.manouba.utils.SharedPrefManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterFragment extends Fragment {

    private EditText etName, etEmail, etPassword, etConfirmPassword;
    private Spinner spinnerUniversity;
    private MaterialButton btnRegister;
    private ProgressBar progressRegister;
    private boolean passwordVisible = false, confirmVisible = false;

    private final String[] universities = {
            "Select your university...", "ENSI", "ISCAE", "ISAMM",
            "IPSI", "ISD", "ESCT", "FLAH", "ESEN", "OTHER"
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etName = view.findViewById(R.id.et_name);
        etEmail = view.findViewById(R.id.et_email);
        etPassword = view.findViewById(R.id.et_password);
        etConfirmPassword = view.findViewById(R.id.et_confirm_password);
        spinnerUniversity = view.findViewById(R.id.spinner_university);
        btnRegister = view.findViewById(R.id.btn_register);
        progressRegister = view.findViewById(R.id.progress_register);

        // Setup spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, universities);
        spinnerUniversity.setAdapter(adapter);

        // Back button
        view.findViewById(R.id.btn_back).setOnClickListener(v -> {
            try {
                Navigation.findNavController(view).popBackStack();
            } catch (Exception e) {
                if (getActivity() != null) getActivity().onBackPressed();
            }
        });

        // Toggle password visibility
        view.findViewById(R.id.btn_toggle_password).setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            etPassword.setTransformationMethod(passwordVisible ? null :
                    PasswordTransformationMethod.getInstance());
            etPassword.setSelection(etPassword.length());
        });

        view.findViewById(R.id.btn_toggle_confirm).setOnClickListener(v -> {
            confirmVisible = !confirmVisible;
            etConfirmPassword.setTransformationMethod(confirmVisible ? null :
                    PasswordTransformationMethod.getInstance());
            etConfirmPassword.setSelection(etConfirmPassword.length());
        });

        btnRegister.setOnClickListener(v -> handleRegister());

        view.findViewById(R.id.btn_login).setOnClickListener(v -> {
            try {
                Navigation.findNavController(view).popBackStack();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void handleRegister() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();
        int uniPos = spinnerUniversity.getSelectedItemPosition();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(requireContext(), R.string.error_empty_fields, Toast.LENGTH_SHORT).show();
            return;
        }
        if (uniPos == 0) {
            Toast.makeText(requireContext(), R.string.select_university, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!password.equals(confirmPassword)) {
            Toast.makeText(requireContext(), R.string.error_passwords_mismatch, Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(requireContext(), R.string.error_password_length, Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        JsonObject body = new JsonObject();
        body.addProperty("name", name);
        body.addProperty("email", email);
        body.addProperty("password", password);
        body.addProperty("university", universities[uniPos]);

        RetrofitClient.getInstance().getApi().register(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!isAdded()) return;
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject data = response.body();
                        if (!data.has("token") || !data.has("user")) {
                            Log.e("RegisterFragment", "Invalid response: missing token or user");
                            Toast.makeText(requireContext(), "Invalid server response", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String token = data.get("token").getAsString();
                        JsonObject user = data.getAsJsonObject("user");

                        if (user == null || user.isJsonNull()) {
                            Log.e("RegisterFragment", "Invalid response: user object is null");
                            Toast.makeText(requireContext(), "Registration failed: no user data", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        SharedPrefManager pref = SharedPrefManager.getInstance(requireContext());
                        pref.saveToken(token);

                        String fn = user.has("first_name") && !user.get("first_name").isJsonNull()
                                ? user.get("first_name").getAsString() : "";
                        String ln = user.has("last_name") && !user.get("last_name").isJsonNull()
                                ? user.get("last_name").getAsString() : "";
                        String fullName = (fn + " " + ln).trim();
                        if (fullName.isEmpty()) fullName = name;

                        int userId = user.has("id") && !user.get("id").isJsonNull()
                                ? user.get("id").getAsInt() : -1;

                        pref.saveUser(fullName, email, universities[uniPos], userId);

                        if (getActivity() instanceof AuthActivity) {
                            ((AuthActivity) getActivity()).navigateToMain();
                        }
                    } catch (Exception e) {
                        Log.e("RegisterFragment", "Error parsing register response", e);
                        Toast.makeText(requireContext(), "Error processing registration", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String serverMessage = getServerErrorMessage(response);
                    if (serverMessage != null && !serverMessage.isEmpty()) {
                        Toast.makeText(requireContext(), serverMessage, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(requireContext(), R.string.error_register, Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                if (!isAdded()) return;
                setLoading(false);
                Toast.makeText(requireContext(), R.string.error_network, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        btnRegister.setEnabled(!loading);
        btnRegister.setText(loading ? R.string.creating_account : R.string.create_account);
        progressRegister.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private String getServerErrorMessage(Response<JsonObject> response) {
        try {
            if (response.errorBody() == null) return null;
            String raw = response.errorBody().string();
            if (raw == null || raw.trim().isEmpty()) return null;

            JsonElement parsed = JsonParser.parseString(raw);
            if (!parsed.isJsonObject()) return raw.trim();

            JsonObject obj = parsed.getAsJsonObject();
            if (obj.has("error") && !obj.get("error").isJsonNull()) {
                return obj.get("error").getAsString();
            }
            for (String key : obj.keySet()) {
                JsonElement value = obj.get(key);
                if (value == null || value.isJsonNull()) continue;
                if (value.isJsonArray() && value.getAsJsonArray().size() > 0) {
                    return value.getAsJsonArray().get(0).getAsString();
                }
                return value.getAsString();
            }
        } catch (Exception e) {
            Log.e("RegisterFragment", "Failed to parse error response", e);
        }
        return null;
    }
}

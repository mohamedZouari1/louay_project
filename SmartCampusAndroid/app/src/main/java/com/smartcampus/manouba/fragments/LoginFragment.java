package com.smartcampus.manouba.fragments;

import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
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

public class LoginFragment extends Fragment {

    private EditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private ProgressBar progressLogin;
    private ImageView btnTogglePassword;
    private boolean passwordVisible = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etEmail = view.findViewById(R.id.et_email);
        etPassword = view.findViewById(R.id.et_password);
        btnLogin = view.findViewById(R.id.btn_login);
        progressLogin = view.findViewById(R.id.progress_login);
        btnTogglePassword = view.findViewById(R.id.btn_toggle_password);

        TextView btnSignUp = view.findViewById(R.id.btn_sign_up);

        btnTogglePassword.setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            if (passwordVisible) {
                etPassword.setTransformationMethod(null);
            } else {
                etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
            etPassword.setSelection(etPassword.length());
        });

        btnLogin.setOnClickListener(v -> handleLogin());

        btnSignUp.setOnClickListener(v -> {
            try {
                Navigation.findNavController(view).navigate(R.id.action_login_to_register);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void handleLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), R.string.error_empty_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        JsonObject body = new JsonObject();
        body.addProperty("email", email);
        body.addProperty("password", password);

        RetrofitClient.getInstance().getApi().login(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!isAdded()) return;
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject data = response.body();
                        if (!data.has("token") || !data.has("user")) {
                            Log.e("LoginFragment", "Invalid response: missing token or user");
                            Toast.makeText(requireContext(), "Invalid server response", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String token = data.get("token").getAsString();
                        JsonObject user = data.getAsJsonObject("user");

                        if (user == null || user.isJsonNull()) {
                            Log.e("LoginFragment", "Invalid response: user object is null");
                            Toast.makeText(requireContext(), "Invalid session data", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        SharedPrefManager pref = SharedPrefManager.getInstance(requireContext());
                        pref.saveToken(token);

                        String firstName = user.has("first_name") && !user.get("first_name").isJsonNull() 
                                ? user.get("first_name").getAsString() : "";
                        String lastName = user.has("last_name") && !user.get("last_name").isJsonNull() 
                                ? user.get("last_name").getAsString() : "";
                        String fullName = (firstName + " " + lastName).trim();
                        
                        String emailFromUser = user.has("email") && !user.get("email").isJsonNull()
                                ? user.get("email").getAsString() : email;
                        
                        if (fullName.isEmpty()) fullName = emailFromUser.split("@")[0];

                        String userEmail = emailFromUser;
                        String university = "";
                        if (user.has("profile") && !user.get("profile").isJsonNull()) {
                            JsonObject profile = user.getAsJsonObject("profile");
                            university = profile.has("university") && !profile.get("university").isJsonNull()
                                    ? profile.get("university").getAsString() : "";
                        }
                        
                        int userId = user.has("id") && !user.get("id").isJsonNull() 
                                ? user.get("id").getAsInt() : -1;

                        pref.saveUser(fullName, userEmail, university, userId);

                        if (getActivity() instanceof AuthActivity) {
                            ((AuthActivity) getActivity()).navigateToMain();
                        }
                    } catch (Exception e) {
                        Log.e("LoginFragment", "Error parsing login response", e);
                        Toast.makeText(requireContext(), "Error processing login", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String serverMessage = getServerErrorMessage(response);
                    if (serverMessage != null && !serverMessage.isEmpty()) {
                        Toast.makeText(requireContext(), serverMessage, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(requireContext(), R.string.error_login, Toast.LENGTH_SHORT).show();
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
        btnLogin.setEnabled(!loading);
        btnLogin.setText(loading ? R.string.logging_in : R.string.login);
        progressLogin.setVisibility(loading ? View.VISIBLE : View.GONE);
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
            Log.e("LoginFragment", "Failed to parse error response", e);
        }
        return null;
    }
}

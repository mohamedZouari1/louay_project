package com.smartcampus.manouba.fragments;

import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
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
import com.google.gson.JsonObject;
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
                    JsonObject data = response.body();
                    String token = data.get("token").getAsString();
                    JsonObject user = data.getAsJsonObject("user");

                    SharedPrefManager pref = SharedPrefManager.getInstance(requireContext());
                    pref.saveToken(token);

                    String firstName = user.has("first_name") ? user.get("first_name").getAsString() : "";
                    String lastName = user.has("last_name") ? user.get("last_name").getAsString() : "";
                    String fullName = (firstName + " " + lastName).trim();
                    if (fullName.isEmpty()) fullName = email.split("@")[0];

                    String userEmail = user.get("email").getAsString();
                    String university = "";
                    if (user.has("profile") && !user.get("profile").isJsonNull()) {
                        JsonObject profile = user.getAsJsonObject("profile");
                        university = profile.has("university") ? profile.get("university").getAsString() : "";
                    }
                    int userId = user.get("id").getAsInt();

                    pref.saveUser(fullName, userEmail, university, userId);

                    if (getActivity() instanceof AuthActivity) {
                        ((AuthActivity) getActivity()).navigateToMain();
                    }
                } else {
                    Toast.makeText(requireContext(), R.string.error_login, Toast.LENGTH_SHORT).show();
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
}

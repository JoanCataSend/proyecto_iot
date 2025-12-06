package com.example.proyecto_iot;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyecto_iot.utils.CustomToast;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private ImageView togglePasswordImage;
    private boolean passwordVisible = false;

    private Button loginButton, registerButton, forgotButton, googleButton, facebookButton, xButton;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;
    private CallbackManager mCallbackManager;
    private static final int RC_SIGN_IN = 9001;

    private FrameLayout loadingOverlay;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        emailEditText = findViewById(R.id.editTextText);
        passwordEditText = findViewById(R.id.editTextTextPassword);

        togglePasswordImage = findViewById(R.id.imageViewTogglePassword);

        loginButton = findViewById(R.id.button);
        registerButton = findViewById(R.id.button2);
        forgotButton = findViewById(R.id.button3);
        googleButton = findViewById(R.id.button4);
        facebookButton = findViewById(R.id.button6);
        xButton = findViewById(R.id.button5);

        // Overlay + loader
        loadingOverlay = findViewById(R.id.loadingOverlay);
        progressBar = findViewById(R.id.progressBar);

        togglePasswordImage.setOnClickListener(v -> togglePasswordVisibility());
        loginButton.setOnClickListener(v -> loginWithEmail());

        registerButton.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));

        forgotButton.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class)));

        // GOOGLE LOGIN
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        googleButton.setOnClickListener(v -> {
            mostrarLoading(true);
            signInWithGoogle();
        });

        // FACEBOOK LOGIN
        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(getApplication());
        mCallbackManager = CallbackManager.Factory.create();

        facebookButton.setOnClickListener(v -> {
            mostrarLoading(true);

            LoginManager.getInstance().logInWithReadPermissions(
                    LoginActivity.this, Arrays.asList("email", "public_profile"));

            LoginManager.getInstance().registerCallback(mCallbackManager,
                    new FacebookCallback<LoginResult>() {
                        @Override
                        public void onSuccess(LoginResult loginResult) {
                            handleFacebookAccessToken(loginResult.getAccessToken());
                        }

                        @Override
                        public void onCancel() {
                            mostrarLoading(false);
                            CustomToast.warning(LoginActivity.this,
                                    "Inicio con Facebook cancelado");
                        }

                        @Override
                        public void onError(FacebookException error) {
                            mostrarLoading(false);
                            CustomToast.error(LoginActivity.this,
                                    "Error: " + error.getMessage());
                        }
                    });
        });

        xButton.setOnClickListener(v ->
                CustomToast.warning(this, "Inicio con X pendiente de implementación"));
    }

    // ======================================================
    private void togglePasswordVisibility() {

        if (!passwordVisible) {
            passwordEditText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            togglePasswordImage.setImageResource(R.drawable.ic_eye_open);
        } else {
            passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            togglePasswordImage.setImageResource(R.drawable.ic_eye_closed);
        }

        passwordVisible = !passwordVisible;
        passwordEditText.setSelection(passwordEditText.getText().length());
    }

    // ======================================================
    private void loginWithEmail() {

        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            CustomToast.warning(this, "Completa todos los campos");
            return;
        }

        mostrarLoading(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {

                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user != null && user.isEmailVerified()) {

                            // IMPORTANTE: NO ocultamos overlay
                            navigateAfterLogin(user);

                        } else {
                            mostrarLoading(false);
                            CustomToast.warning(this,
                                    "Debes verificar tu email antes de continuar.");
                            mAuth.signOut();
                        }

                    } else {
                        mostrarLoading(false);
                        CustomToast.error(this,
                                "Error: " + task.getException().getLocalizedMessage());
                    }
                });
    }

    // ======================================================
    private void signInWithGoogle() {
        startActivityForResult(mGoogleSignInClient.getSignInIntent(), RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        mCallbackManager.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {

            Task<GoogleSignInAccount> task =
                    GoogleSignIn.getSignedInAccountFromIntent(data);

            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());

            } catch (ApiException e) {
                mostrarLoading(false);
                CustomToast.error(this,
                        "Error en inicio con Google: " + e.getMessage());
            }
        }
    }

    // ======================================================
    private void firebaseAuthWithGoogle(String idToken) {

        AuthCredential credential =
                GoogleAuthProvider.getCredential(idToken, null);

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {

                        // No ocultamos overlay aquí
                        navigateAfterLogin(mAuth.getCurrentUser());

                    } else {
                        mostrarLoading(false);
                        CustomToast.error(this,
                                "Error al autenticar con Google");
                    }
                });
    }

    // ======================================================
    private void handleFacebookAccessToken(AccessToken token) {

        AuthCredential credential =
                FacebookAuthProvider.getCredential(token.getToken());

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {

                        // No quitamos overlay
                        navigateAfterLogin(mAuth.getCurrentUser());

                    } else {
                        mostrarLoading(false);
                        CustomToast.error(this,
                                "Error en inicio con Facebook");
                    }
                });
    }

    // ======================================================
    private void navigateAfterLogin(FirebaseUser user) {

        getSharedPreferences("kova_prefs", MODE_PRIVATE)
                .edit()
                .putBoolean("wizard_completed", true)
                .apply();

        db.collection("Coches")
                .whereArrayContains("Propietario", user.getUid())
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {

                    Intent next;

                    if (query.isEmpty()) {
                        next = new Intent(this, PrimerCocheActivity.class);
                    } else {
                        next = new Intent(this, MainActivity.class);
                    }

                    startActivity(next);
                    finish(); // overlay desaparece aquí automáticamente

                })
                .addOnFailureListener(e -> {

                    mostrarLoading(false);
                    CustomToast.error(this,
                            "Error comprobando los coches.");

                    startActivity(new Intent(this, PrimerCocheActivity.class));
                    finish();
                });
    }

    // ======================================================
    private void mostrarLoading(boolean mostrar) {

        if (mostrar) {
            loadingOverlay.setAlpha(0f);
            loadingOverlay.setVisibility(View.VISIBLE);
            loadingOverlay.animate().alpha(1f).setDuration(200).start();

            loginButton.setEnabled(false);
            googleButton.setEnabled(false);
            facebookButton.setEnabled(false);
            registerButton.setEnabled(false);
            forgotButton.setEnabled(false);
            xButton.setEnabled(false);

        } else {
            loadingOverlay.animate().alpha(0f).setDuration(200)
                    .withEndAction(() -> loadingOverlay.setVisibility(View.GONE))
                    .start();

            loginButton.setEnabled(true);
            googleButton.setEnabled(true);
            facebookButton.setEnabled(true);
            registerButton.setEnabled(true);
            forgotButton.setEnabled(true);
            xButton.setEnabled(true);
        }
    }
}

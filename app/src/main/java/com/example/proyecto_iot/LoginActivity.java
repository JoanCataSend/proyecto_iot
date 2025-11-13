package com.example.proyecto_iot;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

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
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Arrays;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private Button loginButton, registerButton, forgotButton, googleButton, facebookButton, xButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Google Sign-In
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    // Facebook
    private CallbackManager mCallbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- Inicializar SDK de Facebook ---
        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(getApplication());

        setContentView(R.layout.login);

        // --- Inicializar Firebase ---
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // --- Referencias a vistas ---
        emailEditText = findViewById(R.id.editTextText);
        passwordEditText = findViewById(R.id.editTextTextPassword);
        loginButton = findViewById(R.id.button);
        registerButton = findViewById(R.id.button2);
        forgotButton = findViewById(R.id.button3);
        googleButton = findViewById(R.id.button4);
        xButton = findViewById(R.id.button5);
        facebookButton = findViewById(R.id.button6);

        // --- Mostrar / Ocultar contraseña ---
        ImageView togglePasswordImage = findViewById(R.id.imageViewTogglePassword);
        final boolean[] passwordVisible = {false};

        togglePasswordImage.setOnClickListener(v -> {
            if (passwordVisible[0]) {
                passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                togglePasswordImage.setImageResource(R.drawable.ic_eye_closed);
                passwordVisible[0] = false;
            } else {
                passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                togglePasswordImage.setImageResource(R.drawable.ic_eye_open);
                passwordVisible[0] = true;
            }
            passwordEditText.setSelection(passwordEditText.getText().length());
        });

        // --- LOGIN CON CORREO Y CONTRASEÑA ---
        loginButton.setOnClickListener(v -> loginWithEmail());

        // --- REGISTRO Y RECUPERACIÓN ---
        registerButton.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
        forgotButton.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class)));

        // --- GOOGLE SIGN IN ---
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        googleButton.setOnClickListener(v -> signInWithGoogle());

        // --- FACEBOOK SIGN IN ---
        mCallbackManager = CallbackManager.Factory.create();
        facebookButton.setOnClickListener(v -> {
            LoginManager.getInstance().logInWithReadPermissions(
                    LoginActivity.this, Arrays.asList("email", "public_profile"));
            LoginManager.getInstance().registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
                @Override
                public void onSuccess(LoginResult loginResult) {
                    handleFacebookAccessToken(loginResult.getAccessToken());
                }

                @Override
                public void onCancel() {
                    Toast.makeText(LoginActivity.this, "Inicio con Facebook cancelado", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(FacebookException error) {
                    Toast.makeText(LoginActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });

        xButton.setOnClickListener(v ->
                Toast.makeText(this, "Inicio con X pendiente de implementación", Toast.LENGTH_SHORT).show());
    }

    // ---------------- CORREO Y CONTRASEÑA ----------------
    private void loginWithEmail() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user != null && user.isEmailVerified()) {
                            comprobarPrimerCoche(user.getUid());
                        } else {
                            Toast.makeText(this,
                                    "Tu cuenta no está verificada. Revisa tu correo electrónico.",
                                    Toast.LENGTH_LONG).show();
                            mAuth.signOut();
                        }
                    } else {
                        Toast.makeText(this,
                                "Error: " + task.getException().getLocalizedMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ---------------- GOOGLE ----------------
    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mCallbackManager.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Toast.makeText(this, "Error en inicio con Google: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) comprobarPrimerCoche(user.getUid());
                    } else {
                        Toast.makeText(this, "Error al autenticar con Google", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ---------------- FACEBOOK ----------------
    private void handleFacebookAccessToken(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) comprobarPrimerCoche(user.getUid());
                    } else {
                        Toast.makeText(this, "Error en inicio con Facebook", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ---------------- VERIFICAR SI TIENE COCHE ----------------
    private void comprobarPrimerCoche(String userId) {
        db.collection("Coches")
                .whereArrayContains("Propietario", userId)
                .get()
                .addOnSuccessListener(query -> {
                    Intent intent;
                    if (query.isEmpty()) {
                        // 🚗 Usuario nuevo sin coches → redirigir a registro de coche
                        intent = new Intent(LoginActivity.this, PrimerCocheActivity.class);
                    } else {
                        // ✅ Usuario ya tiene coche → ir al menú principal
                        intent = new Intent(LoginActivity.this, MainActivity.class);
                    }
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al verificar coches: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}

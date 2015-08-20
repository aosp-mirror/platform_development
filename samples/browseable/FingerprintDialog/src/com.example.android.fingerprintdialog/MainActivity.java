/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.example.android.fingerprintdialog;

import android.Manifest;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.inject.Inject;

/**
 * Main entry point for the sample, showing a backpack and "Purchase" button.
 */
public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String DIALOG_FRAGMENT_TAG = "myFragment";
    private static final String SECRET_MESSAGE = "Very secret message";
    /** Alias for our key in the Android Key Store */
    private static final String KEY_NAME = "my_key";

    private static final int FINGERPRINT_PERMISSION_REQUEST_CODE = 0;

    @Inject KeyguardManager mKeyguardManager;
    @Inject FingerprintManager mFingerprintManager;
    @Inject FingerprintAuthenticationDialogFragment mFragment;
    @Inject KeyStore mKeyStore;
    @Inject KeyGenerator mKeyGenerator;
    @Inject Cipher mCipher;
    @Inject SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((InjectedApplication) getApplication()).inject(this);

        requestPermissions(new String[]{Manifest.permission.USE_FINGERPRINT},
                FINGERPRINT_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] state) {
        if (requestCode == FINGERPRINT_PERMISSION_REQUEST_CODE
                && state[0] == PackageManager.PERMISSION_GRANTED) {
            setContentView(R.layout.activity_main);
            Button purchaseButton = (Button) findViewById(R.id.purchase_button);
            if (!mKeyguardManager.isKeyguardSecure()) {
                // Show a message that the user hasn't set up a fingerprint or lock screen.
                Toast.makeText(this,
                        "Secure lock screen hasn't set up.\n"
                                + "Go to 'Settings -> Security -> Fingerprint' to set up a fingerprint",
                        Toast.LENGTH_LONG).show();
                purchaseButton.setEnabled(false);
                return;
            }
            if (!mFingerprintManager.hasEnrolledFingerprints()) {
                purchaseButton.setEnabled(false);
                // This happens when no fingerprints are registered.
                Toast.makeText(this,
                        "Go to 'Settings -> Security -> Fingerprint' and register at least one fingerprint",
                        Toast.LENGTH_LONG).show();
                return;
            }
            createKey();
            purchaseButton.setEnabled(true);
            purchaseButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    findViewById(R.id.confirmation_message).setVisibility(View.GONE);
                    findViewById(R.id.encrypted_message).setVisibility(View.GONE);

                    // Set up the crypto object for later. The object will be authenticated by use
                    // of the fingerprint.
                    if (initCipher()) {

                        // Show the fingerprint dialog. The user has the option to use the fingerprint with
                        // crypto, or you can fall back to using a server-side verified password.
                        mFragment.setCryptoObject(new FingerprintManager.CryptoObject(mCipher));
                        boolean useFingerprintPreference = mSharedPreferences
                                .getBoolean(getString(R.string.use_fingerprint_to_authenticate_key),
                                        true);
                        if (useFingerprintPreference) {
                            mFragment.setStage(
                                    FingerprintAuthenticationDialogFragment.Stage.FINGERPRINT);
                        } else {
                            mFragment.setStage(
                                    FingerprintAuthenticationDialogFragment.Stage.PASSWORD);
                        }
                        mFragment.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);
                    } else {
                        // This happens if the lock screen has been disabled or or a fingerprint got
                        // enrolled. Thus show the dialog to authenticate with their password first
                        // and ask the user if they want to authenticate with fingerprints in the
                        // future
                        mFragment.setCryptoObject(new FingerprintManager.CryptoObject(mCipher));
                        mFragment.setStage(
                                FingerprintAuthenticationDialogFragment.Stage.NEW_FINGERPRINT_ENROLLED);
                        mFragment.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);
                    }
                }
            });
        }
    }

    /**
     * Initialize the {@link Cipher} instance with the created key in the {@link #createKey()}
     * method.
     *
     * @return {@code true} if initialization is successful, {@code false} if the lock screen has
     * been disabled or reset after the key was generated, or if a fingerprint got enrolled after
     * the key was generated.
     */
    private boolean initCipher() {
        try {
            mKeyStore.load(null);
            SecretKey key = (SecretKey) mKeyStore.getKey(KEY_NAME, null);
            mCipher.init(Cipher.ENCRYPT_MODE, key);
            return true;
        } catch (KeyPermanentlyInvalidatedException e) {
            return false;
        } catch (KeyStoreException | CertificateException | UnrecoverableKeyException | IOException
                | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to init Cipher", e);
        }
    }

    public void onPurchased(boolean withFingerprint) {
        if (withFingerprint) {
            // If the user has authenticated with fingerprint, verify that using cryptography and
            // then show the confirmation message.
            tryEncrypt();
        } else {
            // Authentication happened with backup password. Just show the confirmation message.
            showConfirmation(null);
        }
    }

    // Show confirmation, if fingerprint was used show crypto information.
    private void showConfirmation(byte[] encrypted) {
        findViewById(R.id.confirmation_message).setVisibility(View.VISIBLE);
        if (encrypted != null) {
            TextView v = (TextView) findViewById(R.id.encrypted_message);
            v.setVisibility(View.VISIBLE);
            v.setText(Base64.encodeToString(encrypted, 0 /* flags */));
        }
    }

    /**
     * Tries to encrypt some data with the generated key in {@link #createKey} which is
     * only works if the user has just authenticated via fingerprint.
     */
    private void tryEncrypt() {
        try {
            byte[] encrypted = mCipher.doFinal(SECRET_MESSAGE.getBytes());
            showConfirmation(encrypted);
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            Toast.makeText(this, "Failed to encrypt the data with the generated key. "
                    + "Retry the purchase", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Failed to encrypt the data with the generated key." + e.getMessage());
        }
    }

    /**
     * Creates a symmetric key in the Android Key Store which can only be used after the user has
     * authenticated with fingerprint.
     */
    public void createKey() {
        // The enrolling flow for fingerprint. This is where you ask the user to set up fingerprint
        // for your flow. Use of keys is necessary if you need to know if the set of
        // enrolled fingerprints has changed.
        try {
            mKeyStore.load(null);
            // Set the alias of the entry in Android KeyStore where the key will appear
            // and the constrains (purposes) in the constructor of the Builder
            mKeyGenerator.init(new KeyGenParameterSpec.Builder(KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT |
                            KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                            // Require the user to authenticate with a fingerprint to authorize every use
                            // of the key
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build());
            mKeyGenerator.generateKey();
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException
                | CertificateException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

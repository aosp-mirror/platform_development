/*
 * Copyright 2013 The Android Open Source Project
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

package com.example.android.apis.security;

import com.example.android.apis.R;

import android.app.Activity;
import android.content.Context;
import android.database.DataSetObserver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.security.KeyPairGeneratorSpec;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import javax.security.auth.x500.X500Principal;

public class KeyStoreUsage extends Activity {
    private static final String TAG = "AndroidKeyStoreUsage";

    /**
     * An instance of {@link java.security.KeyStore} through which this app
     * talks to the {@code AndroidKeyStore}.
     */
    KeyStore mKeyStore;

    /**
     * Used by the {@code ListView} in our layout to list the keys available in
     * our {@code KeyStore} by their alias names.
     */
    AliasAdapter mAdapter;

    /**
     * Button in the UI that causes a new keypair to be generated in the
     * {@code KeyStore}.
     */
    Button mGenerateButton;

    /**
     * Button in the UI that causes data to be signed by a key we selected from
     * the list available in the {@code KeyStore}.
     */
    Button mSignButton;

    /**
     * Button in the UI that causes data to be signed by a key we selected from
     * the list available in the {@code KeyStore}.
     */
    Button mVerifyButton;

    /**
     * Button in the UI that causes a key entry to be deleted from the
     * {@code KeyStore}.
     */
    Button mDeleteButton;

    /**
     * Text field in the UI that holds plaintext.
     */
    EditText mPlainText;

    /**
     * Text field in the UI that holds the signature.
     */
    EditText mCipherText;

    /**
     * The alias of the selected entry in the KeyStore.
     */
    private String mSelectedAlias;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.keystore_usage);

        /*
         * Set up our {@code ListView} with an adapter that allows
         * us to choose from the available entry aliases.
         */
        ListView lv = (ListView) findViewById(R.id.entries_list);
        mAdapter = new AliasAdapter(getApplicationContext());
        lv.setAdapter(mAdapter);
        lv.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mSelectedAlias = mAdapter.getItem(position);
                setKeyActionButtonsEnabled(true);
            }
        });

        // This is alias the user wants for a generated key.
        final EditText aliasInput = (EditText) findViewById(R.id.entry_name);
        mGenerateButton = (Button) findViewById(R.id.generate_button);
        mGenerateButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                 * When the user presses the "Generate" button, we'll
                 * check the alias isn't blank here.
                 */
                final String alias = aliasInput.getText().toString();
                if (alias == null || alias.length() == 0) {
                    aliasInput.setError(getResources().getText(R.string.keystore_no_alias_error));
                } else {
                    /*
                     * It's not blank, so disable the generate button while
                     * the generation of the key is happening. It will be
                     * enabled by the {@code AsyncTask} later after its
                     * work is done.
                     */
                    aliasInput.setError(null);
                    mGenerateButton.setEnabled(false);
                    new GenerateTask().execute(alias);
                }
            }
        });

        mSignButton = (Button) findViewById(R.id.sign_button);
        mSignButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final String alias = mSelectedAlias;
                final String data = mPlainText.getText().toString();
                if (alias != null) {
                    setKeyActionButtonsEnabled(false);
                    new SignTask().execute(alias, data);
                }
            }
        });

        mVerifyButton = (Button) findViewById(R.id.verify_button);
        mVerifyButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final String alias = mSelectedAlias;
                final String data = mPlainText.getText().toString();
                final String signature = mCipherText.getText().toString();
                if (alias != null) {
                    setKeyActionButtonsEnabled(false);
                    new VerifyTask().execute(alias, data, signature);
                }
            }
        });

        mDeleteButton = (Button) findViewById(R.id.delete_button);
        mDeleteButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final String alias = mSelectedAlias;
                if (alias != null) {
                    setKeyActionButtonsEnabled(false);
                    new DeleteTask().execute(alias);
                }
            }
        });

        mPlainText = (EditText) findViewById(R.id.plaintext);
        mPlainText.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                mPlainText.setTextColor(getResources().getColor(android.R.color.primary_text_dark));
            }
        });

        mCipherText = (EditText) findViewById(R.id.ciphertext);
        mCipherText.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                mCipherText
                        .setTextColor(getResources().getColor(android.R.color.primary_text_dark));
            }
        });

        updateKeyList();
    }

    private class AliasAdapter extends ArrayAdapter<String> {
        public AliasAdapter(Context context) {
            // We want users to choose a key, so use the appropriate layout.
            super(context, android.R.layout.simple_list_item_single_choice);
        }

        /**
         * This clears out all previous aliases and replaces it with the
         * current entries.
         */
        public void setAliases(List<String> items) {
            clear();
            addAll(items);
            notifyDataSetChanged();
        }
    }

    private void updateKeyList() {
        setKeyActionButtonsEnabled(false);
        new UpdateKeyListTask().execute();
    }

    /**
     * Sets all the buttons related to actions that act on an existing key to
     * enabled or disabled.
     */
    private void setKeyActionButtonsEnabled(boolean enabled) {
        mSignButton.setEnabled(enabled);
        mVerifyButton.setEnabled(enabled);
        mDeleteButton.setEnabled(enabled);
    }

    private class UpdateKeyListTask extends AsyncTask<Void, Void, Enumeration<String>> {
        @Override
        protected Enumeration<String> doInBackground(Void... params) {
            try {
// BEGIN_INCLUDE(list)
                /*
                 * Load the Android KeyStore instance using the the
                 * "AndroidKeyStore" provider to list out what entries are
                 * currently stored.
                 */
                KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
                ks.load(null);
                Enumeration<String> aliases = ks.aliases();
// END_INCLUDE(list)
                return aliases;
            } catch (KeyStoreException e) {
                Log.w(TAG, "Could not list keys", e);
                return null;
            } catch (NoSuchAlgorithmException e) {
                Log.w(TAG, "Could not list keys", e);
                return null;
            } catch (CertificateException e) {
                Log.w(TAG, "Could not list keys", e);
                return null;
            } catch (IOException e) {
                Log.w(TAG, "Could not list keys", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Enumeration<String> result) {
            List<String> aliases = new ArrayList<String>();
            while (result.hasMoreElements()) {
                aliases.add(result.nextElement());
            }
            mAdapter.setAliases(aliases);
        }
    }

    private class GenerateTask extends AsyncTask<String, Void, Boolean> {
        @Override
        protected Boolean doInBackground(String... params) {
            final String alias = params[0];
            try {
// BEGIN_INCLUDE(generate)
                /*
                 * Generate a new entry in the KeyStore by using the
                 * KeyPairGenerator API. We have to specify the attributes for a
                 * self-signed X.509 certificate here so the KeyStore can attach
                 * the public key part to it. It can be replaced later with a
                 * certificate signed by a Certificate Authority (CA) if needed.
                 */
                Calendar cal = Calendar.getInstance();
                Date now = cal.getTime();
                cal.add(Calendar.YEAR, 1);
                Date end = cal.getTime();

                KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore");
                kpg.initialize(new KeyPairGeneratorSpec.Builder(getApplicationContext())
                        .setAlias(alias)
                        .setStartDate(now)
                        .setEndDate(end)
                        .setSerialNumber(BigInteger.valueOf(1))
                        .setSubject(new X500Principal("CN=test1"))
                        .build());

                KeyPair kp = kpg.generateKeyPair();
// END_INCLUDE(generate)
                return true;
            } catch (NoSuchAlgorithmException e) {
                Log.w(TAG, "Could not generate key", e);
                return false;
            } catch (InvalidAlgorithmParameterException e) {
                Log.w(TAG, "Could not generate key", e);
                return false;
            } catch (NoSuchProviderException e) {
                Log.w(TAG, "Could not generate key", e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            updateKeyList();
            mGenerateButton.setEnabled(true);
        }

        @Override
        protected void onCancelled() {
            mGenerateButton.setEnabled(true);
        }
    }

    private class SignTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            final String alias = params[0];
            final String dataString = params[1];
            try {
                byte[] data = dataString.getBytes();
// BEGIN_INCLUDE(sign)
                /*
                 * Use a PrivateKey in the KeyStore to create a signature over
                 * some data.
                 */
                KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
                ks.load(null);
                KeyStore.Entry entry = ks.getEntry(alias, null);
                if (!(entry instanceof PrivateKeyEntry)) {
                    Log.w(TAG, "Not an instance of a PrivateKeyEntry");
                    return null;
                }
                Signature s = Signature.getInstance("SHA256withRSA");
                s.initSign(((PrivateKeyEntry) entry).getPrivateKey());
                s.update(data);
                byte[] signature = s.sign();
// END_INCLUDE(sign)
                return Base64.encodeToString(signature, Base64.DEFAULT);
            } catch (NoSuchAlgorithmException e) {
                Log.w(TAG, "Could not generate key", e);
                return null;
            } catch (KeyStoreException e) {
                Log.w(TAG, "Could not generate key", e);
                return null;
            } catch (CertificateException e) {
                Log.w(TAG, "Could not generate key", e);
                return null;
            } catch (IOException e) {
                Log.w(TAG, "Could not generate key", e);
                return null;
            } catch (UnrecoverableEntryException e) {
                Log.w(TAG, "Could not generate key", e);
                return null;
            } catch (InvalidKeyException e) {
                Log.w(TAG, "Could not generate key", e);
                return null;
            } catch (SignatureException e) {
                Log.w(TAG, "Could not generate key", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            mCipherText.setText(result);
            setKeyActionButtonsEnabled(true);
        }

        @Override
        protected void onCancelled() {
            mCipherText.setText("error!");
            setKeyActionButtonsEnabled(true);
        }
    }

    private class VerifyTask extends AsyncTask<String, Void, Boolean> {
        @Override
        protected Boolean doInBackground(String... params) {
            final String alias = params[0];
            final String dataString = params[1];
            final String signatureString = params[2];
            try {
                byte[] data = dataString.getBytes();
                byte[] signature;
                try {
                    signature = Base64.decode(signatureString, Base64.DEFAULT);
                } catch (IllegalArgumentException e) {
                    signature = new byte[0];
                }
// BEGIN_INCLUDE(verify)
                /*
                 * Verify a signature previously made by a PrivateKey in our
                 * KeyStore. This uses the X.509 certificate attached to our
                 * private key in the KeyStore to validate a previously
                 * generated signature.
                 */
                KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
                ks.load(null);
                KeyStore.Entry entry = ks.getEntry(alias, null);
                if (!(entry instanceof PrivateKeyEntry)) {
                    Log.w(TAG, "Not an instance of a PrivateKeyEntry");
                    return false;
                }
                Signature s = Signature.getInstance("SHA256withRSA");
                s.initVerify(((PrivateKeyEntry) entry).getCertificate());
                s.update(data);
                boolean valid = s.verify(signature);
// END_INCLUDE(verify)
                return valid;
            } catch (NoSuchAlgorithmException e) {
                Log.w(TAG, "Could not generate key", e);
                return false;
            } catch (KeyStoreException e) {
                Log.w(TAG, "Could not generate key", e);
                return false;
            } catch (CertificateException e) {
                Log.w(TAG, "Could not generate key", e);
                return false;
            } catch (IOException e) {
                Log.w(TAG, "Could not generate key", e);
                return false;
            } catch (UnrecoverableEntryException e) {
                Log.w(TAG, "Could not generate key", e);
                return false;
            } catch (InvalidKeyException e) {
                Log.w(TAG, "Could not generate key", e);
                return false;
            } catch (SignatureException e) {
                Log.w(TAG, "Could not generate key", e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                mCipherText.setTextColor(getResources().getColor(R.color.solid_green));
            } else {
                mCipherText.setTextColor(getResources().getColor(R.color.solid_red));
            }
            setKeyActionButtonsEnabled(true);
        }

        @Override
        protected void onCancelled() {
            mCipherText.setText("error!");
            setKeyActionButtonsEnabled(true);
            mCipherText.setTextColor(getResources().getColor(android.R.color.primary_text_dark));
        }
    }

    private class DeleteTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            final String alias = params[0];
            try {
// BEGIN_INCLUDE(delete)
                /*
                 * Deletes a previously generated or stored entry in the
                 * KeyStore.
                 */
                KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
                ks.load(null);
                ks.deleteEntry(alias);
// END_INCLUDE(delete)
            } catch (NoSuchAlgorithmException e) {
                Log.w(TAG, "Could not generate key", e);
            } catch (KeyStoreException e) {
                Log.w(TAG, "Could not generate key", e);
            } catch (CertificateException e) {
                Log.w(TAG, "Could not generate key", e);
            } catch (IOException e) {
                Log.w(TAG, "Could not generate key", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            updateKeyList();
        }

        @Override
        protected void onCancelled() {
            updateKeyList();
        }
    }
}

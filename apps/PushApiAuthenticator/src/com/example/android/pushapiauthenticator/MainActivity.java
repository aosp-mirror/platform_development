/*
 * Copyright (C) 2016 The Android Open Source Project
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
 * limitations under the License.
 */


package com.example.android.pushapiauthenticator;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorDescription;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.List;

public class MainActivity extends Activity {

    public static final String TYPE = "com.example.android.pushapiauthenticator";

    private static AccountManager am;
    private ComponentName authenticatorComponent;

    public boolean isAccountAdded(Account a) {
        Account[] accounts = am.getAccountsByType(TYPE);
        for (Account account : accounts) {
            if (a.equals(account)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        am = AccountManager.get(getApplicationContext());
        final Button getAllRequestingApps = (Button) findViewById(R.id.getallrequestingapps);
        final TextView getAllRequesting3pUids = (TextView) findViewById(R.id.requestingapps);

        final RadioGroup accountChooser = (RadioGroup) findViewById(R.id.accountGroup);
        final RadioGroup optionChooser = (RadioGroup) findViewById(R.id.optionsGroup);
        final RadioGroup packagesChooser = (RadioGroup) findViewById(R.id.packagesChooser);
        final Button selectOption = (Button) findViewById(R.id.selectoptionbutton);
        final TextView authStatus = (TextView) findViewById(R.id.authenticatorstatus);

        final Toast hitGet =
                Toast.makeText(getApplicationContext(), "Hit the GET Button!", Toast.LENGTH_SHORT);
        final Toast enterPackageName = Toast.makeText(getApplicationContext(),
                "Choose a packageName!", Toast.LENGTH_SHORT);
        final Toast chooseAccountWarning =
                Toast.makeText(getApplicationContext(), "Choose an Account!", Toast.LENGTH_SHORT);
        final Toast chooseOptionWarning =
                Toast.makeText(getApplicationContext(), "Choose an Option!", Toast.LENGTH_SHORT);

        final String ACCOUNT_PASSWORD = "some password";
        final Bundle ACCOUNT_BUNDLE = new Bundle();

        Account terraAccount = new Account("TERRA", TYPE);
        Account aquaAccount = new Account("AQUA", TYPE);
        Account ventusAccount = new Account("VENTUS", TYPE);
        authenticatorComponent = new ComponentName(
                getApplicationContext().getPackageName(),
                getApplicationContext().getPackageName()
                 + ".MyAccountauthenticatorComponent");


        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Welcome to Auth App. \nPlease make sure you have: \n\n1. Test App 1\n"
                + "\n2. Test App 2 \n\ninstalled for the demo. These applications"
                + " provide tests, use cases, and proof of concept of Account Discovery API!\n")
                .setTitle("WELCOME")
                .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // do nothing
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();

        getAllRequestingApps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                List<ApplicationInfo> list = getPackageManager().getInstalledApplications(
                        PackageManager.GET_META_DATA);
                StringBuilder uidMasterString = new StringBuilder();
                StringBuilder packageMasterString = new StringBuilder();
                for (ApplicationInfo ai :list) {
                    String label = (String) ai.processName;
                    if (label.contains("pushapi")) {
                        uidMasterString.append(label + "\n");
                    }
                }
                    if (uidMasterString.length() > 0) {
                        getAllRequesting3pUids.setText(uidMasterString);
                    } else {
                        getAllRequesting3pUids.setText("----");
                    }
            }
        });

        selectOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Account currentAccount = terraAccount;
                int checkedAccount = accountChooser.getCheckedRadioButtonId();
                int checkedOption = optionChooser.getCheckedRadioButtonId();
                int checkedApp = packagesChooser.getCheckedRadioButtonId();
                if (checkedApp == -1) {
                    enterPackageName.show();
                } else if (checkedAccount == -1) {
                    chooseAccountWarning.show();
                } else if (checkedOption == -1) {
                    chooseOptionWarning.show();
                } else {
                    // all conditions satisfied
                    if (checkedAccount == R.id.terrabutton) {
                        currentAccount = terraAccount;
                    } else if (checkedAccount == R.id.aquabutton) {
                        currentAccount = aquaAccount;
                    } else if (checkedAccount == R.id.ventusbutton) {
                        currentAccount = ventusAccount;
                    }
                    String packageName =
                            ((RadioButton) findViewById(checkedApp)).getText().toString();
                    switch (checkedOption) {
                        case R.id.visibleButton:
                            am.setAccountVisibility(currentAccount, packageName,
                                    AccountManager.VISIBILITY_USER_MANAGED_VISIBLE);
                            Toast.makeText(
                                    getApplicationContext(), "Set UM_VISIBLE(2) "
                                            + currentAccount.name + " to " + packageName,
                                    Toast.LENGTH_SHORT).show();
                            break;
                        case R.id.notVisibleButton:
                            am.setAccountVisibility(currentAccount, packageName,
                                    AccountManager.VISIBILITY_USER_MANAGED_NOT_VISIBLE);
                            Toast.makeText(
                                    getApplicationContext(), "Set UM_NOT_VISIBLE(4) "
                                            + currentAccount.name + " to " + packageName,
                                    Toast.LENGTH_SHORT).show();
                            break;
                        case R.id.forcedNotVisibleButton:
                            am.setAccountVisibility(currentAccount, packageName,
                                    AccountManager.VISIBILITY_NOT_VISIBLE);
                            Toast.makeText(
                                    getApplicationContext(), "Removing visibility(3) "
                                            + currentAccount.name + " of " + packageName,
                                    Toast.LENGTH_SHORT).show();
                            break;
                        case R.id.getButton:
                            Toast.makeText(getApplicationContext(),
                                    "Is " + currentAccount.name + " visible to " + packageName
                                            + "?\n"
                                            + am.getAccountVisibility(currentAccount, packageName),
                                    Toast.LENGTH_SHORT).show();
                            break;
                        case R.id.addAccountButton:
                            Toast.makeText(getApplicationContext(),
                                    "Adding account explicitly!"
                                            + am.addAccountExplicitly(currentAccount, null, null),
                                    Toast.LENGTH_SHORT).show();
                            break;
                        case R.id.addAccountButtonWithVisibility:
                            HashMap<String, Integer> packageAndVisibilitys = new HashMap<>();
                            packageAndVisibilitys.put(packageName,
                                    AccountManager.VISIBILITY_USER_MANAGED_VISIBLE);
                            Toast.makeText(getApplicationContext(),
                                    "Adding account explicitly!"
                                            + am.addAccountExplicitly(currentAccount, null, null,
                                                    packageAndVisibilitys)
                                            + " with visibility for  " + packageName + "!",
                                    Toast.LENGTH_SHORT).show();
                            break;
                        case R.id.removeAccount:
                            Toast.makeText(getApplicationContext(),
                                    "Removing account explicitly!"
                                            + am.removeAccountExplicitly(currentAccount),
                                    Toast.LENGTH_SHORT).show();
                            break;
                        case R.id.renameAccount:
                            try {
                                AccountManagerFuture<Account> accountRenameFuture =
                                        am.renameAccount(currentAccount, currentAccount.name + "1",
                                                null, null);
                                Account renamedAccount = accountRenameFuture.getResult();
                                Toast.makeText(getApplicationContext(),
                                        "New account name " + renamedAccount, Toast.LENGTH_SHORT)
                                        .show();
                            } catch (Exception e) {
                                Toast.makeText(getApplicationContext(), "Exception" + e,
                                        Toast.LENGTH_SHORT).show();
                            }
                            break;
                        case R.id.enableComponent:
                            Toast.makeText(getApplicationContext(),
                                    "Enabling Component", Toast.LENGTH_SHORT).show();
                            getPackageManager().setComponentEnabledSetting(authenticatorComponent,
                                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                                    PackageManager.DONT_KILL_APP);
                            break;
                        case R.id.disableComponent:
                            Toast.makeText(getApplicationContext(),
                                    "Disabling Component", Toast.LENGTH_SHORT).show();
                            getPackageManager().setComponentEnabledSetting(authenticatorComponent,
                                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                                    PackageManager.DONT_KILL_APP);
                            break;

                    }
                }
            }
        });
    }
}

/*
 * Copyright 2018 Realm Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.realm.todo;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import io.realm.ObjectServerError;
import io.realm.Realm;
import io.realm.SyncConfiguration;
import io.realm.SyncCredentials;
import io.realm.SyncUser;
import io.realm.internal.sync.permissions.ObjectPermissionsModule;

import static io.realm.todo.Constants.AUTH_URL;
import static io.realm.todo.Constants.REALM_BASE_URL;

public class WelcomeActivity extends AppCompatActivity {

    private EditText mUserNameTextView;
    private EditText mPasswordTextView;
    private View mProgressView;
    private View mLoginFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        SyncUser user = SyncUser.currentUser();
        if (user != null) {
            setUpDefaultRealm(user);
            navigateToListOfProject();
        }

        // Set up the login form.
        mUserNameTextView = findViewById(R.id.username);
        mPasswordTextView = findViewById(R.id.password);
        Button loginButton = findViewById(R.id.login_button);
        loginButton.setOnClickListener(view -> attemptLogin());
        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
    }

    private void attemptLogin() {
        // Reset errors.
        mUserNameTextView.setError(null);
        // Store values at the time of the login attempt.
        String username = mUserNameTextView.getText().toString();
        String password = mPasswordTextView.getText().toString();

        showProgress(true);

        SyncCredentials credentials = SyncCredentials.usernamePassword(username, password);
        SyncUser.loginAsync(credentials, AUTH_URL, new SyncUser.Callback<SyncUser>() {
            @Override
            public void onSuccess(SyncUser user) {
                setUpDefaultRealm(user);
                PermissionHelper.initializePermissions(user, () -> navigateToListOfProject());
            }

            @Override
            public void onError(ObjectServerError error) {
                showProgress(false);
                mUserNameTextView.setError("Uh oh something went wrong! (check your logcat please)");
                mUserNameTextView.requestFocus();
                Log.e("Login error", error.toString());
            }
        });
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
        mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });
        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        mProgressView.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void setUpDefaultRealm(SyncUser user) {
        SyncConfiguration configuration = new SyncConfiguration.Builder(
                user,
                REALM_BASE_URL + "/todo")
                .partialRealm()
                .modules(Realm.getDefaultModule(), new ObjectPermissionsModule())
                .build();
        Realm.setDefaultConfiguration(configuration);
    }

    private void navigateToListOfProject() {
        Intent intent = new Intent(WelcomeActivity.this, ProjectsActivity.class);
        startActivity(intent);
    }
}


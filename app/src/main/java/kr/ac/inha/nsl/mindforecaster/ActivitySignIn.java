package kr.ac.inha.nsl.mindforecaster;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ActivitySignIn extends AppCompatActivity {

    // region Constants
    static final String KEY_USERNAME = "username";
    static final String KEY_PASSWORD = "password";

    final int GPS_PERMISSION_REQUEST_CODE = 1;
    final int USAGE_ACCESS_PERMISSION_REQUEST_CODE = 2;
    // endregion

    // region Variables
    static SharedPreferences loginPrefs = null;
    private EditText userLogin;
    private EditText userPassword;
    private RelativeLayout loadingPanel;
    // endregion

    // region Override
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);

        boolean askForPermission = false;
        if (Tools.isLocationPermissionDenied(this)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, GPS_PERMISSION_REQUEST_CODE);
            askForPermission = true;
        }
        if (Tools.isUsageAccessDenied(this)) {
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            startActivityForResult(intent, USAGE_ACCESS_PERMISSION_REQUEST_CODE);
            askForPermission = true;
        }
        if (askForPermission)
            Toast.makeText(this, "Please grant Usage Access and Location permissions first to use this app!", Toast.LENGTH_LONG).show();

        Tools.init(this);
        init();
    }

    @Override
    protected void onStop() {
        super.onStop();
        loadingPanel.setVisibility(View.GONE);
    }
    // endregion

    private void init() {
        // region Initialize UI Variables
        userLogin = findViewById(R.id.txt_login);
        userPassword = findViewById(R.id.txt_password);
        loadingPanel = findViewById(R.id.loadingPanel);
        // endregion

        if (loginPrefs == null)
            loginPrefs = getSharedPreferences("UserLogin", 0);

        if (loginPrefs.contains(ActivitySignIn.KEY_USERNAME) && loginPrefs.contains(ActivitySignIn.KEY_PASSWORD)) {
            loadingPanel.setVisibility(View.VISIBLE);
            signIn(loginPrefs.getString(ActivitySignIn.KEY_USERNAME, null), loginPrefs.getString(ActivitySignIn.KEY_PASSWORD, null));
        } else
            Toast.makeText(this, "Not logged in yet!)", Toast.LENGTH_SHORT).show();

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (mNotificationManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(getString(R.string.notif_channel_id), name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null)
                notificationManager.createNotificationChannel(channel);
        }
    }

    public void signInClick(View view) {
        signIn(userLogin.getText().toString(), userPassword.getText().toString());
    }

    public void signUpClick(View view) {
        Intent intent = new Intent(this, ActivitySignUp.class);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    public void signIn(String username, String password) {
        loadingPanel.setVisibility(View.VISIBLE);

        if (Tools.isUsageAccessDenied(this) || Tools.isLocationPermissionDenied(this)) {
            Toast.makeText(this, "Please grant Usage Access and Location permissions first to use this app!", Toast.LENGTH_SHORT).show();

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, GPS_PERMISSION_REQUEST_CODE);
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            startActivityForResult(intent, USAGE_ACCESS_PERMISSION_REQUEST_CODE);

            loadingPanel.setVisibility(View.GONE);
            return;
        }

        if (Tools.isNetworkAvailable())
            Tools.execute(new MyRunnable(
                    this,
                    getString(R.string.url_login, getString(R.string.server_ip)),
                    username,
                    password
            ) {
                @Override
                public void run() {
                    String url = (String) args[0];
                    String username = (String) args[1];
                    String password = (String) args[2];

                    try {
                        List<NameValuePair> params = new ArrayList<>();
                        params.add(new BasicNameValuePair("username", username));
                        params.add(new BasicNameValuePair("password", password));

                        JSONObject json = new JSONObject(Tools.post(url, params));

                        switch (json.getInt("result")) {
                            case Tools.RES_OK:
                                runOnUiThread(new MyRunnable(activity, args) {
                                    @Override
                                    public void run() {
                                        String username = (String) args[1];
                                        String password = (String) args[2];

                                        SharedPreferences.Editor editor = ActivitySignIn.loginPrefs.edit();
                                        editor.putString(ActivitySignIn.KEY_USERNAME, username);
                                        editor.putString(ActivitySignIn.KEY_PASSWORD, password);
                                        editor.apply();

                                        if (ActivitySignIn.loginPrefs.getBoolean("firstTime", true)) {
                                            Calendar sundayNotifTime = Calendar.getInstance(Locale.getDefault());
                                            sundayNotifTime.set(Calendar.DAY_OF_WEEK, 1);
                                            sundayNotifTime.set(Calendar.HOUR_OF_DAY, 20);
                                            sundayNotifTime.set(Calendar.MINUTE, 0);
                                            sundayNotifTime.set(Calendar.SECOND, 0);
                                            DialogNotificationSettings.sunday = (Calendar) sundayNotifTime.clone();
                                            editor.putLong("SundayReminderTime", sundayNotifTime.getTimeInMillis());
                                            Tools.addSundayNotification(ActivitySignIn.this, sundayNotifTime);

                                            Calendar dailyNotifTime = Calendar.getInstance(Locale.getDefault());
                                            dailyNotifTime.set(Calendar.HOUR_OF_DAY, 8);
                                            dailyNotifTime.set(Calendar.MINUTE, 0);
                                            dailyNotifTime.set(Calendar.SECOND, 0);
                                            DialogNotificationSettings.everyMorning = (Calendar) dailyNotifTime.clone();
                                            editor.putLong("EveryMorningReminderTime", dailyNotifTime.getTimeInMillis());
                                            Tools.addDailyNotification(ActivitySignIn.this, dailyNotifTime, getString(R.string.daily_notif_question), false);

                                            dailyNotifTime.set(Calendar.HOUR_OF_DAY, 22);
                                            dailyNotifTime.set(Calendar.MINUTE, 0);
                                            dailyNotifTime.set(Calendar.SECOND, 0);
                                            DialogNotificationSettings.everyEvening = (Calendar) dailyNotifTime.clone();
                                            editor.putLong("EveryEveningReminderTime", dailyNotifTime.getTimeInMillis());
                                            Tools.addDailyNotification(ActivitySignIn.this, dailyNotifTime, getString(R.string.daily_notif_request), true);

                                            editor.putBoolean("firstTime", false);
                                            editor.apply();
                                        }

                                        Intent intent = new Intent(ActivitySignIn.this, ActivityMain.class);
                                        if (getIntent().hasExtra("eventDate")) {
                                            intent.putExtra("eventDate", getIntent().getLongExtra("eventDate", 0));
                                            intent.putExtra("isEvaluate", getIntent().getBooleanExtra("isEvaluate", false));
                                        }
                                        startActivity(intent);
                                        finish();
                                        overridePendingTransition(R.anim.activity_in, R.anim.activity_out);
                                    }
                                });
                                break;
                            case Tools.RES_FAIL:
                                Thread.sleep(2000);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(ActivitySignIn.this, "Failed to sign in.", Toast.LENGTH_SHORT).show();
                                        loadingPanel.setVisibility(View.GONE);
                                    }
                                });
                                break;
                            case Tools.RES_SRV_ERR:
                                Thread.sleep(2000);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(ActivitySignIn.this, "Failed to sign in. (SERVER SIDE ERROR)", Toast.LENGTH_SHORT).show();
                                        loadingPanel.setVisibility(View.GONE);
                                    }
                                });
                                break;
                            default:
                                break;
                        }
                    } catch (JSONException | InterruptedException | IOException e) {
                        e.printStackTrace();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(ActivitySignIn.this, "Failed to sign in.", Toast.LENGTH_SHORT).show();
                                loadingPanel.setVisibility(View.GONE);
                            }
                        });
                    }
                    enableTouch();
                }
            });
        else if (loginPrefs.getString(ActivitySignIn.KEY_USERNAME, null) != null && loginPrefs.getString(ActivitySignIn.KEY_PASSWORD, null) != null) {
            Toast.makeText(this, "Please connect to internet first!", Toast.LENGTH_SHORT).show();
            // Intent intent = new Intent(ActivitySignIn.this, ActivityMain.class);
            // if (getIntent().hasExtra("eventDate")) {
            //     intent.putExtra("eventDate", getIntent().getLongExtra("eventDate", 0));
            //     intent.putExtra("isEvaluate", getIntent().getBooleanExtra("isEvaluate", false));
            // }
            // startActivity(intent);
            // finish();
            // overridePendingTransition(R.anim.activity_in, R.anim.activity_out);
        }
    }
}

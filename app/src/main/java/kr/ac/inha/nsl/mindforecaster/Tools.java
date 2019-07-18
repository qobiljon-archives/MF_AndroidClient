package kr.ac.inha.nsl.mindforecaster;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AppOpsManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.SparseArray;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.System.currentTimeMillis;

public class Tools {

    // region Constants
    static final long LAST_REBOOT_TIMESTAMP = currentTimeMillis() - SystemClock.elapsedRealtime();
    static final short RES_OK = 0;
    static final short RES_SRV_ERR = -1;
    static final short RES_FAIL = 1;
    // endregion

    // region Variables
    static String PACKAGE_NAME;
    static File locationDataFile;
    static File activityRecognitionDataFile;
    private static int cellWidth, cellHeight;

    private static ExecutorService executor = Executors.newCachedThreadPool();

    private static SparseArray<PendingIntent> eventNotifs = new SparseArray<>();
    private static SparseArray<PendingIntent> intervNotifs = new SparseArray<>();
    private static SparseArray<PendingIntent> sundayNotifs = new SparseArray<>();
    private static SparseArray<PendingIntent> dailyNotifs = new SparseArray<>();

    private static ConnectivityManager connectivityManager;

    private static UsageStatsManager usageStatsManager;
    private static String usageStatsSubmitUrl;
    private static String locationDataSubmitUrl;
    private static String activityRecognitionSubmitUrl;

    static boolean init(final Activity activity) {
        boolean allPermissionsProvided = true;

        // set up internet connectivity checker
        PACKAGE_NAME = activity.getPackageName();

        connectivityManager = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        usageStatsManager = (UsageStatsManager) activity.getSystemService(Context.USAGE_STATS_SERVICE);

        usageStatsSubmitUrl = activity.getString(R.string.url_usage_stats_submit, activity.getString(R.string.server_ip));
        locationDataSubmitUrl = activity.getString(R.string.url_location_data_submit, activity.getString(R.string.server_ip));
        activityRecognitionSubmitUrl = activity.getString(R.string.url_activity_recognition_submit, activity.getString(R.string.server_ip));

        // check for permissions needed for data collection
        // check app-usage permissions
        if (!usageAccessIsGranted(activity)) {
            Toast.makeText(activity, "Please provide usage access to this app in settings!", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            activity.startActivity(intent);
            allPermissionsProvided = false;
        }
        // check GPS location permissions (Google Play services => Last Known Location + Activity Recognition API)
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            allPermissionsProvided = false;
        }

        return allPermissionsProvided;
    }

    static void initDataCollectorService(final Activity activity) {
        if (!DataCollectorService.isServiceRunning)
            activity.startService(new Intent(activity, DataCollectorService.class));
    }
    // endregion

    static synchronized String post(String url, List<NameValuePair> params) throws IOException {
        HttpPost httppost = new HttpPost(url);
        @SuppressWarnings("deprecation")
        HttpClient httpclient = new DefaultHttpClient();
        httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
        HttpResponse response = httpclient.execute(httppost);

        checkAndSendUsageAccessStats();
        checkAndSendLocationData();
        checkAndSendActivityData();

        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
            return Tools.inputStreamToString(response.getEntity().getContent());
        else return null;
    }

    static void checkAndSendUsageAccessStats() throws IOException {
        if (usageStatsManager == null)
            return;

        long lastSavedTimestamp = SignInActivity.loginPrefs.getLong("lastUsageSubmissionTime", -1);

        Calendar fromCal = Calendar.getInstance(Locale.getDefault());
        if (lastSavedTimestamp == -1)
            fromCal.add(Calendar.YEAR, -1);
        else
            fromCal.setTime(new Date(lastSavedTimestamp));
        Calendar tillCal = Calendar.getInstance(Locale.getDefault());
        tillCal.set(Calendar.MILLISECOND, 0);

        StringBuilder sb = new StringBuilder();
        for (UsageStats stats : usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, fromCal.getTimeInMillis(), currentTimeMillis()))
            if (stats.getPackageName().equals(PACKAGE_NAME))
                if (sb.length() == 0)
                    sb.append(String.format(
                            Locale.getDefault(),
                            "%d %d",
                            stats.getLastTimeUsed() / 1000,
                            stats.getTotalTimeInForeground() / 1000
                    ));
                else
                    sb.append(String.format(
                            Locale.getDefault(),
                            ",%d %d",
                            stats.getLastTimeUsed() / 1000,
                            stats.getTotalTimeInForeground() / 1000
                    ));

        if (sb.length() > 0) {
            HttpPost httppost = new HttpPost(usageStatsSubmitUrl);
            @SuppressWarnings("deprecation")
            HttpClient httpclient = new DefaultHttpClient();
            ArrayList<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("username", SignInActivity.loginPrefs.getString("username", null)));
            params.add(new BasicNameValuePair("password", SignInActivity.loginPrefs.getString("password", null)));
            params.add(new BasicNameValuePair("app_usage", sb.toString()));
            httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
            HttpResponse response = httpclient.execute(httppost);

            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                SharedPreferences.Editor editor = SignInActivity.loginPrefs.edit();
                editor.putLong("lastUsageSubmissionTime", tillCal.getTimeInMillis());
                editor.apply();
            }
        }

    }

    static synchronized void checkAndSendLocationData() throws IOException {
        if (locationDataFile == null || locationDataSubmitUrl == null)
            return;

        String locationData = readLocationData();
        if (locationData.length() == 0)
            return;

        if (!Character.isDigit(locationData.charAt(locationData.length() - 1)))
            locationData = locationData.substring(0, locationData.length() - 1);

        HttpPost httppost = new HttpPost(locationDataSubmitUrl);
        @SuppressWarnings("deprecation")
        HttpClient httpclient = new DefaultHttpClient();
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("username", SignInActivity.loginPrefs.getString("username", null)));
        params.add(new BasicNameValuePair("password", SignInActivity.loginPrefs.getString("password", null)));
        params.add(new BasicNameValuePair("data", locationData));
        httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
        HttpResponse response = httpclient.execute(httppost);

        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            if (locationDataFile.delete())
                //noinspection ResultOfMethodCallIgnored
                locationDataFile.createNewFile();
        }
    }

    static void checkAndSendActivityData() throws IOException {
        if (activityRecognitionDataFile == null)
            return;

        String activityRecognitionData = readActivityRecognitionData();
        if (activityRecognitionData.length() == 0)
            return;

        if (!Character.isDigit(activityRecognitionData.charAt(activityRecognitionData.length() - 1)))
            activityRecognitionData = activityRecognitionData.substring(0, activityRecognitionData.length() - 1);

        HttpPost httppost = new HttpPost(activityRecognitionSubmitUrl);
        @SuppressWarnings("deprecation")
        HttpClient httpclient = new DefaultHttpClient();
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("username", SignInActivity.loginPrefs.getString("username", null)));
        params.add(new BasicNameValuePair("password", SignInActivity.loginPrefs.getString("password", null)));
        params.add(new BasicNameValuePair("data", activityRecognitionData));
        httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
        HttpResponse response = httpclient.execute(httppost);

        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            if (activityRecognitionDataFile.delete())
                //noinspection ResultOfMethodCallIgnored
                activityRecognitionDataFile.createNewFile();
        }
    }

    static void execute(Runnable runnable) {
        if (runnable instanceof MyRunnable)
            disable_touch(((MyRunnable) runnable).activity);
        executor.execute(runnable);
    }


    @SuppressWarnings("unused")
    private static boolean isLocationEnabled(Context context) {
        try {
            return Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE) != Settings.Secure.LOCATION_MODE_OFF;
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean usageAccessIsGranted(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(context.getPackageName(), 0);
            AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            int mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, applicationInfo.uid, applicationInfo.packageName);
            return (mode == AppOpsManager.MODE_ALLOWED);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    static boolean isNetworkAvailable() {
        if (connectivityManager == null)
            return false;
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }


    static synchronized void storeLocationData(long timestamp, double latitude, double longitude, double altitude) throws IOException {
        FileWriter writer = new FileWriter(locationDataFile, true);
        writer.write(String.format(
                Locale.getDefault(),
                "%d %f %f %f\n",
                timestamp / 1000,
                latitude,
                longitude,
                altitude
        ));
        writer.close();
    }

    private static synchronized String readLocationData() throws IOException {
        StringBuilder result = new StringBuilder();

        FileReader reader = new FileReader(locationDataFile);
        char[] buf = new char[128];
        int read;
        while ((read = reader.read(buf)) > 0)
            result.append(buf, 0, read);

        return result.toString();
    }

    static synchronized void storeActivityRecognitionData(long timestamp, String activity, String transition) throws IOException {
        FileWriter writer = new FileWriter(activityRecognitionDataFile, true);
        writer.write(String.format(
                Locale.getDefault(),
                "%d %s %s\n",
                timestamp / 1000,
                activity,
                transition
        ));
        writer.close();
    }

    private static synchronized String readActivityRecognitionData() throws IOException {
        StringBuilder result = new StringBuilder();

        FileReader reader = new FileReader(activityRecognitionDataFile);
        char[] buf = new char[128];
        int read;
        while ((read = reader.read(buf)) > 0)
            result.append(buf, 0, read);

        return result.toString();
    }


    static void setCellSize(int width, int height) {
        cellWidth = width;
        cellHeight = height;
    }

    static void cellClearOut(ViewGroup[][] grid, int row, int col, Activity activity, ViewGroup parent, LinearLayout.OnClickListener cellClickListener) {
        if (grid[row][col] == null) {
            activity.getLayoutInflater().inflate(R.layout.date_cell, parent, true);
            ViewGroup res = (ViewGroup) parent.getChildAt(parent.getChildCount() - 1);
            res.getLayoutParams().width = cellWidth;
            res.getLayoutParams().height = cellHeight;
            res.setOnClickListener(cellClickListener);
            grid[row][col] = res;
        } else {
            TextView date_text = grid[row][col].findViewById(R.id.date_text_view);
            date_text.setTextColor(activity.getColor(R.color.textColor));
            date_text.setBackground(null);

            while (grid[row][col].getChildCount() > 1)
                grid[row][col].removeViewAt(1);
        }
    }


    private static String inputStreamToString(InputStream is) throws IOException {
        InputStreamReader reader = new InputStreamReader(is);
        StringBuilder sb = new StringBuilder();

        char[] buf = new char[128];
        int read;
        while ((read = reader.read(buf)) > 0)
            sb.append(buf, 0, read);

        reader.close();
        return sb.toString();
    }

    @SuppressWarnings("unused")
    private static String convertFromUTF8(byte[] raw) {
        return new String(raw, StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unused")
    private static String convertToUTF8(String s) {
        return new String(s.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
    }

    @ColorInt
    static int stressLevelToColor(Context context, int level) {
        switch (level) {
            case 0:
                return ResourcesCompat.getColor(context.getResources(), R.color.slvl0_color, null);
            case 1:
                return ResourcesCompat.getColor(context.getResources(), R.color.slvl1_color, null);
            case 2:
                return ResourcesCompat.getColor(context.getResources(), R.color.slvl2_color, null);
            case 3:
                return ResourcesCompat.getColor(context.getResources(), R.color.slvl3_color, null);
            case 4:
                return ResourcesCompat.getColor(context.getResources(), R.color.slvl4_color, null);
            default:
                return 0;
        }
       /* float c = 5.11f;

        if (level > 98)
            return Color.RED;
        else if (level < 50)
            return Color.argb(0xff, (int) (level * c), 0xff, 0);
        else
            return Color.argb(0xff, 0xff, (int) (c * (100 - level)), 0);*/
    }


    private static void writeToFile(Context context, String fileName, String data) {
        try {
            FileOutputStream outputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
            outputStreamWriter.write(data);
            outputStreamWriter.close();
            outputStream.close();
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    private static String readFromFile(Context context, String fileName) {
        String ret = "[]";

        try {
            InputStream inputStream = context.openFileInput(fileName);

            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString;
                StringBuilder stringBuilder = new StringBuilder();

                while ((receiveString = bufferedReader.readLine()) != null)
                    stringBuilder.append(receiveString);

                bufferedReader.close();
                inputStream.close();
                ret = stringBuilder.toString();
            }
        } catch (FileNotFoundException e) {
            Log.e("Exception", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("Exception", "Can not read file: " + e.toString());
        }

        return ret;
    }


    static void cacheMonthlyEvents(Context context, Event[] events, int month, int year) {
        if (events.length == 0)
            return;

        JSONArray array = new JSONArray();
        for (Event event : events)
            array.put(event.toJson());

        Tools.writeToFile(context, String.format(Locale.getDefault(), "events_%02d_%d.json", month, year), array.toString());
    }

    static Event[] readOfflineMonthlyEvents(Context context, int month, int year) {
        JSONArray array;
        try {
            array = new JSONArray(readFromFile(context, String.format(Locale.getDefault(), "events_%02d_%d.json", month, year)));
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        try {
            Event[] res = new Event[array.length()];
            for (int n = 0; n < array.length(); n++) {
                res[n] = new Event(1);
                res[n].fromJson(array.getJSONObject(n));
            }
            return res;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void cacheInterventions(Context context, Intervention[] interventions, String type) throws JSONException {
        if (interventions == null || interventions.length == 0)
            return;

        JSONArray array = new JSONArray();
        for (Intervention intervention : interventions)
            array.put(intervention.to_json());

        Tools.writeToFile(context, String.format(Locale.getDefault(), "%s_interventions.json", type), array.toString());
    }

    static void cacheSystemInterventions(Context context, Intervention[] sysInterventions) throws JSONException {
        cacheInterventions(context, sysInterventions, "system");
    }

    static void cacheSurveys(Context context, JSONArray survey1, JSONArray survey2, JSONArray survey3) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("survey1", survey1);
        obj.put("survey2", survey2);
        obj.put("survey3", survey3);
        Tools.writeToFile(context, "survey.json", obj.toString());
    }

    static void cachePeerInterventions(Context context, Intervention[] peerInterventions) throws JSONException {
        cacheInterventions(context, peerInterventions, "peer");
    }

    private static Intervention[] readOfflineInterventions(Context context, String type) throws JSONException {
        JSONArray array = new JSONArray(readFromFile(context, String.format(Locale.getDefault(), "%s_interventions.json", type)));

        Intervention[] res = new Intervention[array.length()];
        for (int n = 0; n < array.length(); n++)
            res[n] = Intervention.from_json(new JSONObject(array.getString(n)));
        return res;
    }

    static JSONArray[] loadOfflineSurvey(Context context) {
        try {
            JSONObject obj = new JSONObject(readFromFile(context, "survey.json"));
            return new JSONArray[]{
                    obj.getJSONArray("survey1"),
                    obj.getJSONArray("survey2"),
                    obj.getJSONArray("survey3")
            };
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    static Intervention[] readOfflineSystemInterventions(Context context) throws JSONException {
        return readOfflineInterventions(context, "system");
    }

    static Intervention[] readOfflinePeerInterventions(Context context) throws JSONException {
        return readOfflineInterventions(context, "peer");
    }


    static void addDailyNotif(Context context, Calendar when, String text, boolean isEvaluate) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlaramReceiverEveryDay.class);
        intent.putExtra("Content", text);
        intent.putExtra("notification_id", when.getTimeInMillis());
        intent.putExtra("isEvaluated", isEvaluate);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, (int) when.getTimeInMillis(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (alarmManager != null)
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, when.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
        dailyNotifs.put((int) when.getTimeInMillis(), pendingIntent);
    }

    static void addSundayNotif(Context context, Calendar when) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiverEverySunday.class);
        intent.putExtra("Content", context.getString(R.string.sunday_notif_question));
        intent.putExtra("notification_id", when.getTimeInMillis());
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, (int) when.getTimeInMillis(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (alarmManager != null)
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, when.getTimeInMillis(), AlarmManager.INTERVAL_DAY * 7, pendingIntent);
        sundayNotifs.put((int) when.getTimeInMillis(), pendingIntent);
    }

    static void addEventNotif(Context context, Calendar when, long event_id, String text) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiverEvent.class);
        intent.putExtra("Content", text);
        intent.putExtra("EventId", event_id);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, (int) event_id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (alarmManager != null)
            alarmManager.set(AlarmManager.RTC_WAKEUP, when.getTimeInMillis(), pendingIntent);
        eventNotifs.put((int) event_id, pendingIntent);
    }

    static void addInterventionNotification(Context context, Calendar when, long event_id, String intervText, String eventText) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiverIntervention.class);
        intent.putExtra("Content1", intervText);
        intent.putExtra("Content2", eventText);
        intent.putExtra("notification_id", event_id);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, (int) event_id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (alarmManager != null)
            alarmManager.set(AlarmManager.RTC_WAKEUP, when.getTimeInMillis(), pendingIntent);
        intervNotifs.put((int) event_id, pendingIntent);
    }

    static void cancelNotif(Context context, int notif_id) {
        SparseArray<PendingIntent> map;
        if (dailyNotifs.get(notif_id, null) != null)
            map = dailyNotifs;
        else if (sundayNotifs.get(notif_id, null) != null)
            map = sundayNotifs;
        else if (eventNotifs.get(notif_id, null) != null)
            map = eventNotifs;
        else if (intervNotifs.get(notif_id, null) != null)
            map = intervNotifs;
        else
            return;

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null)
            alarmManager.cancel(map.get(notif_id));
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null)
            notificationManager.cancel(notif_id);

        map.remove(notif_id);
    }


    private static void disable_touch(Activity activity) {
        activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    static void enable_touch(Activity activity) {
        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }


    static <T> void shuffle(T[] array) {
        int n = array.length;
        Random random = new Random();
        // Loop over array.
        for (int i = 0; i < array.length; i++) {
            int randIndex = i + random.nextInt(n - i);
            T randomElement = array[randIndex];
            array[randIndex] = array[i];
            array[i] = randomElement;
        }
    }

    static String notificationMinutesToString(Context context, int minsValue) {
        if (minsValue == 0)
            return context.getString(R.string.none);

        StringBuilder sb = new StringBuilder();

        boolean before = minsValue < 0;
        minsValue = Math.abs(minsValue);
        short days = (short) (minsValue / 1440);
        short hrs = (short) (minsValue % 1440 / 60);
        short mins = (short) (minsValue % 1440 % 60);

        if (days > 0)
            sb.append(String.format(Locale.getDefault(), " %d %s", days, context.getString(R.string.days)));
        if (hrs > 0)
            sb.append(String.format(Locale.getDefault(), " %d %s", hrs, context.getResources().getStringArray(R.array.time_scale_values)[1]));
        if (mins > 0)
            sb.append(String.format(Locale.getDefault(), " %d %s", mins, context.getString(R.string.minutes)));
        sb.append(String.format(Locale.getDefault(), " %s", before ? context.getString(R.string.before) : context.getString(R.string.after)));

        String res = sb.toString();
        if (res.startsWith(" "))
            res = res.substring(1);
        return res;
    }

    static <T> void swap(T[] array, int a, int b) {
        T tmp = array[a];
        array[a] = array[b];
        array[b] = tmp;
    }
}

abstract class MyRunnable implements Runnable {
    Object[] args;
    Activity activity;

    MyRunnable(Activity activity, Object... args) {
        this.activity = activity;
        this.args = Arrays.copyOf(args, args.length);
    }

    void enableTouch() {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Tools.enable_touch(activity);
            }
        });
    }
}

class Event {
    static final int NO_REPEAT = 0, REPEAT_EVERYDAY = 1, REPEAT_WEEKLY = 2;
    //region Variables
    static Event[] currentEventBank;
    private static LongSparseArray<Event> idEventMap = new LongSparseArray<>();
    private boolean newEvent;
    private long id;
    private String title = "";
    private int stressLevel = -1;
    private int realStressLevel = -1;
    private Calendar startTime;
    private Calendar endTime;
    private String intervention;
    private int interventionReminder;
    private long interventionLastPickedTime;
    private String stressType = "unknown";
    private String stressCause = "";
    private long repeatId;
    private long repeatTill;
    private int repeatMode;
    private int eventReminder;
    private boolean evaluated;

    Event(long id) {
        newEvent = id == 0;
        if (newEvent)
            this.id = currentTimeMillis() / 1000;
        else
            this.id = id;
    }

    static synchronized ArrayList<Event> getOneDayEvents(@NonNull Calendar day) {
        return getOneDayEvents(currentEventBank, day);
    }

    private static ArrayList<Event> getOneDayEvents(Event[] eventBank, @NonNull Calendar day) {
        ArrayList<Event> res = new ArrayList<>();

        if (eventBank == null || eventBank.length == 0)
            return res;

        Calendar comDay = (Calendar) day.clone();
        comDay.set(Calendar.HOUR_OF_DAY, 0);
        comDay.set(Calendar.MINUTE, 0);
        comDay.set(Calendar.SECOND, 0);
        comDay.set(Calendar.MILLISECOND, 0);
        long periodFrom = comDay.getTimeInMillis();

        comDay.add(Calendar.DAY_OF_MONTH, 1);
        comDay.add(Calendar.MINUTE, -1);
        long periodTill = comDay.getTimeInMillis();

        for (Event event : eventBank) {
            long evStartTime = event.getStartTime().getTimeInMillis();
            long evEndTime = event.getEndTime().getTimeInMillis();

            if (periodFrom <= evStartTime && evStartTime < periodTill)
                res.add(event);
            else if (periodFrom < evEndTime && evEndTime <= periodTill)
                res.add(event);
            else if (evStartTime <= periodFrom && periodTill <= evEndTime)
                res.add(event);
        }

        return res;
    }
    //endregion

    static void setCurrentEventBank(Event[] bank) {
        currentEventBank = bank;

        idEventMap.clear();
        for (Event event : currentEventBank)
            idEventMap.put(event.id, event);
    }

    static Event getEventById(long key) {
        return idEventMap.get(key);
    }

    static void updateEventReminders(Context context) {
        Calendar today = Calendar.getInstance(Locale.getDefault()), cal;
        for (Event event : currentEventBank) {
            if (event.getEventReminder() != 0) {
                cal = event.getStartTime();
                cal.add(Calendar.MINUTE, event.getEventReminder());
                if (cal.before(today))
                    Tools.cancelNotif(context, (int) event.getEventId());
                else {
                    String reminderStr = Tools.notificationMinutesToString(context, event.getEventReminder());
                    reminderStr = reminderStr.substring(0, reminderStr.lastIndexOf(' '));
                    if (reminderStr.equals("1 " + context.getString(R.string.days)))
                        reminderStr = context.getString(R.string.tomorrow);
                    else
                        reminderStr = String.format(context.getString(R.string.notification_event_time), context.getString(R.string.after), reminderStr);
                    Tools.addEventNotif(context, cal, event.getEventId(), String.format(context.getResources().getString(R.string.notification_event), event.getTitle(), reminderStr));
                }
            }
        }
    }

    static void updateInterventionReminders(Context context) {
        Calendar today = Calendar.getInstance(Locale.getDefault()), calIntervBeforeEvent, calIntervAfterEvent;
        for (Event event : currentEventBank) {
            Calendar calIntervNotifId = Calendar.getInstance(Locale.getDefault());
            calIntervNotifId.setTimeInMillis(event.getStartTime().getTimeInMillis());
            calIntervNotifId.add(Calendar.MILLISECOND, 1);

            if (event.getInterventionReminder() < 0) {
                calIntervBeforeEvent = event.getStartTime();
                calIntervBeforeEvent.add(Calendar.MINUTE, event.getInterventionReminder());
                if (calIntervBeforeEvent.before(today)) {
                    Tools.cancelNotif(context, (int) calIntervNotifId.getTimeInMillis());
                } else
                    Tools.addInterventionNotification(
                            context,
                            calIntervBeforeEvent,
                            (int) calIntervNotifId.getTimeInMillis(),
                            String.format(
                                    Locale.getDefault(),
                                    "%s: %s",
                                    context.getString(R.string.intervention),
                                    event.getIntervention()
                            ),
                            String.format(
                                    Locale.getDefault(),
                                    "%s: %s",
                                    context.getString(R.string.upcoming_event),
                                    event.getTitle()
                            )
                    );
            } else if (event.getInterventionReminder() != 0) {
                calIntervAfterEvent = event.getEndTime();
                calIntervAfterEvent.add(Calendar.MINUTE, event.getInterventionReminder());
                if (calIntervAfterEvent.before(today)) {
                    Tools.cancelNotif(context, (int) calIntervNotifId.getTimeInMillis());
                } else
                    Tools.addInterventionNotification(
                            context,
                            calIntervAfterEvent,
                            (int) calIntervNotifId.getTimeInMillis(),
                            String.format(
                                    Locale.getDefault(),
                                    "%s: %s",
                                    context.getString(R.string.intervention),
                                    event.getIntervention()
                            ),
                            String.format(
                                    Locale.getDefault(),
                                    "%s: %s",
                                    context.getString(R.string.passed_event),
                                    event.getTitle()
                            )
                    );
            }
        }
    }

    boolean isNewEvent() {
        return newEvent;
    }

    long getEventId() {
        return id;
    }

    Calendar getStartTime() {
        return (Calendar) startTime.clone();
    }

    void setStartTime(Calendar startTime) {
        this.startTime = (Calendar) startTime.clone();
        this.startTime.set(Calendar.SECOND, 0);
        this.startTime.set(Calendar.MILLISECOND, 0);
    }

    Calendar getEndTime() {
        return (Calendar) endTime.clone();
    }

    void setEndTime(Calendar endTime) {
        this.endTime = (Calendar) endTime.clone();
        this.endTime.set(Calendar.SECOND, 0);
        this.endTime.set(Calendar.MILLISECOND, 0);
    }

    int getStressLevel() {
        return stressLevel;
    }

    void setStressLevel(int stressLevel) {
        this.stressLevel = stressLevel;
    }

    int getRealStressLevel() {
        return realStressLevel;
    }

    private void setRealStressLevel(int realStressLevel) {
        this.realStressLevel = realStressLevel;
    }

    String getTitle() {
        return title;
    }

    void setTitle(String title) {
        this.title = title;
    }

    String getIntervention() {
        return intervention;
    }

    void setIntervention(String intervention) {
        this.intervention = intervention;
    }

    String getStressType() {
        return stressType;
    }

    void setStressType(String stressType) {
        this.stressType = stressType;
    }

    String getStressCause() {
        return stressCause;
    }

    void setStressCause(String stressCause) {
        this.stressCause = stressCause;
    }

    int getRepeatMode() {
        return repeatMode;
    }

    void setRepeatMode(int repeatMode) {
        this.repeatMode = repeatMode;
    }

    long getRepeatTill() {
        return repeatTill;
    }

    void setRepeatTill(long repeatTill) {
        this.repeatTill = repeatTill;
    }

    long getRepeatId() {
        return repeatId;
    }

    private void setRepeatId(long repeatId) {
        this.repeatId = repeatId;
    }

    int getInterventionReminder() {
        return interventionReminder;
    }

    void setInterventionReminder(int interventionReminder) {
        this.interventionReminder = interventionReminder;
    }

    int getEventReminder() {
        return eventReminder;
    }

    void setEventReminder(int eventReminder) {
        this.eventReminder = eventReminder;
    }

    boolean isEvaluated() {
        return evaluated;
    }

    void setEvaluated(boolean evaluated) {
        this.evaluated = evaluated;
    }

    long getInterventionLastPickedTime() {
        return interventionLastPickedTime;
    }

    private void setInterventionLastPickedTime(long interventionLastPickedTime) {
        this.interventionLastPickedTime = interventionLastPickedTime;
    }

    JSONObject toJson() {
        JSONObject eventJson = new JSONObject();

        try {
            eventJson.put("eventId", getEventId());
            eventJson.put("title", getTitle());
            eventJson.put("stressLevel", getStressLevel());
            eventJson.put("realStressLevel", getRealStressLevel());
            eventJson.put("startTime", getStartTime().getTimeInMillis());
            eventJson.put("endTime", getEndTime().getTimeInMillis());
            eventJson.put("intervention", getIntervention());
            eventJson.put("interventionReminder", getInterventionReminder());
            eventJson.put("interventionLastPickedTime", getInterventionLastPickedTime());
            eventJson.put("stressType", getStressType());
            eventJson.put("stressCause", getStressCause());
            eventJson.put("repeatMode", getRepeatMode());
            eventJson.put("repeatId", getRepeatId());
            eventJson.put("repeatTill", getRepeatTill());
            eventJson.put("eventReminder", getEventReminder());
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        return eventJson;
    }

    void fromJson(JSONObject eventJson) {
        try {
            Calendar startTime = Calendar.getInstance(Locale.getDefault()), endTime = Calendar.getInstance(Locale.getDefault());
            startTime.setTimeInMillis(eventJson.getLong("startTime") * 1000);
            endTime.setTimeInMillis(eventJson.getLong("endTime") * 1000);

            id = eventJson.getLong("eventId");
            setTitle(eventJson.getString("title"));
            String stressLevelStr = String.valueOf(eventJson.get("stressLevel"));
            setStressLevel(stressLevelStr.equals("N/A") ? -1 : Integer.parseInt(stressLevelStr));
            stressLevelStr = String.valueOf(eventJson.get("realStressLevel"));
            setRealStressLevel(stressLevelStr.equals("N/A") ? -1 : Integer.parseInt(stressLevelStr));
            setStartTime(startTime);
            setEndTime(endTime);
            setIntervention(eventJson.getString("intervention").equals("N/A") ? null : new JSONObject(eventJson.getString("intervention")).getString("description"));
            setInterventionReminder((short) eventJson.getInt("interventionReminder"));
            setStressType(eventJson.getString("stressType").equals("N/A") ? "" : eventJson.getString("stressType"));
            setStressCause(eventJson.getString("stressCause").equals("N/A") ? "" : eventJson.getString("stressCause"));
            setRepeatMode(eventJson.getInt("repeatMode"));
            setRepeatId(eventJson.getLong("repeatId"));
            setInterventionLastPickedTime(eventJson.getLong("interventionLastPickedTime"));
            setRepeatTill(eventJson.getLong("repeatTill"));
            setEventReminder((short) eventJson.getInt("eventReminder"));
            setEventReminder((short) eventJson.getInt("eventReminder"));
            setEvaluated(eventJson.getBoolean("isEvaluated"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}

@SuppressWarnings({"WeakerAccess", "unused"})
class Intervention {
    // static final short CREATION_METHOD_SYSTEM = 0;
    static final short CREATION_METHOD_USER = 1;
    static Intervention[] systemInterventionBank;
    static Intervention[] peerInterventionBank;
    static Intervention[] currentInterventionBank;
    private static HashMap<String, Intervention> descr2IntervMap = new HashMap<>();
    private String description;
    private String creator;
    private short creationMethod;
    private boolean isPublic;
    private short numberOfSelections;
    private short numberOfLikes;
    private short numberOfDislikes;

    Intervention(String description, @Nullable String creator, int creationMethod, boolean isPublic, int numberOfSelections, int numberOfLikes, int numberOfDislikes) {
        this.description = description;
        this.creator = creator;
        this.creationMethod = (short) creationMethod;
        this.isPublic = isPublic;
        this.numberOfSelections = (short) numberOfSelections;
        this.numberOfLikes = (short) numberOfLikes;
        this.numberOfDislikes = (short) numberOfDislikes;
    }

    Intervention(String description, String username, int creationMethod) {
        this.description = description;
        this.creator = username;
        this.creationMethod = (short) creationMethod;
        isPublic = false;
        numberOfSelections = 0;
        numberOfLikes = 0;
        numberOfDislikes = 0;
    }

    static void setSystemInterventionBank(Intervention[] bank) {
        systemInterventionBank = bank;
        currentInterventionBank = bank;
        reloadDescr2IntervMap();
    }

    static void setPeerInterventionBank(Intervention[] bank) {
        peerInterventionBank = bank;
        currentInterventionBank = bank;
        reloadDescr2IntervMap();
    }

    static void addSelfInterventionToBank(Intervention intervention) {
        peerInterventionBank = Arrays.copyOf(peerInterventionBank, peerInterventionBank.length + 1);
        peerInterventionBank[peerInterventionBank.length - 1] = intervention;
        reloadDescr2IntervMap();
    }

    private synchronized static void reloadDescr2IntervMap() {
        descr2IntervMap.clear();
        if (systemInterventionBank != null)
            for (Intervention intervention : systemInterventionBank)
                descr2IntervMap.put(intervention.getDescription(), intervention);
        if (peerInterventionBank != null)
            for (Intervention intervention : peerInterventionBank)
                descr2IntervMap.put(intervention.getDescription(), intervention);
    }

    static Intervention getInterventionByDescription(String description) {
        return descr2IntervMap.get(description);
    }

    static Intervention from_json(JSONObject object) throws JSONException {
        return new Intervention(
                object.getString("description"),
                object.getString("creator"),
                object.getInt("creation_method"),
                object.getBoolean("is_public"),
                object.getInt("number_of_selections"),
                object.getInt("number_of_likes"),
                object.getInt("number_of_dislikes")
        );
    }

    String getDescription() {
        return description;
    }

    String getCreator() {
        return creator;
    }

    short getCreationMethod() {
        return creationMethod;
    }

    boolean isPublic() {
        return isPublic;
    }

    short getNumberOfSelections() {
        return numberOfSelections;
    }

    void increaseNumberOfSelections() {
        this.numberOfSelections++;
    }

    short getNumberOfLikes() {
        return numberOfLikes;
    }

    void increaseNumberOfLikes() {
        this.numberOfLikes++;
    }

    short getNumberOfDislikes() {
        return numberOfDislikes;
    }

    void increaseNumberOfDislikes() {
        this.numberOfDislikes++;
    }

    JSONObject to_json() throws JSONException {
        JSONObject res = new JSONObject();
        res.put("description", getDescription());
        res.put("creator", getCreator());
        res.put("creation_method", getCreationMethod());
        res.put("is_public", isPublic());
        res.put("number_of_selections", getNumberOfSelections());
        res.put("number_of_likes", getNumberOfLikes());
        res.put("number_of_dislikes", getNumberOfDislikes());
        return res;
    }
}

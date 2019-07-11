package kr.ac.inha.nsl.mindforecaster;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.SparseArray;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Tools {
    // region Variables
    static final short
            RES_OK = 0,
            RES_SRV_ERR = -1,
            RES_FAIL = 1;

    private static int cellWidth, cellHeight;
    private static ExecutorService executor = Executors.newCachedThreadPool();
    private static SparseArray<PendingIntent> eventNotifs = new SparseArray<>();
    private static SparseArray<PendingIntent> intervNotifs = new SparseArray<>();
    private static SparseArray<PendingIntent> sundayNotifs = new SparseArray<>();
    private static SparseArray<PendingIntent> dailyNotifs = new SparseArray<>();
    // endregion

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

    static String post(String url, List<NameValuePair> params) throws IOException {
        HttpPost httppost = new HttpPost(url);
        @SuppressWarnings("deprecation")
        HttpClient httpclient = new DefaultHttpClient();
        httppost.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));
        HttpResponse response = httpclient.execute(httppost);

        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
            return Tools.inputStreamToString(response.getEntity().getContent());
        else return null;
    }

    @SuppressWarnings("unused")
    private static String convertFromUTF8(byte[] raw) {
        return new String(raw, StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unused")
    private static String convertToUTF8(String s) {
        return new String(s.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
    }

    static void execute(MyRunnable runnable) {
        disable_touch(runnable.activity);
        executor.execute(runnable);
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

    static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo;
        if (connectivityManager == null)
            return false;
        activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
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

    private static void cacheInterventions(Context context, String[] interventions, String type) {
        if (interventions.length == 0)
            return;

        JSONArray array = new JSONArray();
        for (String intervention : interventions)
            array.put(intervention);

        Tools.writeToFile(context, String.format(Locale.getDefault(), "%s_interventions.json", type), array.toString());
    }

    static void cacheSystemInterventions(Context context, String[] sysInterventions) {
        cacheInterventions(context, sysInterventions, "system");
    }

    static void cacheSurveys(Context context, JSONArray survey1, JSONArray survey2, JSONArray survey3) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("survey1", survey1);
        obj.put("survey2", survey2);
        obj.put("survey3", survey3);
        Tools.writeToFile(context, "survey.json", obj.toString());
    }

    static void cachePeerInterventions(Context context, String[] peerInterventions) {
        cacheInterventions(context, peerInterventions, "peer");
    }

    private static String[] readOfflineInterventions(Context context, String type) {
        JSONArray array;
        try {
            array = new JSONArray(readFromFile(context, String.format(Locale.getDefault(), "%s_interventions.json", type)));
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        try {
            String[] res = new String[array.length()];
            for (int n = 0; n < array.length(); n++)
                res[n] = array.getString(n);
            return res;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
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

    static String[] readOfflineSystemInterventions(Context context) {
        return readOfflineInterventions(context, "system");
    }

    static String[] readOfflinePeerInterventions(Context context) {
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

    static void addIntervNotif(Context context, Calendar when, long event_id, String intervText, String eventText) {
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

    static String notifMinsToString(Context context, int minsValue) {
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
}

abstract class MyRunnable implements Runnable {
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

    Object[] args;
    Activity activity;
}

class Event {
    Event(long id) {
        newEvent = id == 0;
        if (newEvent)
            this.id = System.currentTimeMillis() / 1000;
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

    //region Variables
    private static Event[] currentEventBank;
    private static LongSparseArray<Event> idEventMap = new LongSparseArray<>();
    static final int NO_REPEAT = 0, REPEAT_EVERYDAY = 1, REPEAT_WEEKLY = 2;

    private boolean newEvent;

    private long id;
    private String title;
    private int stressLevel;
    private int realStressLevel;
    private Calendar startTime;
    private Calendar endTime;
    private String intervention;
    private int interventionReminder;
    private String stressType;
    private String stressCause;
    private long repeatId;
    private long repeatTill;
    private int repeatMode;
    private int eventReminder;
    private boolean evaluated;
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

    boolean isNewEvent() {
        return newEvent;
    }

    long getEventId() {
        return id;
    }

    void setStartTime(Calendar startTime) {
        this.startTime = (Calendar) startTime.clone();
        this.startTime.set(Calendar.SECOND, 0);
        this.startTime.set(Calendar.MILLISECOND, 0);
    }

    Calendar getStartTime() {
        return (Calendar) startTime.clone();
    }

    void setEndTime(Calendar endTime) {
        this.endTime = (Calendar) endTime.clone();
        this.endTime.set(Calendar.SECOND, 0);
        this.endTime.set(Calendar.MILLISECOND, 0);
    }

    Calendar getEndTime() {
        return (Calendar) endTime.clone();
    }

    void setStressLevel(int stressLevel) {
        this.stressLevel = stressLevel;
    }

    int getStressLevel() {
        return stressLevel;
    }

    private void setRealStressLevel(int realStressLevel) {
        this.realStressLevel = realStressLevel;
    }

    int getRealStressLevel() {
        return realStressLevel;
    }

    void setTitle(String title) {
        this.title = title;
    }

    String getTitle() {
        return title;
    }

    void setIntervention(String intervention) {
        this.intervention = intervention;
    }

    String getIntervention() {
        return intervention;
    }

    void setStressType(String stressType) {
        this.stressType = stressType;
    }

    String getStressType() {
        return stressType;
    }

    void setStressCause(String stressCause) {
        this.stressCause = stressCause;
    }

    String getStressCause() {
        return stressCause;
    }

    void setRepeatMode(int repeatMode) {
        this.repeatMode = repeatMode;
    }

    int getRepeatMode() {
        return repeatMode;
    }

    private void setRepeatId(long repeatId) {
        this.repeatId = repeatId;
    }

    void setRepeatTill(long repeatTill) {
        this.repeatTill = repeatTill;
    }

    long getRepeatTill() {
        return repeatTill;
    }

    long getRepeatId() {
        return repeatId;
    }

    void setInterventionReminder(int interventionReminder) {
        this.interventionReminder = interventionReminder;
    }

    int getInterventionReminder() {
        return interventionReminder;
    }

    void setEventReminder(int eventReminder) {
        this.eventReminder = eventReminder;
    }

    int getEventReminder() {
        return eventReminder;
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
            setStressLevel(eventJson.getInt("stressLevel"));
            setRealStressLevel(eventJson.getInt("realStressLevel"));
            setStartTime(startTime);
            setEndTime(endTime);
            setIntervention(eventJson.getString("intervention"));
            setInterventionReminder((short) eventJson.getInt("interventionReminder"));
            setStressType(eventJson.getString("stressType"));
            setStressCause(eventJson.getString("stressCause"));
            setRepeatMode(eventJson.getInt("repeatMode"));
            setRepeatId(eventJson.getLong("repeatId"));
            setRepeatTill(eventJson.getLong("repeatTill"));
            setEventReminder((short) eventJson.getInt("eventReminder"));
            setEventReminder((short) eventJson.getInt("eventReminder"));
            setEvaluated(eventJson.getBoolean("isEvaluated"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
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
                    String reminderStr = Tools.notifMinsToString(context, event.getEventReminder());
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

    static void updateIntervReminder(Context context) {
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
                    Tools.addIntervNotif(
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
                    Tools.addIntervNotif(
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

    void setEvaluated(boolean evaluated) {
        this.evaluated = evaluated;
    }

    boolean isEvaluated() {
        return evaluated;
    }
}

@SuppressWarnings("unused")
class Intervention {
    static final short CREATION_METHOD_SYSTEM = 0;
    static final short CREATION_METHOD_USER = 1;
}
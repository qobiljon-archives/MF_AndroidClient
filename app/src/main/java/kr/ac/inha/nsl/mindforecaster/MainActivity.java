package kr.ac.inha.nsl.mindforecaster;

import android.annotation.SuppressLint;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.my_toolbar));
        init();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        updateCalendarView();
        super.onActivityResult(requestCode, resultCode, data);
    }

    // region Variables
    final static int EVENT_ACTIVITY = 0;

    private GridLayout event_grid;
    private ViewGroup[][] cells = new ViewGroup[7][5];
    private TextView monthNameTextView;
    private TextView yearValueTextView;
    private Calendar currentCal;

    // region CellClick Listener
    LinearLayout.OnClickListener cellClick = new LinearLayout.OnClickListener() {
        @Override
        public void onClick(View view) {
            showTodayEvents((long) view.getTag());
        }
    };
    // endregion
    // endregion

    private void showTodayEvents(long dateTimeMillis) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        DialogFragment dialogFragment = new EventsListDialog();
        Bundle args = new Bundle();
        args.putLong("selectedDayMillis", dateTimeMillis);
        dialogFragment.setArguments(args);
        dialogFragment.show(ft, "dialog");
    }

    private void init() {
        currentCal = Calendar.getInstance(Locale.getDefault());
        currentCal.set(currentCal.get(Calendar.YEAR), currentCal.get(Calendar.MONTH), currentCal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        event_grid = findViewById(R.id.event_grid);
        monthNameTextView = findViewById(R.id.header_month_name);
        yearValueTextView = findViewById(R.id.header_year);

        event_grid.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                event_grid.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                Tools.setCellSize(event_grid.getWidth() / event_grid.getColumnCount(), event_grid.getHeight() / event_grid.getRowCount());
                updateCalendarView();
            }
        });

        if (Tools.isNetworkAvailable(this)) {
            Tools.execute(new MyRunnable(this) {
                @Override
                public void run() {
                    try {
                        ArrayList<NameValuePair> params = new ArrayList<>();
                        JSONObject res = new JSONObject(Tools.post(getString(R.string.url_system_intervention_fetch, getString(R.string.server_ip)), params));
                        if (res.getInt("result") == Tools.RES_OK)
                            runOnUiThread(new MyRunnable(activity, res.getJSONArray("interventions")) {
                                @Override
                                public void run() {
                                    try {
                                        JSONArray arr = (JSONArray) args[0];
                                        Intervention[] interventions = new Intervention[arr.length()];
                                        for (int n = 0; n < arr.length(); n++) {
                                            Intervention intervention = Intervention.from_json(new JSONObject(arr.getString(n)));
                                            interventions[n] = intervention;
                                        }
                                        Tools.cacheSystemInterventions(MainActivity.this, interventions);
                                        Intervention.setSystemInterventionBank(interventions);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }
                }
            });

            Tools.execute(new MyRunnable(this) {
                @Override
                public void run() {
                    try {
                        List<NameValuePair> params = new ArrayList<>();
                        params.add(new BasicNameValuePair("username", SignInActivity.loginPrefs.getString(SignInActivity.username, null)));
                        params.add(new BasicNameValuePair("password", SignInActivity.loginPrefs.getString(SignInActivity.password, null)));
                        JSONObject res = new JSONObject(Tools.post(getString(R.string.url_peer_intervention_fetch, getString(R.string.server_ip)), params));
                        if (res.getInt("result") == Tools.RES_OK)
                            runOnUiThread(new MyRunnable(activity, res.getJSONArray("interventions")) {
                                @Override
                                public void run() {
                                    try {
                                        JSONArray arr = (JSONArray) args[0];
                                        Intervention[] interventions = new Intervention[arr.length()];
                                        for (int n = 0; n < arr.length(); n++) {
                                            Intervention intervention = Intervention.from_json(new JSONObject(arr.getString(n)));
                                            interventions[n] = intervention;
                                        }
                                        Tools.cachePeerInterventions(MainActivity.this, interventions);
                                        Intervention.setPeerInterventionBank(interventions);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            try {
                Intervention[] systemInterventions = Tools.readOfflineSystemInterventions(MainActivity.this);
                Intervention.setSystemInterventionBank(systemInterventions);

                Intervention[] peerInterventions = Tools.readOfflinePeerInterventions(MainActivity.this);
                Intervention.setPeerInterventionBank(peerInterventions);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void updateCalendarView() {
        // Update the value of yearValueTextView and month according to the currently selected month
        monthNameTextView.setText(getResources().getStringArray(R.array.months_array)[currentCal.get(Calendar.MONTH)]);
        yearValueTextView.setText(String.valueOf(currentCal.get(Calendar.YEAR)));

        // First clear our the grid
        for (int row = 0; row < event_grid.getRowCount(); row++)
            for (int col = 0; col < event_grid.getColumnCount(); col++)
                Tools.cellClearOut(cells, col, row, this, event_grid, cellClick);

        // Check if displayed month contains today
        Calendar today = Calendar.getInstance(Locale.getDefault());
        TextView todayText;

        if (currentCal.get(Calendar.MONTH) == today.get(Calendar.MONTH) && currentCal.get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
            int col = today.get(Calendar.DAY_OF_WEEK) - 1;
            int row = today.get(Calendar.DAY_OF_MONTH);
            today.set(Calendar.DAY_OF_MONTH, 1);
            row = (row + today.get(Calendar.DAY_OF_WEEK) - 2) / 7;

            todayText = cells[col][row].findViewById(R.id.date_text_view);
            todayText.setTextColor(Color.WHITE);
            todayText.setBackgroundResource(R.drawable.bg_today_view);
        }

        // Calculate which date of week current month starts from

        int firstDayOfMonth = getFirstWeekdayIndex(currentCal.get(Calendar.DAY_OF_MONTH), currentCal.get(Calendar.MONTH), currentCal.get(Calendar.YEAR));
        int numOfDaysCurMonth = currentCal.getActualMaximum(Calendar.DAY_OF_MONTH);

        Calendar clone = (Calendar) currentCal.clone();
        clone.add(Calendar.MONTH, -1);
        int prevCnt = clone.getActualMaximum(Calendar.DAY_OF_MONTH) - firstDayOfMonth + 2;

        // Set dates to display
        int col = 0, row, count = 1;
        for (row = 0; row < event_grid.getRowCount(); row++) {
            if (row == 0) {
                for (col = 0; col < firstDayOfMonth - 1; col++) {
                    @SuppressLint("CutPasteId")
                    TextView date_text = cells[col][row].findViewById(R.id.date_text_view);
                    date_text.setText(String.valueOf(prevCnt));

                    clone.set(Calendar.DAY_OF_MONTH, prevCnt);
                    cells[col][row].setTag(clone.getTimeInMillis());

                    prevCnt++;
                }
                clone.add(Calendar.MONTH, 1);
                for (; col < event_grid.getColumnCount(); col++, count++) {
                    @SuppressLint("CutPasteId")
                    TextView date_text = cells[col][row].findViewById(R.id.date_text_view);
                    date_text.setText(String.valueOf(count));

                    clone.set(Calendar.DAY_OF_MONTH, count);
                    cells[col][row].setTag(clone.getTimeInMillis());
                }
            } else
                for (col = 0; count <= numOfDaysCurMonth && col < event_grid.getColumnCount(); col++, count++) {
                    @SuppressLint("CutPasteId")
                    TextView date_text = cells[col][row].findViewById(R.id.date_text_view);
                    date_text.setText(String.valueOf(count));

                    clone.set(Calendar.DAY_OF_MONTH, count);
                    cells[col][row].setTag(clone.getTimeInMillis());
                }
        }
        clone.add(Calendar.MONTH, 1);

        for (count = 1, row = event_grid.getRowCount() - 1; col < event_grid.getColumnCount(); col++, count++) {
            @SuppressLint("CutPasteId")
            TextView date_text = cells[col][row].findViewById(R.id.date_text_view);
            date_text.setText(String.valueOf(count));

            clone.set(Calendar.DAY_OF_MONTH, count);
            cells[col][row].setTag(clone.getTimeInMillis());
        }

        // Download events from internet and display them
        Calendar periodFrom = Calendar.getInstance(Locale.getDefault());
        Calendar periodTill = Calendar.getInstance(Locale.getDefault());
        periodFrom.setTimeInMillis((long) cells[0][0].getTag());
        periodTill.setTimeInMillis((long) cells[cells.length - 1][cells[0].length - 1].getTag());
        periodTill.add(Calendar.DAY_OF_MONTH, 1);
        if (Tools.isNetworkAvailable(this))
            Tools.execute(new MyRunnable(
                    this,
                    getString(R.string.url_events_fetch, getString(R.string.server_ip)),
                    SignInActivity.loginPrefs.getString(SignInActivity.username, null),
                    SignInActivity.loginPrefs.getString(SignInActivity.password, null),
                    periodFrom.getTimeInMillis() / 1000,
                    periodTill.getTimeInMillis() / 1000,
                    currentCal.get(Calendar.MONTH),
                    currentCal.get(Calendar.YEAR)
            ) {
                @Override
                public void run() {
                    String url = (String) args[0];
                    String username = (String) args[1];
                    String password = (String) args[2];
                    long period_from = (long) args[3];
                    long period_till = (long) args[4];
                    int month = (int) args[5];
                    int year = (int) args[6];

                    List<NameValuePair> params = new ArrayList<>();
                    try {
                        params.add(new BasicNameValuePair("username", username));
                        params.add(new BasicNameValuePair("password", password));
                        params.add(new BasicNameValuePair("period_from", String.valueOf(period_from)));
                        params.add(new BasicNameValuePair("period_till", String.valueOf(period_till)));

                        JSONObject res = new JSONObject(Tools.post(url, params));
                        switch (res.getInt("result")) {
                            case Tools.RES_OK:
                                JSONArray array = res.getJSONArray("array");
                                Event[] events = new Event[array.length()];

                                for (int n = 0; n < array.length(); n++) {
                                    JSONObject event = array.getJSONObject(n);
                                    events[n] = new Event(event.getLong("eventId"));
                                    events[n].fromJson(event);
                                }
                                Event.setCurrentEventBank(events);
                                Event.updateEventReminders(MainActivity.this);
                                Event.updateIntervReminder(MainActivity.this);
                                Tools.cacheMonthlyEvents(MainActivity.this, events, month, year);

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        for (int row = 0; row < event_grid.getRowCount(); row++)
                                            for (int col = 0; col < event_grid.getColumnCount(); col++) {
                                                Calendar day = Calendar.getInstance(Locale.getDefault());
                                                day.setTimeInMillis((long) cells[col][row].getTag());
                                                ArrayList<Event> dayEvents = Event.getOneDayEvents(day);
                                                for (Event event : dayEvents) {
                                                    getLayoutInflater().inflate(R.layout.event_small_element, cells[col][row]);
                                                    TextView tv = (TextView) cells[col][row].getChildAt(cells[col][row].getChildCount() - 1);
                                                    if (!event.isEvaluated())
                                                        tv.setBackgroundColor(Tools.stressLevelToColor(getApplicationContext(), event.getStressLevel()));
                                                    else
                                                        tv.setBackgroundColor(Tools.stressLevelToColor(getApplicationContext(), event.getRealStressLevel()));
                                                    tv.setText(event.getTitle());
                                                }
                                            }

                                        if (getIntent().hasExtra("eventDate")) {
                                            if (getIntent().getBooleanExtra("isEvaluate", false))
                                                showTodayEvents(getIntent().getLongExtra("eventDate", 0));
                                        }

                                    }
                                });
                                break;
                            case Tools.RES_FAIL:
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(MainActivity.this, "Failed to load the events for this month.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                                break;
                            case Tools.RES_SRV_ERR:
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(MainActivity.this, "Failure occurred while processing the request. (SERVER SIDE)", Toast.LENGTH_SHORT).show();
                                    }
                                });
                                break;
                            default:
                                break;
                        }
                    } catch (JSONException | IOException e) {
                        e.printStackTrace();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Failed to proceed due to an error in connection with server.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    enableTouch();
                }
            });
        else {
            Event.setCurrentEventBank(Tools.readOfflineMonthlyEvents(this, currentCal.get(Calendar.MONTH), currentCal.get(Calendar.YEAR)));
            Event.updateEventReminders(MainActivity.this);
            Event.updateIntervReminder(MainActivity.this);
            for (row = 0; row < event_grid.getRowCount(); row++)
                for (col = 0; col < event_grid.getColumnCount(); col++) {
                    Calendar day = Calendar.getInstance(Locale.getDefault());
                    day.setTimeInMillis((long) cells[col][row].getTag());
                    ArrayList<Event> dayEvents = Event.getOneDayEvents(day);
                    for (Event event : dayEvents) {
                        getLayoutInflater().inflate(R.layout.event_small_element, cells[col][row]);
                        TextView tv = (TextView) cells[col][row].getChildAt(cells[col][row].getChildCount() - 1);
                        tv.setBackgroundColor(Tools.stressLevelToColor(getApplicationContext(), event.getStressLevel()));
                        tv.setText(event.getTitle());
                    }
                }

            if (getIntent().hasExtra("eventDate")) {
                if (getIntent().getBooleanExtra("isEvaluate", false))
                    showTodayEvents(getIntent().getLongExtra("eventDate", 0));
            }
        }

    }

    public int getFirstWeekdayIndex(int day, int month, int year) {
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.set(Calendar.DATE, day);
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        return cal.get(Calendar.DAY_OF_WEEK);
    }

    public void navNextWeekClick(View view) {
        currentCal.add(Calendar.MONTH, 1);
        updateCalendarView();
    }

    public void navPrevWeekClick(View view) {
        currentCal.add(Calendar.MONTH, -1);
        updateCalendarView();
    }

    public void todayClick(MenuItem item) {
        NotificationManager nMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nMgr != null) {
            nMgr.cancelAll();
        }
        currentCal = Calendar.getInstance(Locale.getDefault());
        currentCal.set(currentCal.get(Calendar.YEAR), currentCal.get(Calendar.MONTH), currentCal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        updateCalendarView();
    }

    public void logoutClick(MenuItem item) {
        SharedPreferences.Editor editor = SignInActivity.loginPrefs.edit();
        editor.clear();
        editor.apply();
        Intent intent = new Intent(MainActivity.this, SignInActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(R.anim.activity_in_reverse, R.anim.activity_out_reverse);
    }

    public void notifSettingsClick(MenuItem item) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("dialogSettings");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        DialogFragment dialogFragment = new NotifSettingsDialog();
        dialogFragment.show(ft, "dialog");
    }

    public void selectMonth(View view) {
    }

    public void selectYear(View view) {
    }

    public void onNewEventClick(View view) {
        Intent intent = new Intent(this, EventActivity.class);
        intent.putExtra("selectedDayMillis", Calendar.getInstance(Locale.getDefault()).getTimeInMillis());
        startActivityForResult(intent, 0);
        overridePendingTransition(R.anim.activity_in, R.anim.activity_out);
    }

    static final int SURVEY_ACTIVITY = 0;

    public void surveyClick(MenuItem item) {
        Intent intent = new Intent(this, SurveyActivity.class);
        startActivityForResult(intent, SURVEY_ACTIVITY);
        overridePendingTransition(R.anim.activity_in, R.anim.activity_out);
    }
}

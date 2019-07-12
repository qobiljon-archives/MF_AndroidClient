package kr.ac.inha.nsl.mindforecaster;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatTextView;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class InterventionsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interventions);
        init();
    }

    //region Variables
    private boolean selfIntervention = true;
    static Intervention resultIntervention = null;
    static int resultReminderMinutes = 0;

    private EditText intervTitleText;
    private View intervChoice;
    private ArrayAdapter<String> intervListAdapter;
    private ArrayList<String> currentIntervsList;
    private HashMap<String, Intervention> descr2IntervMap;
    private ViewGroup intervReminderRoot;
    private RadioGroup intervReminderRadGroup;
    private RadioButton customReminderRadioButton;
    private Button[] tabButtons;
    private TextView requestMessageTxt;
    private RadioGroup sortRadioGroup;

    private InputMethodManager imm;

    //endregion

    private void init() {
        intervChoice = findViewById(R.id.intervention_choice);
        intervTitleText = findViewById(R.id.intervention_text);
        requestMessageTxt = findViewById(R.id.request_message_txt);
        intervReminderRoot = findViewById(R.id.interv_reminder_root);
        intervReminderRadGroup = findViewById(R.id.interv_reminder_radgroup);
        customReminderRadioButton = findViewById(R.id.option_custom);
        sortRadioGroup = findViewById(R.id.sort_radio_group);
        tabButtons = new Button[]{
                findViewById(R.id.button_self_intervention),
                findViewById(R.id.button_systems_intervention),
                findViewById(R.id.button_peer_interventions)
        };

        ListView interventionsList = findViewById(R.id.interventions_listview);
        currentIntervsList = new ArrayList<>();
        descr2IntervMap = new HashMap<>();
        intervListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, currentIntervsList);
        interventionsList.setAdapter(intervListAdapter);
        interventionsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String intervStr = ((AppCompatTextView) view).getText().toString();
                resultIntervention = descr2IntervMap.get(intervStr);
                assert resultIntervention != null;
                intervTitleText.setText(resultIntervention.getDescription());
                intervChoice.setVisibility(View.GONE);
                intervTitleText.setVisibility(View.VISIBLE);
                intervReminderRoot.setVisibility(View.VISIBLE);
            }
        });

        TextView eventTitle = findViewById(R.id.event_title_text_view);
        eventTitle.setText(getString(R.string.current_event_title, getIntent().getStringExtra("eventTitle")));

        intervTitleText.setVisibility(View.GONE);
        intervChoice.setVisibility(View.GONE);
        tabButtons[0].callOnClick();

        intervReminderRadGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (intervReminderRadGroup.findViewById(checkedId) != findViewById(R.id.txt_custom_interv_notif))
                    resultReminderMinutes = Integer.parseInt(String.valueOf(intervReminderRadGroup.findViewById(checkedId).getTag()));
            }
        });

        String eventTitleStr = getIntent().getStringExtra("eventTitle");
        eventTitle.setText(getString(R.string.current_event_title, eventTitleStr.length() == 0 ? "[unnamed]" : eventTitleStr));
        if (!EventActivity.event.isNewEvent())
            fillOutExistingValues();


        //region For hiding a soft keyboard
        imm = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null)
            imm.hideSoftInputFromWindow(intervTitleText.getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);

        eventTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                assert imm != null;
                imm.hideSoftInputFromWindow(intervTitleText.getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);
            }
        });
        //endregion
    }

    private void sortInterventions(Intervention[] interventions) {
        if (sortRadioGroup.getCheckedRadioButtonId() == R.id.sort_by_recent_choice_radio_button) {
            List<Intervention> eventInterventions = new ArrayList<>();
            List<Long> intervLastPicketTimes = new ArrayList<>();
            for (Event event : Event.currentEventBank)
                if (event.getIntervention() != null) {
                    int n = intervLastPicketTimes.size() - 1;
                    while (n >= 0) {
                        if (event.getInterventionLastPickedTime() <)
                            n--;
                    }
                    eventInterventions.add(n == -1 ? 0 : (n == intervLastPicketTimes.size() - 1 ? n : n + 1), Intervention.getInterventionByDescription(event.getIntervention()));
                    intervLastPicketTimes.add(n == -1 ? 0 : n + 1, event.getInterventionLastPickedTime());
                }
        } else if (sortRadioGroup.getCheckedRadioButtonId() == R.id.sort_by_popularity_radio_button) {

        }
    }

    public void closeInput(View view) {
        if (imm != null)
            imm.hideSoftInputFromWindow(intervTitleText.getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);
    }

    private void fillOutExistingValues() {
        intervTitleText.setText(EventActivity.event.getIntervention());

        switch (EventActivity.event.getInterventionReminder()) {
            case 0:
                intervReminderRadGroup.check(R.id.option_none);
                break;
            case -1440:
                intervReminderRadGroup.check(R.id.option_day_before);
                break;
            case -60:
                intervReminderRadGroup.check(R.id.option_hour_before);
                break;
            case -30:
                intervReminderRadGroup.check(R.id.option_30mins_before);
                break;
            case -10:
                intervReminderRadGroup.check(R.id.option_10mins_before);
                break;
            case 1440:
                intervReminderRadGroup.check(R.id.option_day_after);
                break;
            case 60:
                intervReminderRadGroup.check(R.id.option_hour_after);
                break;
            case 30:
                intervReminderRadGroup.check(R.id.option_30mins_after);
                break;
            case 10:
                intervReminderRadGroup.check(R.id.option_10mins_after);
                break;
            default:
                intervReminderRadGroup.check(R.id.option_custom);
                customReminderRadioButton.setTag(EventActivity.event.getInterventionReminder());
                customReminderRadioButton.setText(Tools.notifMinsToString(this, EventActivity.event.getInterventionReminder()));
                customReminderRadioButton.setVisibility(View.VISIBLE);
                break;
        }
    }

    public void tabClicked(View view) {
        // Clear out visibility and previously set button color
        intervTitleText.setVisibility(View.GONE);
        intervChoice.setVisibility(View.GONE);
        intervReminderRoot.setVisibility(View.GONE);
        for (Button button : tabButtons)
            button.setBackgroundResource(R.drawable.bg_interv_method_unchecked_view);

        // Act upon the click event
        resultIntervention = null;
        switch (view.getId()) {
            case R.id.button_self_intervention:
                selfIntervention = true;
                tabButtons[0].setBackgroundResource(R.drawable.bg_interv_method_checked_view);
                intervTitleText.setText("");
                intervReminderRadGroup.check(R.id.option_none);
                intervTitleText.setVisibility(View.VISIBLE);
                intervTitleText.setFocusableInTouchMode(true);
                intervReminderRoot.setVisibility(View.VISIBLE);
                break;
            case R.id.button_systems_intervention:
                selfIntervention = false;
                requestMessageTxt.setText(getString(R.string.interventions_list_system));
                tabButtons[1].setBackgroundResource(R.drawable.bg_interv_method_checked_view);
                intervChoice.setVisibility(View.VISIBLE);
                if (Tools.isNetworkAvailable(this)) {
                    Tools.execute(new MyRunnable(
                            this,
                            SignInActivity.loginPrefs.getString(SignInActivity.username, null),
                            SignInActivity.loginPrefs.getString(SignInActivity.password, null),
                            getString(R.string.url_system_intervention_fetch, getString(R.string.server_ip))
                    ) {
                        @Override
                        public void run() {
                            String username = (String) args[0];
                            String password = (String) args[1];
                            String url = (String) args[2];

                            List<NameValuePair> params = new ArrayList<>();
                            try {
                                params.add(new BasicNameValuePair("username", username));
                                params.add(new BasicNameValuePair("password", password));
                                JSONObject res = new JSONObject(Tools.post(url, params));
                                switch (res.getInt("result")) {
                                    case Tools.RES_OK:
                                        runOnUiThread(new MyRunnable(activity, res.getJSONArray("interventions")) {
                                            @Override
                                            public void run() {
                                                try {
                                                    JSONArray arr = (JSONArray) args[0];
                                                    Intervention[] interventions = new Intervention[arr.length()];
                                                    for (int n = 0; n < arr.length(); n++)
                                                        interventions[n] = Intervention.from_json(new JSONObject(arr.getString(n)));
                                                    sortInterventions(interventions);
                                                    for (Intervention intervention : interventions) {
                                                        currentIntervsList.add(intervention.getDescription());
                                                        descr2IntervMap.put(intervention.getDescription(), intervention);
                                                    }
                                                    Tools.cacheSystemInterventions(InterventionsActivity.this, interventions);
                                                    Intervention.setSystemInterventionBank(interventions);
                                                    intervListAdapter.notifyDataSetChanged();
                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        });
                                        break;
                                    case Tools.RES_FAIL:
                                        break;
                                    case Tools.RES_SRV_ERR:
                                        break;
                                    default:
                                        break;
                                }
                            } catch (JSONException | IOException e) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        currentIntervsList.clear();
                                        descr2IntervMap.clear();
                                        intervListAdapter.notifyDataSetChanged();
                                    }
                                });
                                e.printStackTrace();
                            }
                            enableTouch();
                        }
                    });
                } else {
                    try {
                        Intervention[] interventions = Tools.readOfflineSystemInterventions(InterventionsActivity.this);
                        sortInterventions(interventions);
                        Intervention.setSystemInterventionBank(interventions);
                        if (interventions.length == 0)
                            return;
                        currentIntervsList.clear();
                        descr2IntervMap.clear();
                        for (Intervention intervention : interventions) {
                            currentIntervsList.add(intervention.getDescription());
                            descr2IntervMap.put(intervention.getDescription(), intervention);
                        }
                        intervListAdapter.notifyDataSetChanged();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case R.id.button_peer_interventions:
                selfIntervention = false;
                requestMessageTxt.setText(getString(R.string.interventions_list_peer));
                tabButtons[2].setBackgroundResource(R.drawable.bg_interv_method_checked_view);
                intervChoice.setVisibility(View.VISIBLE);
                currentIntervsList.clear();
                descr2IntervMap.clear();
                if (Tools.isNetworkAvailable(this)) {
                    Tools.execute(new MyRunnable(
                            this,
                            SignInActivity.loginPrefs.getString(SignInActivity.username, null),
                            SignInActivity.loginPrefs.getString(SignInActivity.password, null),
                            getString(R.string.url_peer_intervention_fetch, getString(R.string.server_ip))
                    ) {
                        @Override
                        public void run() {
                            String username = (String) args[0];
                            String password = (String) args[1];
                            String url = (String) args[2];

                            List<NameValuePair> params = new ArrayList<>();
                            try {
                                params.add(new BasicNameValuePair("username", username));
                                params.add(new BasicNameValuePair("password", password));

                                JSONObject res = new JSONObject(Tools.post(url, params));
                                switch (res.getInt("result")) {
                                    case Tools.RES_OK:
                                        runOnUiThread(new MyRunnable(
                                                activity,
                                                res.getJSONArray("interventions")
                                        ) {
                                            @Override
                                            public void run() {
                                                try {
                                                    JSONArray arr = (JSONArray) args[0];
                                                    Intervention[] interventions = new Intervention[arr.length()];
                                                    for (int n = 0; n < arr.length(); n++)
                                                        interventions[n] = Intervention.from_json(new JSONObject(arr.getString(n)));
                                                    sortInterventions(interventions);
                                                    for (Intervention intervention : interventions) {
                                                        currentIntervsList.add(intervention.getDescription());
                                                        descr2IntervMap.put(intervention.getDescription(), intervention);
                                                    }
                                                    Tools.cachePeerInterventions(InterventionsActivity.this, interventions);
                                                    Intervention.setPeerInterventionBank(interventions);
                                                    intervListAdapter.notifyDataSetChanged();
                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        });
                                        break;
                                    case Tools.RES_FAIL:
                                        break;
                                    case Tools.RES_SRV_ERR:
                                        break;
                                    default:
                                        break;
                                }

                            } catch (JSONException | IOException e) {
                                e.printStackTrace();
                            }
                            enableTouch();
                        }
                    });
                } else {
                    try {
                        Intervention[] interventions = Tools.readOfflinePeerInterventions(InterventionsActivity.this);
                        sortInterventions(interventions);
                        Intervention.setPeerInterventionBank(interventions);
                        if (interventions.length == 0)
                            return;
                        currentIntervsList.clear();
                        descr2IntervMap.clear();
                        for (Intervention intervention : interventions) {
                            currentIntervsList.add(intervention.getDescription());
                            descr2IntervMap.put(intervention.getDescription(), intervention);
                        }
                        intervListAdapter.notifyDataSetChanged();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                break;
            default:
                break;
        }
        closeInput(view);
    }

    public void cancelClick(View view) {
        setResult(Activity.RESULT_CANCELED);
        finish();
        overridePendingTransition(R.anim.activity_in_reverse, R.anim.activity_out_reverse);
    }

    public void saveClick(View view) {
        if (selfIntervention || !resultIntervention.getDescription().equals(intervTitleText.getText().toString())) {
            if (intervTitleText.length() == 0) {
                Toast.makeText(this, "Please type intervention's name first!", Toast.LENGTH_SHORT).show();
                return;
            }
            resultIntervention = new Intervention(
                    intervTitleText.getText().toString(),
                    SignInActivity.loginPrefs.getString(SignInActivity.username, null),
                    Intervention.CREATION_METHOD_USER
            );
            if (Tools.isNetworkAvailable(this))
                Tools.execute(new MyRunnable(
                        this,
                        getString(R.string.url_intervention_create, getString(R.string.server_ip)),
                        SignInActivity.loginPrefs.getString(SignInActivity.username, null),
                        SignInActivity.loginPrefs.getString(SignInActivity.password, null)
                ) {
                    @Override
                    public void run() {
                        String url = (String) args[0];
                        String username = (String) args[1];
                        String password = (String) args[2];

                        List<NameValuePair> params = new ArrayList<>();
                        try {
                            params.add(new BasicNameValuePair("username", username));
                            params.add(new BasicNameValuePair("password", password));
                            params.add(new BasicNameValuePair("interventionName", resultIntervention.getDescription()));

                            JSONObject res = new JSONObject(Tools.post(url, params));
                            switch (res.getInt("result")) {
                                case Tools.RES_OK:
                                    runOnUiThread(new MyRunnable(activity) {
                                        @Override
                                        public void run() {
                                            Intervention.addSelfInterventionToBank(resultIntervention);
                                            Toast.makeText(InterventionsActivity.this, "Intervention successfully created!", Toast.LENGTH_SHORT).show();
                                            setResult(Activity.RESULT_OK);
                                            finish();
                                            overridePendingTransition(R.anim.activity_in_reverse, R.anim.activity_out_reverse);
                                        }
                                    });
                                    break;
                                case Tools.RES_FAIL:
                                    runOnUiThread(new MyRunnable(activity) {
                                        @Override
                                        public void run() {
                                            Toast.makeText(InterventionsActivity.this, "Intervention already exists. Thus, it was picked for you.", Toast.LENGTH_SHORT).show();
                                            setResult(Activity.RESULT_OK);
                                            finish();
                                            overridePendingTransition(R.anim.activity_in_reverse, R.anim.activity_out_reverse);
                                        }
                                    });
                                    break;
                                case Tools.RES_SRV_ERR:
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(InterventionsActivity.this, "Failure in intervention creation. (SERVER SIDE)", Toast.LENGTH_SHORT).show();
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
                                    Toast.makeText(InterventionsActivity.this, "Failed to create the intervention.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                        enableTouch();
                    }
                });
            else
                Toast.makeText(this, "No network! Please connect to network first!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Intervention was picked successfully!", Toast.LENGTH_SHORT).show();
            setResult(Activity.RESULT_OK);
            finish();
            overridePendingTransition(R.anim.activity_in_reverse, R.anim.activity_out_reverse);
        }
    }

    public void clickCustomIntervNotification(View view) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("dialogCustomNotif");
        if (prev != null)
            ft.remove(prev);
        ft.addToBackStack(null);
        DialogFragment dialogFragment = new CustomNotificationDialog();
        Bundle args = new Bundle();
        args.putBoolean("isEventNotification", false);
        dialogFragment.setArguments(args);
        dialogFragment.show(ft, "dialogCustomNotif");
    }

    public void setCustomNotifParams(int minutes) {
        resultReminderMinutes = minutes;
        customReminderRadioButton.setTag(String.valueOf(minutes));
        intervReminderRadGroup.check(R.id.option_custom);
        customReminderRadioButton.setText(Tools.notifMinsToString(this, minutes));
        customReminderRadioButton.setVisibility(View.VISIBLE);
    }
}

package kr.ac.inha.nsl.mindforecaster;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class InterventionsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interventions);
        init();
    }

    //region Variables
    private boolean selfIntervention = true;
    static String resultIntervText = null;
    static int resultNotifMinutes = 0;

    private String createMethod;
    private EditText intervTitleText;
    private View intervChoice;
    private ViewGroup intervList, intervListMore, intervReminderRoot;
    private RadioGroup intervReminderRadGroup;
    private RadioButton customReminderRadioButton;
    private Button[] tabButtons;
    private TextView requestMessageTxt;

    private InputMethodManager imm;

    //endregion

    @SuppressLint("ClickableViewAccessibility")
    private void init() {
        intervChoice = findViewById(R.id.intervention_choice);
        intervTitleText = findViewById(R.id.intervention_text);
        requestMessageTxt = findViewById(R.id.request_message_txt);
        intervList = findViewById(R.id.interventions_list);
        intervListMore = findViewById(R.id.interventions_list_more);
        intervReminderRoot = findViewById(R.id.interv_reminder_root);
        intervReminderRadGroup = findViewById(R.id.interv_reminder_radgroup);
        customReminderRadioButton = findViewById(R.id.option_custom);
        tabButtons = new Button[]{
                findViewById(R.id.button_self_intervention),
                findViewById(R.id.button_systems_intervention),
                findViewById(R.id.button_peer_interventions)
        };

        TextView eventTitle = findViewById(R.id.event_title_text_view);
        eventTitle.setText(getString(R.string.current_event_title, getIntent().getStringExtra("eventTitle")));

        intervTitleText.setVisibility(View.GONE);
        intervChoice.setVisibility(View.GONE);
        tabButtons[0].callOnClick();

        intervReminderRadGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (intervReminderRadGroup.findViewById(checkedId) != findViewById(R.id.txt_custom_interv_notif))
                    resultNotifMinutes = Integer.parseInt(String.valueOf(intervReminderRadGroup.findViewById(checkedId).getTag()));
            }
        });

        String eventTitleStr = getIntent().getStringExtra("eventTitle");
        eventTitle.setText(getString(R.string.current_event_title, eventTitleStr.length() == 0 ? "[unnamed]" : eventTitleStr));
        if (!EventActivity.event.isNewEvent())
            fillOutExistingValues();


        //region For hiding a soft keyboard
        intervTitleText.setFocusable(false);
        intervTitleText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                intervTitleText.setFocusableInTouchMode(true);
                return false;
            }
        });
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
        resultIntervText = null;
        switch (view.getId()) {
            case R.id.button_self_intervention:
                createMethod = (String) ((Button)view).getText();
                selfIntervention = true;
                tabButtons[0].setBackgroundResource(R.drawable.bg_interv_method_checked_view);
                intervTitleText.setText("");
                intervReminderRadGroup.check(R.id.option_none);
                intervTitleText.setVisibility(View.VISIBLE);
                intervTitleText.setFocusableInTouchMode(true);
                intervReminderRoot.setVisibility(View.VISIBLE);
                break;
            case R.id.button_systems_intervention:
                createMethod = (String) ((Button)view).getText();
                selfIntervention = false;
                requestMessageTxt.setText(getString(R.string.interventions_list_system));
                tabButtons[1].setBackgroundResource(R.drawable.bg_interv_method_checked_view);
                intervChoice.setVisibility(View.VISIBLE);
                intervList.removeAllViews();
                intervListMore.removeAllViews();
                if (Tools.isNetworkAvailable(this))
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

                            JSONObject body = new JSONObject();
                            try {
                                body.put("username", username);
                                body.put("password", password);

                                JSONObject res = new JSONObject(Tools.post(url, body));
                                switch (res.getInt("result")) {
                                    case Tools.RES_OK:
                                        runOnUiThread(new MyRunnable(
                                                activity,
                                                res.getJSONArray("names")
                                        ) {
                                            @Override
                                            public void run() {
                                                JSONArray arr = (JSONArray) args[0];
                                                while (intervList.getChildCount() > 1)
                                                    intervList.removeViewAt(1);
                                                LayoutInflater inflater = getLayoutInflater();
                                                try {
                                                    String[] interv = new String[arr.length()];
                                                    for (int n = 0; n < 20; n++) {
                                                        inflater.inflate(R.layout.intervention_element, intervList);
                                                        final TextView interv_text = intervList.getChildAt(n).findViewById(R.id.intervention_text);
                                                        interv_text.setText(interv[n] = arr.getString(n));
                                                    }
                                                    inflater.inflate(R.layout.more_button_element, intervList);
                                                    for (int n = 20, i = 0; n < arr.length(); n++, i++) {
                                                        inflater.inflate(R.layout.intervention_element, intervListMore);
                                                        final TextView interv_text = intervListMore.getChildAt(i).findViewById(R.id.intervention_text);
                                                        interv_text.setText(interv[n] = arr.getString(n));
                                                    }
                                                    Tools.cacheSystemInterventions(InterventionsActivity.this, interv);
                                                    intervListMore.setVisibility(View.GONE);
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
                else {
                    String[] interventions = Tools.readOfflineSystemInterventions(this);
                    if (interventions == null)
                        return;

                    LayoutInflater inflater = getLayoutInflater();
                    for (int n = 0; n < 20; n++) {
                        inflater.inflate(R.layout.intervention_element, intervList);
                        final TextView interv_text = intervList.getChildAt(n).findViewById(R.id.intervention_text);
                        interv_text.setText(interventions[n]);

                    }
                    inflater.inflate(R.layout.more_button_element, intervList);
                    for (int n = 20, i = 0; n < interventions.length; n++, i++) {
                        inflater.inflate(R.layout.intervention_element, intervListMore);
                        final TextView interv_text = intervListMore.getChildAt(i).findViewById(R.id.intervention_text);
                        interv_text.setText(interventions[n]);

                    }
                    intervListMore.setVisibility(View.GONE);
                }
                break;
            case R.id.button_peer_interventions:
                createMethod = (String) ((Button)view).getText();
                selfIntervention = false;
                requestMessageTxt.setText(getString(R.string.interventions_list_peer));
                intervListMore.setVisibility(View.GONE);
                tabButtons[2].setBackgroundResource(R.drawable.bg_interv_method_checked_view);
                intervChoice.setVisibility(View.VISIBLE);
                intervList.removeAllViews();
                if (Tools.isNetworkAvailable(this))
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

                            JSONObject body = new JSONObject();
                            try {
                                body.put("username", username);
                                body.put("password", password);

                                JSONObject res = new JSONObject(Tools.post(url, body));
                                switch (res.getInt("result")) {
                                    case Tools.RES_OK:
                                        runOnUiThread(new MyRunnable(
                                                activity,
                                                res.getJSONArray("names")
                                        ) {
                                            @Override
                                            public void run() {
                                                JSONArray arr = (JSONArray) args[0];
                                                while (intervList.getChildCount() > 1)
                                                    intervList.removeViewAt(1);

                                                LayoutInflater inflater = getLayoutInflater();
                                                try {
                                                    String[] interv = new String[arr.length()];
                                                    for (int n = 0; n < arr.length(); n++) {
                                                        inflater.inflate(R.layout.intervention_element, intervList);
                                                        TextView interv_text = intervList.getChildAt(n).findViewById(R.id.intervention_text);
                                                        interv_text.setText(interv[n] = arr.getString(n));
                                                    }
                                                    Tools.cachePeerInterventions(InterventionsActivity.this, interv);
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
                else {
                    String[] interventions = Tools.readOfflinePeerInterventions(this);
                    if (interventions == null)
                        return;

                    LayoutInflater inflater = getLayoutInflater();
                    for (int n = 0; n < interventions.length; n++) {
                        inflater.inflate(R.layout.intervention_element, intervList);
                        TextView interv_text = intervList.getChildAt(n).findViewById(R.id.intervention_text);
                        interv_text.setText(interventions[n]);
                    }
                }
                break;
            default:
                break;
        }
        closeInput(view);
    }

    public void onIntervClick(View view) {
        resultIntervText = ((TextView) view.findViewById(R.id.intervention_text)).getText().toString();
        intervTitleText.setText(resultIntervText);
        intervChoice.setVisibility(View.GONE);
        intervTitleText.setVisibility(View.VISIBLE);
        intervReminderRoot.setVisibility(View.VISIBLE);
    }

    public void cancelClick(View view) {
        setResult(Activity.RESULT_CANCELED);
        finish();
        overridePendingTransition(R.anim.activity_in_reverse, R.anim.activity_out_reverse);
    }

    public void saveClick(View view) {
        if (selfIntervention || !resultIntervText.equals(intervTitleText.getText().toString())) {
            if (intervTitleText.length() == 0) {
                Toast.makeText(this, "Please type intervention's name first!", Toast.LENGTH_SHORT).show();
                return;
            }
            resultIntervText = intervTitleText.getText().toString();
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

                        JSONObject body = new JSONObject();
                        try {
                            body.put("username", username);
                            body.put("password", password);
                            body.put("interventionName", resultIntervText);
                            body.put("intervCreateMethod", createMethod);

                            JSONObject res = new JSONObject(Tools.post(url, body));
                            switch (res.getInt("result")) {
                                case Tools.RES_OK:
                                    runOnUiThread(new MyRunnable(
                                            activity
                                    ) {
                                        @Override
                                        public void run() {
                                            Toast.makeText(InterventionsActivity.this, "Intervention successfully created!", Toast.LENGTH_SHORT).show();
                                            setResult(Activity.RESULT_OK);
                                            finish();
                                            overridePendingTransition(R.anim.activity_in_reverse, R.anim.activity_out_reverse);
                                        }
                                    });
                                    break;
                                case Tools.RES_FAIL:
                                    runOnUiThread(new MyRunnable(
                                            activity
                                    ) {
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
            if (resultIntervText == null) {
                Toast.makeText(this, "Please pick an intervention first!", Toast.LENGTH_SHORT).show();
                return;
            } else {
                Toast.makeText(this, "Intervention was picked successfully!", Toast.LENGTH_SHORT).show();
            }

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
        resultNotifMinutes = minutes;
        customReminderRadioButton.setTag(String.valueOf(minutes));
        intervReminderRadGroup.check(R.id.option_custom);
        customReminderRadioButton.setText(Tools.notifMinsToString(this, minutes));
        customReminderRadioButton.setVisibility(View.VISIBLE);
    }

    public void clickmoreInterventions(View view) {
        view.setVisibility(View.GONE);
        intervListMore.setVisibility(View.VISIBLE);
    }

}

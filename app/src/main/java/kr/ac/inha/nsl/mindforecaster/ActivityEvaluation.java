package kr.ac.inha.nsl.mindforecaster;

import android.app.Activity;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ActivityEvaluation extends AppCompatActivity {

    //region Variables
    private EditText journalText, realStressReason;
    private CheckBox eventCompletionCheck, intervCompletionCheck, intervSharingCheck;
    private SeekBar realStressLevel;
    private SeekBar expectedStressLevel;
    private SeekBar intervEffectiveness;
    //endregion

    // region Override
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_evaluation);
        init();
    }
    // endregion

    private void init() {
        eventCompletionCheck = findViewById(R.id.event_cempletion_check);
        intervCompletionCheck = findViewById(R.id.intervention_completion);
        realStressLevel = findViewById(R.id.real_stress_level_seekbar);
        expectedStressLevel = findViewById(R.id.expected_stress_level_seekbar);
        intervSharingCheck = findViewById(R.id.intervention_sharing_check);
        intervEffectiveness = findViewById(R.id.intervention_effectiveness);
        journalText = findViewById(R.id.journal_text);
        realStressReason = findViewById(R.id.stress_incr_reason_edit);

        ViewGroup interventionLayout = findViewById(R.id.intervention_layout);
        final ViewGroup stressIncrDetails = findViewById(R.id.stress_incr_details_view);
        final ViewGroup stressDecrDetails = findViewById(R.id.stress_decr_details_view);
        TextView eventTitle = findViewById(R.id.text_event_title);
        eventTitle.setText(getString(R.string.current_event_title, ActivityEvent.event.getTitle()));
        TextView intervTitle = findViewById(R.id.intervention_title_text);
        intervTitle.setText(getString(R.string.current_interv_title, ActivityEvent.event.getInterventionDescription()));


        if (ActivityEvent.event.getInterventionDescription() == null)
            interventionLayout.setVisibility(View.GONE);

        int stressLevel = ActivityEvent.event.getStressLevel();
        expectedStressLevel.setEnabled(false);
        if (stressLevel == -1) {
            LinearLayout expected_stress_level_labels = findViewById(R.id.expected_stress_level_labels);
            expected_stress_level_labels.setVisibility(View.GONE);
            expectedStressLevel.setVisibility(View.GONE);

            TextView expected_stress_level_not_availble_label = findViewById(R.id.expected_stress_level_not_availble_label);
            expected_stress_level_not_availble_label.setVisibility(View.VISIBLE);
        } else {
            expectedStressLevel.setProgress(stressLevel);
            expectedStressLevel.getProgressDrawable().setColorFilter(Tools.stressLevelToColor(getApplicationContext(), ActivityEvent.event.getStressLevel()), PorterDuff.Mode.SRC_IN);
            expectedStressLevel.getThumb().setColorFilter(Tools.stressLevelToColor(getApplicationContext(), ActivityEvent.event.getStressLevel()), PorterDuff.Mode.SRC_IN);
        }

        realStressLevel.setProgress(ActivityEvent.event.getStressLevel());
        realStressLevel.getProgressDrawable().setColorFilter(Tools.stressLevelToColor(getApplicationContext(), ActivityEvent.event.getStressLevel()), PorterDuff.Mode.SRC_IN);
        realStressLevel.getThumb().setColorFilter(Tools.stressLevelToColor(getApplicationContext(), ActivityEvent.event.getStressLevel()), PorterDuff.Mode.SRC_IN);
        realStressLevel.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                realStressLevel.getProgressDrawable().setColorFilter(Tools.stressLevelToColor(getApplicationContext(), progress), PorterDuff.Mode.SRC_IN);
                realStressLevel.getThumb().setColorFilter(Tools.stressLevelToColor(getApplicationContext(), progress), PorterDuff.Mode.SRC_IN);

                // compare and get expectation and reality discrepancy details if needed
                if (expectedStressLevel.getProgress() < realStressLevel.getProgress()) {
                    stressIncrDetails.setVisibility(View.VISIBLE);
                    stressDecrDetails.setVisibility(View.GONE);
                } else if (expectedStressLevel.getProgress() > realStressLevel.getProgress()) {
                    stressIncrDetails.setVisibility(View.GONE);
                    stressDecrDetails.setVisibility(View.VISIBLE);
                } else if (expectedStressLevel.getProgress() == realStressLevel.getProgress()) {
                    stressIncrDetails.setVisibility(View.GONE);
                    stressDecrDetails.setVisibility(View.GONE);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    public void cancelClick(View view) {
        setResult(Activity.RESULT_CANCELED);
        finish();
        overridePendingTransition(R.anim.activity_in_reverse, R.anim.activity_out_reverse);
    }

    public void saveClick(View view) {
        if (Tools.isNetworkAvailable())
            Tools.execute(new MyRunnable(
                    this,
                    getString(R.string.url_evaluation_submit, getString(R.string.server_ip)),
                    ActivitySignIn.loginPrefs.getString(ActivitySignIn.KEY_USERNAME, null),
                    ActivitySignIn.loginPrefs.getString(ActivitySignIn.KEY_PASSWORD, null)
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
                        params.add(new BasicNameValuePair("eventId", String.valueOf(ActivityEvent.event.getEventId())));
                        params.add(new BasicNameValuePair("interventionName", ActivityEvent.event.getInterventionDescription()));
                        params.add(new BasicNameValuePair("realStressLevel", String.valueOf(realStressLevel.getProgress())));
                        params.add(new BasicNameValuePair("realStressCause", realStressReason.getText().toString()));
                        params.add(new BasicNameValuePair("journal", journalText.getText().toString()));
                        params.add(new BasicNameValuePair("eventDone", String.valueOf(eventCompletionCheck.isChecked())));
                        params.add(new BasicNameValuePair("interventionDone", String.valueOf(intervCompletionCheck.isChecked())));
                        params.add(new BasicNameValuePair("sharedIntervention", String.valueOf(intervSharingCheck.isChecked())));
                        params.add(new BasicNameValuePair("intervEffectiveness", String.valueOf(intervEffectiveness.getProgress())));

                        JSONObject res = new JSONObject(Tools.post(url, params));
                        switch (res.getInt("result")) {
                            case Tools.RES_OK:
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(ActivityEvaluation.this, "Evaluation successfully submitted, thank you!", Toast.LENGTH_SHORT).show();
                                        setResult(Activity.RESULT_OK);
                                        finish();
                                        overridePendingTransition(R.anim.activity_in_reverse, R.anim.activity_out_reverse);
                                    }
                                });
                                break;
                            case Tools.RES_FAIL:
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(ActivityEvaluation.this, "Failed to submit the evaluation.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                                break;
                            case Tools.RES_SRV_ERR:
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(ActivityEvaluation.this, "Failure occurred while processing the request. (SERVER SIDE)", Toast.LENGTH_SHORT).show();
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
                                Toast.makeText(ActivityEvaluation.this, "Failed to proceed due to an error in connection with server.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    enableTouch();
                }
            });
        else
            Toast.makeText(this, "Please connect to a network first!", Toast.LENGTH_SHORT).show();
    }
}

package kr.ac.inha.nsl.mindforecaster;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ActivitySurvey extends AppCompatActivity {

    //region Variables
    private ViewGroup surveyPart1Root, surveyPart1Content;
    private ViewGroup surveyPart2Root, surveyPart2Content;
    private ViewGroup surveyPart3Root, surveyPart3Content;
    //endregion

    // region Override
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey);
        if (!Tools.isNetworkAvailable()) {
            Button cancelButton = findViewById(R.id.cancel_button);
            Toast.makeText(this, "Your phone has internet connectivity issue, please connect to internet first!", Toast.LENGTH_SHORT).show();
            cancelButton.performClick();
        } else
            init();
    }
    // endregion

    private void init() {
        surveyPart1Root = findViewById(R.id.survey1_main_holder);
        surveyPart2Root = findViewById(R.id.survey2_main_holder);
        surveyPart3Root = findViewById(R.id.survey3_main_holder);

        surveyPart1Content = findViewById(R.id.survey1_child_holder);
        surveyPart2Content = findViewById(R.id.survey2_child_holder);
        surveyPart3Content = findViewById(R.id.survey3_child_holder);


        if (Tools.isNetworkAvailable())
            Tools.execute(new MyRunnable(
                    this,
                    ActivitySignIn.loginPrefs.getString(ActivitySignIn.KEY_USERNAME, null),
                    ActivitySignIn.loginPrefs.getString(ActivitySignIn.KEY_PASSWORD, null),
                    getString(R.string.url_survey_questions_fetch, getString(R.string.server_ip))
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
                        JSONObject surveysJson = res.getJSONObject("surveys");
                        switch (res.getInt("result")) {
                            case Tools.RES_OK:
                                runOnUiThread(new MyRunnable(
                                        activity,
                                        surveysJson.getJSONArray("Part 1"),
                                        surveysJson.getJSONArray("Part 2"),
                                        surveysJson.getJSONArray("Part 3")
                                ) {
                                    @Override
                                    public void run() {
                                        JSONArray surveyPart1 = (JSONArray) args[0];
                                        JSONArray surveyPart2 = (JSONArray) args[1];
                                        JSONArray surveyPart3 = (JSONArray) args[2];

                                        LayoutInflater inflater = getLayoutInflater();
                                        try {
                                            for (int n = 0; n < surveyPart1.length(); n++) {
                                                inflater.inflate(R.layout.survey1_element, surveyPart1Content);
                                                TextView titleText = surveyPart1Content.getChildAt(n).findViewById(R.id.txt_survey_element);
                                                String question = String.format(Locale.getDefault(), "%d. %s", n + 1, surveyPart1.getString(n));
                                                titleText.setText(question);
                                            }
                                            for (int n = 0; n < surveyPart2.length(); n++) {
                                                inflater.inflate(R.layout.survey2_element, surveyPart2Content);
                                                TextView titleText = surveyPart2Content.getChildAt(n).findViewById(R.id.txt_survey_element);
                                                String question = String.format(Locale.getDefault(), "%d. %s", n + 1, surveyPart2.getString(n));
                                                titleText.setText(question);
                                            }
                                            for (int n = 0; n < surveyPart3.length(); n++) {
                                                inflater.inflate(R.layout.survey3_element, surveyPart3Content);
                                                TextView titleText = surveyPart3Content.getChildAt(n).findViewById(R.id.txt_survey_element);
                                                String question = String.format(Locale.getDefault(), "%d. %s", n + 1, surveyPart3.getString(n));
                                                titleText.setText(question);
                                            }

                                            Tools.cacheSurveys(ActivitySurvey.this, surveyPart1, surveyPart2, surveyPart3);
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
            try {
                JSONArray[] offlineSurvey = Tools.loadOfflineSurveys(this);
                if (offlineSurvey == null) {
                    Toast.makeText(this, "Please connect to a network first!", Toast.LENGTH_SHORT).show();
                    return;
                }

                LayoutInflater inflater = getLayoutInflater();
                for (int n = 0; n < offlineSurvey[0].length(); n++) {
                    inflater.inflate(R.layout.survey1_element, surveyPart1Content);
                    TextView interv_text = surveyPart1Content.getChildAt(n).findViewById(R.id.txt_survey_element);
                    JSONObject object = offlineSurvey[0].getJSONObject(n);
                    String survTxt = (n + 1) + ". " + object.getString("key");
                    interv_text.setText(survTxt);
                }
                for (int n = 0; n < offlineSurvey[1].length(); n++) {
                    inflater.inflate(R.layout.survey2_element, surveyPart2Content);
                    TextView interv_text = surveyPart2Content.getChildAt(n).findViewById(R.id.txt_survey_element);
                    JSONObject object = offlineSurvey[1].getJSONObject(n);
                    String survTxt = (n + 1) + ". " + object.getString("key");
                    interv_text.setText(survTxt);
                }
                for (int n = 0; n < offlineSurvey[2].length(); n++) {
                    inflater.inflate(R.layout.survey3_element, surveyPart3Content);
                    TextView intervention_description = surveyPart3Content.getChildAt(n).findViewById(R.id.txt_survey_element);
                    JSONObject object = offlineSurvey[2].getJSONObject(n);
                    String survTxt = (n + 1) + ". " + object.getString("key");
                    intervention_description.setText(survTxt);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void expandSurveyPart1(View view) {
        if (surveyPart1Root.getVisibility() == View.VISIBLE) {
            surveyPart1Root.setVisibility(View.GONE);
            ((TextView) view).setCompoundDrawablesWithIntrinsicBounds(null, null, getDrawable(R.drawable.img_expand), null);
        } else {
            surveyPart1Root.setVisibility(View.VISIBLE);
            ((TextView) view).setCompoundDrawablesWithIntrinsicBounds(null, null, getDrawable(R.drawable.img_collapse), null);
        }
    }

    public void expandSurveyPart2(View view) {
        if (surveyPart2Root.getVisibility() == View.VISIBLE) {
            surveyPart2Root.setVisibility(View.GONE);
            ((TextView) view).setCompoundDrawablesWithIntrinsicBounds(null, null, getDrawable(R.drawable.img_expand), null);
        } else {
            surveyPart2Root.setVisibility(View.VISIBLE);
            ((TextView) view).setCompoundDrawablesWithIntrinsicBounds(null, null, getDrawable(R.drawable.img_collapse), null);
        }
    }

    public void expandSurveyPart3(View view) {
        if (surveyPart3Root.getVisibility() == View.VISIBLE) {
            surveyPart3Root.setVisibility(View.GONE);
            ((TextView) view).setCompoundDrawablesWithIntrinsicBounds(null, null, getDrawable(R.drawable.img_expand), null);
        } else {
            surveyPart3Root.setVisibility(View.VISIBLE);
            ((TextView) view).setCompoundDrawablesWithIntrinsicBounds(null, null, getDrawable(R.drawable.img_collapse), null);
        }
    }

    public void saveClick(View view) {
        if (Tools.isNetworkAvailable())
            Tools.execute(new MyRunnable(
                    this,
                    getString(R.string.url_survey_submit, getString(R.string.server_ip)),
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
                        StringBuilder values = new StringBuilder();
                        for (int n = 0; n < surveyPart1Content.getChildCount(); n++)
                            values.append(String.format(Locale.getDefault(), "%d,", ((SeekBar) surveyPart1Content.getChildAt(n).findViewById(R.id.scale)).getProgress()));
                        for (int n = 0; n < surveyPart2Content.getChildCount(); n++)
                            values.append(String.format(Locale.getDefault(), "%d,", ((SeekBar) surveyPart2Content.getChildAt(n).findViewById(R.id.scale)).getProgress()));
                        for (int n = 0; n < surveyPart3Content.getChildCount(); n++)
                            values.append(String.format(Locale.getDefault(), "%d,", ((SeekBar) surveyPart3Content.getChildAt(n).findViewById(R.id.scale)).getProgress()));
                        if (values.length() > 0)
                            values.deleteCharAt(values.length() - 1);

                        params.add(new BasicNameValuePair("username", username));
                        params.add(new BasicNameValuePair("password", password));
                        params.add(new BasicNameValuePair("values", values.toString()));

                        JSONObject res = new JSONObject(Tools.post(url, params));
                        switch (res.getInt("result")) {
                            case Tools.RES_OK:
                                runOnUiThread(new MyRunnable(
                                        activity
                                ) {
                                    @Override
                                    public void run() {
                                        Toast.makeText(ActivitySurvey.this, "Survey is submitted successfully!", Toast.LENGTH_SHORT).show();
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
                                        Toast.makeText(ActivitySurvey.this, "Failure in survey submission. Result = 1", Toast.LENGTH_SHORT).show();
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
                                        Toast.makeText(ActivitySurvey.this, "Failure in survey submission creation. (SERVER SIDE)", Toast.LENGTH_SHORT).show();
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
                                Toast.makeText(ActivitySurvey.this, "Failed to submit the survey.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    enableTouch();
                }
            });
        else
            Toast.makeText(this, "Please connect to a network first!", Toast.LENGTH_SHORT).show();
    }

    public void cancelClick(View view) {
        setResult(Activity.RESULT_CANCELED);
        finish();
        overridePendingTransition(R.anim.activity_in_reverse, R.anim.activity_out_reverse);
    }
}

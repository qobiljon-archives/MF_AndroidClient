package kr.ac.inha.nsl.mindforecaster;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;

public class SurveyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey);
        init();
    }

    //region Variables
    private ViewGroup surveyMainHolder1, surveyChildHolder1;
    private ViewGroup surveyMainHolder2, surveyChildHolder2;
    private ViewGroup surveyMainHolder3, surveyChildHolder3;
    //endregion

    private void init() {
        surveyMainHolder1 = findViewById(R.id.survey1_main_holder);
        surveyMainHolder2 = findViewById(R.id.survey2_main_holder);
        surveyMainHolder3 = findViewById(R.id.survey3_main_holder);

        surveyChildHolder1 = findViewById(R.id.survey1_child_holder);
        surveyChildHolder2 = findViewById(R.id.survey2_child_holder);
        surveyChildHolder3 = findViewById(R.id.survey3_child_holder);

        loadTheSurvey();
    }

    private void loadTheSurvey() {
        surveyChildHolder1.removeAllViews();
        surveyChildHolder2.removeAllViews();
        surveyChildHolder3.removeAllViews();

        if (Tools.isNetworkAvailable(this))
            Tools.execute(new MyRunnable(
                    this,
                    SignInActivity.loginPrefs.getString(SignInActivity.username, null),
                    SignInActivity.loginPrefs.getString(SignInActivity.password, null),
                    getString(R.string.url_survey_questions_fetch, getString(R.string.server_ip))
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
                        JSONObject survJson = res.getJSONObject("surveys");
                        switch (res.getInt("result")) {
                            case Tools.RES_OK:
                                runOnUiThread(new MyRunnable(
                                        activity,
                                        survJson.getJSONArray("Part 1"),
                                        survJson.getJSONArray("Part 2"),
                                        survJson.getJSONArray("Part 3")
                                ) {
                                    @Override
                                    public void run() {
                                        JSONArray arrSurvey1 = (JSONArray) args[0];
                                        JSONArray arrSurvey2 = (JSONArray) args[1];
                                        JSONArray arrSurvey3 = (JSONArray) args[2];

                                        LayoutInflater inflater = getLayoutInflater();
                                        try {
                                            for (int n = 0; n < arrSurvey1.length(); n++) {
                                                inflater.inflate(R.layout.survey1_element, surveyChildHolder1);
                                                TextView interv_text = surveyChildHolder1.getChildAt(n).findViewById(R.id.txt_survey_element);
                                                JSONObject object = arrSurvey1.getJSONObject(n);
                                                String survTxt = String.format(Locale.getDefault(), "%d. %s", n + 1, object.getString("key"));
                                                interv_text.setText(survTxt);
                                            }
                                            for (int n = 0; n < arrSurvey2.length(); n++) {
                                                inflater.inflate(R.layout.survey2_element, surveyChildHolder2);
                                                TextView interv_text = surveyChildHolder2.getChildAt(n).findViewById(R.id.txt_survey_element);
                                                JSONObject object = arrSurvey2.getJSONObject(n);
                                                String survTxt = String.valueOf(n + 1) + ". " + object.getString("key");
                                                interv_text.setText(survTxt);
                                            }
                                            for (int n = 0; n < arrSurvey3.length(); n++) {
                                                inflater.inflate(R.layout.survey3_element, surveyChildHolder3);
                                                TextView interv_text = surveyChildHolder3.getChildAt(n).findViewById(R.id.txt_survey_element);
                                                JSONObject object = arrSurvey3.getJSONObject(n);
                                                String survTxt = String.valueOf(n + 1) + ". " + object.getString("key");
                                                interv_text.setText(survTxt);
                                            }

                                            Tools.cacheSurveys(SurveyActivity.this, arrSurvey1, arrSurvey2, arrSurvey3);
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
                JSONArray[] recvSurv = Tools.readOfflineSurvey(this);
                if (recvSurv == null) {
                    Toast.makeText(this, "Please connect to a network first!", Toast.LENGTH_SHORT).show();
                    return;
                }

                LayoutInflater inflater = getLayoutInflater();
                for (int n = 0; n < recvSurv[0].length(); n++) {
                    inflater.inflate(R.layout.survey1_element, surveyChildHolder1);
                    TextView interv_text = surveyChildHolder1.getChildAt(n).findViewById(R.id.txt_survey_element);
                    JSONObject object = recvSurv[0].getJSONObject(n);
                    String survTxt = String.valueOf(n + 1) + ". " + object.getString("key");
                    interv_text.setText(survTxt);
                }
                for (int n = 0; n < recvSurv[1].length(); n++) {
                    inflater.inflate(R.layout.survey2_element, surveyChildHolder2);
                    TextView interv_text = surveyChildHolder2.getChildAt(n).findViewById(R.id.txt_survey_element);
                    JSONObject object = recvSurv[1].getJSONObject(n);
                    String survTxt = String.valueOf(n + 1) + ". " + object.getString("key");
                    interv_text.setText(survTxt);
                }
                for (int n = 0; n < recvSurv[2].length(); n++) {
                    inflater.inflate(R.layout.survey3_element, surveyChildHolder3);
                    TextView interv_text = surveyChildHolder3.getChildAt(n).findViewById(R.id.txt_survey_element);
                    JSONObject object = recvSurv[2].getJSONObject(n);
                    String survTxt = String.valueOf(n + 1) + ". " + object.getString("key");
                    interv_text.setText(survTxt);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void expandSurveyHolder1(View view) {
        if (surveyMainHolder1.getVisibility() == View.VISIBLE) {
            surveyMainHolder1.setVisibility(View.GONE);
            ((TextView) view).setCompoundDrawablesWithIntrinsicBounds(null, null, getDrawable(R.drawable.img_expand), null);
        } else {
            surveyMainHolder1.setVisibility(View.VISIBLE);
            ((TextView) view).setCompoundDrawablesWithIntrinsicBounds(null, null, getDrawable(R.drawable.img_collapse), null);
        }
    }

    public void expandSurveyHolder2(View view) {
        if (surveyMainHolder2.getVisibility() == View.VISIBLE) {
            surveyMainHolder2.setVisibility(View.GONE);
            ((TextView) view).setCompoundDrawablesWithIntrinsicBounds(null, null, getDrawable(R.drawable.img_expand), null);
        } else {
            surveyMainHolder2.setVisibility(View.VISIBLE);
            ((TextView) view).setCompoundDrawablesWithIntrinsicBounds(null, null, getDrawable(R.drawable.img_collapse), null);
        }
    }

    public void expandSurveyHolder3(View view) {
        if (surveyMainHolder3.getVisibility() == View.VISIBLE) {
            surveyMainHolder3.setVisibility(View.GONE);
            ((TextView) view).setCompoundDrawablesWithIntrinsicBounds(null, null, getDrawable(R.drawable.img_expand), null);
        } else {
            surveyMainHolder3.setVisibility(View.VISIBLE);
            ((TextView) view).setCompoundDrawablesWithIntrinsicBounds(null, null, getDrawable(R.drawable.img_collapse), null);
        }
    }

    public void saveClick(View view) {
        if (Tools.isNetworkAvailable(this))
            Tools.execute(new MyRunnable(
                    this,
                    getString(R.string.url_survey_submit, getString(R.string.server_ip)),
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
                        JSONArray survey1 = new JSONArray();
                        JSONArray survey2 = new JSONArray();
                        JSONArray survey3 = new JSONArray();

                        for (int n = 0; n < surveyChildHolder1.getChildCount(); n++)
                            survey1.put(((SeekBar) surveyChildHolder1.getChildAt(n).findViewById(R.id.scale)).getProgress());

                        for (int n = 0; n < surveyChildHolder2.getChildCount(); n++)
                            survey2.put(((SeekBar) surveyChildHolder2.getChildAt(n).findViewById(R.id.scale)).getProgress());

                        for (int n = 0; n < surveyChildHolder3.getChildCount(); n++)
                            survey3.put(((SeekBar) surveyChildHolder3.getChildAt(n).findViewById(R.id.scale)).getProgress());

                        body.put("username", username);
                        body.put("password", password);
                        body.put("survey1", survey1);
                        body.put("survey2", survey2);
                        body.put("survey3", survey3);

                        JSONObject res = new JSONObject(Tools.post(url, body));
                        switch (res.getInt("result")) {
                            case Tools.RES_OK:
                                runOnUiThread(new MyRunnable(
                                        activity
                                ) {
                                    @Override
                                    public void run() {
                                        Toast.makeText(SurveyActivity.this, "Survey is submitted successfully!", Toast.LENGTH_SHORT).show();
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
                                        Toast.makeText(SurveyActivity.this, "Failure in survey submission. Result = 1", Toast.LENGTH_SHORT).show();
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
                                        Toast.makeText(SurveyActivity.this, "Failure in survey submission creation. (SERVER SIDE)", Toast.LENGTH_SHORT).show();
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
                                Toast.makeText(SurveyActivity.this, "Failed to submit the survey.", Toast.LENGTH_SHORT).show();
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

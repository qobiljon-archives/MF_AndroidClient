package kr.ac.inha.nsl.mindforecaster;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
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

public class EventActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event);
        init();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK)
            switch (requestCode) {
                case INTERVENTION_ACTIVITY:
                    event.setIntervention(InterventionsActivity.resultIntervention.getDescription());
                    event.setInterventionReminder(InterventionsActivity.resultReminderMinutes);
                    selectedInterv.setText(event.getIntervention());
                    switch (InterventionsActivity.resultReminderMinutes) {
                        case -1440:
                            intervReminderTxt.setText(getString(R.string.intervention_reminder_text, getString(R.string._1_day_before)));
                            break;
                        case -60:
                            intervReminderTxt.setText(getString(R.string.intervention_reminder_text, getString(R.string._1_hour_before)));
                            break;
                        case -30:
                            intervReminderTxt.setText(getString(R.string.intervention_reminder_text, getString(R.string._30_minutes_before)));
                            break;
                        case -10:
                            intervReminderTxt.setText(getString(R.string.intervention_reminder_text, getString(R.string._10_minutes_before)));
                            break;
                        case 1440:
                            intervReminderTxt.setText(getString(R.string.intervention_reminder_text1, getString(R.string._1_day_after1)));
                            break;
                        case 60:
                            intervReminderTxt.setText(getString(R.string.intervention_reminder_text1, getString(R.string._1_hour_after1)));
                            break;
                        case 30:
                            intervReminderTxt.setText(getString(R.string.intervention_reminder_text1, getString(R.string._30_minutes_after1)));
                            break;
                        case 10:
                            intervReminderTxt.setText(getString(R.string.intervention_reminder_text1, getString(R.string._10_minutes_after1)));
                            break;
                        default:
                            if (InterventionsActivity.resultReminderMinutes > 0) {
                                intervReminderTxt.setText(getString(R.string.intervention_reminder_text1, Tools.notifMinsToString(this, InterventionsActivity.resultReminderMinutes)));

                            } else
                                intervReminderTxt.setText(getString(R.string.intervention_reminder_text, Tools.notifMinsToString(this, InterventionsActivity.resultReminderMinutes)));
                            break;
                    }
                    intervReminderTxt.setVisibility(View.VISIBLE);
                    break;
                case EVALUATION_ACTIVITY:
                    finish();
                    overridePendingTransition(R.anim.activity_in_reverse, R.anim.activity_out_reverse);
                    break;
                default:
                    break;
            }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        setResult(Activity.RESULT_CANCELED);
        super.onBackPressed();
    }

    //region Variables
    private final int EVALUATION_ACTIVITY = 0, INTERVENTION_ACTIVITY = 1;
    static Event event;

    private ViewGroup inactiveLayout;
    private ViewGroup stressLevelDetails;
    private ViewGroup interventionDetails;
    private ViewGroup repeatDetails;
    private ViewGroup notificationDetails;
    private TextView startDateText;
    private TextView startTimeText;
    private TextView endDateText;
    private TextView endTimeText;
    private TextView selectedInterv;
    private TextView intervReminderTxt;
    private TextView saveButton;
    private TextView cancelButton;
    private TextView deleteButton;
    private TextView repeatValueText;
    private TextView stressLevelValueText;
    private TextView notificationValueText;
    private TextView interventionTextView;
    private TextView tabEvaluation;
    private TextView activityTitle;
    private RadioButton customNotifRadioButton;
    private RadioGroup stressTypeGroup, repeatModeGroup, eventNotificationGroup;
    private EditText eventTitle, stressCause;
    private Switch switchAllDay;
    private SeekBar stressLvl;
    private ViewGroup weekdaysGroup;
    private CheckBox[] repeatWeeklDayChecks = new CheckBox[7];
    //endregion

    private void init() {
        eventTitle = findViewById(R.id.edit_event_title);
        switchAllDay = findViewById(R.id.all_day_switch);
        startDateText = findViewById(R.id.txt_event_start_date);
        startTimeText = findViewById(R.id.txt_event_start_time);
        endDateText = findViewById(R.id.txt_event_end_date);
        endTimeText = findViewById(R.id.txt_event_end_time);
        stressLvl = findViewById(R.id.real_stress_level_seek);
        inactiveLayout = findViewById(R.id.layout_to_be_inactive);
        stressTypeGroup = findViewById(R.id.stress_type_group);
        stressCause = findViewById(R.id.txt_stress_cause);
        activityTitle = findViewById(R.id.activity_title);
        ViewGroup tabNotif = findViewById(R.id.tab_notification);
        ViewGroup tabRepeat = findViewById(R.id.tab_repeat);
        ViewGroup tabStressLvl = findViewById(R.id.tab_anticipated_strs_lvl);
        interventionTextView = findViewById(R.id.tab_interventions);
        stressLevelDetails = findViewById(R.id.stress_level_details);
        interventionDetails = findViewById(R.id.intervention_details);
        repeatDetails = findViewById(R.id.repeat_details);
        notificationDetails = findViewById(R.id.notification_details);
        ViewGroup resultDetails = findViewById(R.id.result_details);
        selectedInterv = findViewById(R.id.selected_intervention);
        repeatModeGroup = findViewById(R.id.repeat_mode_group);
        eventNotificationGroup = findViewById(R.id.event_notification_group);
        intervReminderTxt = findViewById(R.id.txt_interv_reminder_time);
        saveButton = findViewById(R.id.btn_create);
        cancelButton = findViewById(R.id.btn_cancel);
        deleteButton = findViewById(R.id.btn_delete);
        customNotifRadioButton = findViewById(R.id.radio_btn_custom);
        ViewGroup postEventLayout = findViewById(R.id.postEventLayout);
        repeatValueText = findViewById(R.id.info_txt_repeat);
        stressLevelValueText = findViewById(R.id.info_txt_stress_level);
        notificationValueText = findViewById(R.id.info_txt_notification);
        tabEvaluation = findViewById(R.id.tab_evaluation);

        weekdaysGroup = findViewById(R.id.weekdays_group);
        repeatWeeklDayChecks[0] = findViewById(R.id.sun);
        repeatWeeklDayChecks[1] = findViewById(R.id.mon);
        repeatWeeklDayChecks[2] = findViewById(R.id.tue);
        repeatWeeklDayChecks[3] = findViewById(R.id.wed);
        repeatWeeklDayChecks[4] = findViewById(R.id.thu);
        repeatWeeklDayChecks[5] = findViewById(R.id.fri);
        repeatWeeklDayChecks[6] = findViewById(R.id.sat);

        if (getIntent().hasExtra("eventId")) {
            // Editing an existing event
            event = Event.getEventById(getIntent().getLongExtra("eventId", 0));
            event.setRepeatTill(0);

            switchAllDay.setEnabled(false);
            eventTitle.setEnabled(false);
            startDateText.setEnabled(false);
            startTimeText.setEnabled(false);
            endDateText.setEnabled(false);
            endTimeText.setEnabled(false);
            stressLvl.setEnabled(false);
            stressCause.setEnabled(false);
            stressTypeGroup.setEnabled(false);
            for (int n = 0; n < stressTypeGroup.getChildCount(); n++)
                stressTypeGroup.getChildAt(n).setEnabled(false);
            repeatModeGroup.setEnabled(false);
            for (int n = 0; n < repeatModeGroup.getChildCount(); n++)
                repeatModeGroup.getChildAt(n).setEnabled(false);
            eventNotificationGroup.setEnabled(false);
            for (int n = 0; n < eventNotificationGroup.getChildCount(); n++)
                eventNotificationGroup.getChildAt(n).setEnabled(false);
            selectedInterv.setEnabled(false);

            if (event.getEndTime().before(Calendar.getInstance(Locale.getDefault()))) {
                saveButton.setVisibility(View.GONE);
                cancelButton.setVisibility(View.GONE);
                postEventLayout.setVisibility(View.VISIBLE);

                tabNotif.setVisibility(View.GONE);
                tabRepeat.setVisibility(View.GONE);
                tabStressLvl.setVisibility(View.GONE);
                interventionTextView.setVisibility(View.GONE);

                tabEvaluation.setText(getString(R.string.evaluation));
                if (event.isEvaluated()) {
                    initResultView();
                    resultDetails.setVisibility(View.VISIBLE);
                }
            } else {
                saveButton.setText(getString(R.string.edit));
                cancelButton.setVisibility(View.GONE);
            }

            eventTitle.setFocusable(false);
            eventTitle.clearFocus();
        } else {
            event = new Event(0);
            event.setRepeatTill(0);
            event.setStartTime(Calendar.getInstance(Locale.getDefault()));
            event.setEndTime(Calendar.getInstance(Locale.getDefault()));
            event.setStressType("unknown");
            event.setInterventionReminder(0);
            deleteButton.setVisibility(View.GONE);

            if (getIntent().hasExtra("selectedDayMillis")) {
                Calendar startTime = event.getStartTime();
                Calendar endTime = event.getEndTime();

                startTime.setTimeInMillis(getIntent().getLongExtra("selectedDayMillis", 0));
                startTime.set(Calendar.HOUR_OF_DAY, Calendar.getInstance(Locale.getDefault()).get(Calendar.HOUR_OF_DAY) + 1);
                startTime.set(Calendar.MINUTE, 0);
                endTime.setTimeInMillis(startTime.getTimeInMillis());
                endTime.add(Calendar.HOUR_OF_DAY, 1);

                event.setStartTime(startTime);
                event.setEndTime(endTime);
            }
        }

        fillOutExistingValues();

        Calendar startTime = event.getStartTime();
        Calendar endTime = event.getEndTime();

        startDateText.setText(String.format(Locale.getDefault(),
                "%d, %s %d, %s",
                startTime.get(Calendar.YEAR),
                startTime.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()),
                startTime.get(Calendar.DAY_OF_MONTH),
                startTime.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault())
        ));
        startTimeText.setText(String.format(Locale.getDefault(),
                "%02d:%02d",
                startTime.get(Calendar.HOUR_OF_DAY),
                startTime.get(Calendar.MINUTE))
        );
        endDateText.setText(String.format(Locale.getDefault(),
                "%d, %s %d, %s",
                endTime.get(Calendar.YEAR),
                endTime.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()),
                endTime.get(Calendar.DAY_OF_MONTH),
                endTime.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault())
        ));
        endTimeText.setText(String.format(Locale.getDefault(),
                "%02d:%02d",
                endTime.get(Calendar.HOUR_OF_DAY),
                endTime.get(Calendar.MINUTE))
        );

        stressLvl.getProgressDrawable().setColorFilter(ResourcesCompat.getColor(getResources(), R.color.slvl0_color, null), PorterDuff.Mode.SRC_IN);
        stressLvl.getThumb().setColorFilter(ResourcesCompat.getColor(getResources(), R.color.slvl0_color, null), PorterDuff.Mode.SRC_IN);

        switchAllDay.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    startTimeText.setVisibility(View.GONE);
                    endTimeText.setVisibility(View.GONE);

                    Calendar startTime = event.getStartTime();
                    Calendar endTime = event.getEndTime();

                    startTime.set(Calendar.HOUR_OF_DAY, 0);
                    startTime.set(Calendar.MINUTE, 0);
                    startTime.set(Calendar.SECOND, 0);
                    startTime.set(Calendar.MILLISECOND, 0);

                    event.setStartTime(startTime);
                    event.setEndTime(endTime);
                } else {
                    startTimeText.setVisibility(View.VISIBLE);
                    endTimeText.setVisibility(View.VISIBLE);
                }
            }
        });

        repeatModeGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, final int checkedId) {
                for (CheckBox cb : repeatWeeklDayChecks)
                    cb.setChecked(false);
                weekdaysGroup.setVisibility(View.GONE);

                if (checkedId == R.id.no_repeat_radio) {
                    event.setRepeatMode(Event.NO_REPEAT);
                    event.setRepeatTill(0);
                    repeatValueText.setText(getString(R.string.only_oncehihine));
                    return;
                }

                Calendar cal = Calendar.getInstance(Locale.getDefault());
                cal.setTimeInMillis(event.getStartTime().getTimeInMillis());

                DatePickerDialog.OnDateSetListener listener = new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker picker, int year, int month, int dayOfMonth) {
                        Calendar tmpCal = Calendar.getInstance(Locale.getDefault());
                        tmpCal.set(year, month, dayOfMonth, 0, 0, 0);
                        tmpCal.set(Calendar.MILLISECOND, 0);
                        Calendar calTill = (Calendar) tmpCal.clone();

                        tmpCal.add(Calendar.DAY_OF_MONTH, 1);
                        if (event.getStartTime().after(tmpCal))
                            event.setRepeatTill(0);
                        else
                            event.setRepeatTill(tmpCal.getTimeInMillis() / 1000);

                        switch (checkedId) {
                            case R.id.everyday_repeat_radio:
                                event.setRepeatMode(Event.REPEAT_EVERYDAY);
                                calTill.setTimeInMillis(event.getRepeatTill());
                                repeatValueText.setText(String.format(getString(R.string.everyday_repeat), calTill.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()), calTill.get(Calendar.DAY_OF_MONTH)));
                                break;
                            case R.id.everyweek_repeat_radio:
                                for (CheckBox cb : repeatWeeklDayChecks)
                                    cb.setChecked(false);
                                repeatWeeklDayChecks[event.getStartTime().get(Calendar.DAY_OF_WEEK) - 1].setChecked(true);
                                weekdaysGroup.setVisibility(View.VISIBLE);
                                event.setRepeatMode(Event.REPEAT_WEEKLY);
                                calTill.setTimeInMillis(event.getRepeatTill());
                                repeatValueText.setText(String.format(getString(R.string.everyweek_repeat), calTill.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()), calTill.get(Calendar.DAY_OF_MONTH)));
                                break;
                            default:
                                break;
                        }
                    }
                };

                DatePickerDialog dialog = new DatePickerDialog(EventActivity.this, R.style.DialogTheme, listener, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
                dialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == DialogInterface.BUTTON_NEGATIVE) {
                            for (CheckBox cb : repeatWeeklDayChecks)
                                cb.setChecked(false);
                            weekdaysGroup.setVisibility(View.GONE);
                            repeatModeGroup.check(R.id.no_repeat_radio);
                        }
                    }
                });
                dialog.setTitle(getString(R.string.repeat_until));
                dialog.show();
            }
        });
    }

    private void fillOutExistingValues() {
        InterventionsActivity.resultIntervention = Intervention.getInterventionByDescription(event.getIntervention());
        InterventionsActivity.resultReminderMinutes = event.getInterventionReminder();

        eventTitle.setText(event.getTitle());
        stressLvl.setProgress(event.getStressLevel());
        switch (event.getStressType()) {
            case "positive":
                stressTypeGroup.check(R.id.stressor_positive);
                break;
            case "negative":
                stressTypeGroup.check(R.id.stressor_negative);
                break;
            case "unknown":
                stressTypeGroup.check(R.id.stressor_unknown);
                break;
            default:
                break;
        }

        stressCause.setText(event.getStressCause());
        switch (event.getRepeatMode()) {
            case Event.NO_REPEAT:
                repeatModeGroup.check(R.id.no_repeat_radio);
                break;
            case Event.REPEAT_EVERYDAY:
                repeatModeGroup.check(R.id.everyday_repeat_radio);
                break;
            case Event.REPEAT_WEEKLY:
                repeatModeGroup.check(R.id.everyweek_repeat_radio);
                break;
            default:
                break;
        }

        notificationValueText.setText(Tools.notifMinsToString(this, event.getEventReminder()));
        switch (event.getEventReminder()) {
            case 0:
                eventNotificationGroup.check(R.id.option_none);
                break;
            case -1440:
                eventNotificationGroup.check(R.id.option_day_before);
                break;
            case -60:
                eventNotificationGroup.check(R.id.option_hour_before);
                break;
            case -30:
                eventNotificationGroup.check(R.id.option_30mins_before);
                break;
            case -10:
                eventNotificationGroup.check(R.id.option_10mins_before);
                break;
            default:
                eventNotificationGroup.check(R.id.radio_btn_custom);
                customNotifRadioButton.setText(Tools.notifMinsToString(this, event.getEventReminder()));
                customNotifRadioButton.setVisibility(View.VISIBLE);
                break;
        }

        switchAllDay.setChecked(event.getStartTime().get(Calendar.HOUR_OF_DAY) == 0 && event.getStartTime().get(Calendar.MINUTE) == 0 && event.getStartTime().get(Calendar.SECOND) == 0 &&
                event.getStartTime().get(Calendar.MILLISECOND) == 0 && event.getEndTime().get(Calendar.HOUR_OF_DAY) == 0 && event.getEndTime().get(Calendar.MINUTE) == 0 &&
                event.getEndTime().get(Calendar.SECOND) == 0 && event.getEndTime().get(Calendar.MILLISECOND) == 0
        );
        selectedInterv.setText(event.getIntervention());
        switch (event.getInterventionReminder()) {
            case 0:
                intervReminderTxt.setVisibility(View.GONE);
                break;
            case -1440:
                intervReminderTxt.setText(getString(R.string.intervention_reminder_text, getResources().getString(R.string._1_day_before)));
                break;
            case -60:
                intervReminderTxt.setText(getString(R.string.intervention_reminder_text, getResources().getString(R.string._1_hour_before)));
                break;
            case -30:
                intervReminderTxt.setText(getString(R.string.intervention_reminder_text, getResources().getString(R.string._30_minutes_before)));
                break;
            case -10:
                intervReminderTxt.setText(getString(R.string.intervention_reminder_text, getResources().getString(R.string._10_minutes_before)));
                break;
            case 1440:
                intervReminderTxt.setText(getString(R.string.intervention_reminder_text1, getResources().getString(R.string._1_day_after1)));
                break;
            case 60:
                intervReminderTxt.setText(getString(R.string.intervention_reminder_text1, getResources().getString(R.string._1_hour_after1)));
                break;
            case 30:
                intervReminderTxt.setText(getString(R.string.intervention_reminder_text1, getResources().getString(R.string._30_minutes_after1)));
                break;
            case 10:
                intervReminderTxt.setText(getString(R.string.intervention_reminder_text1, getResources().getString(R.string._10_minutes_after1)));
                break;
            default:
                if (event.getInterventionReminder() > 0) {
                    intervReminderTxt.setText(getString(R.string.intervention_reminder_text1, Tools.notifMinsToString(this, event.getInterventionReminder())));

                } else
                    intervReminderTxt.setText(getString(R.string.intervention_reminder_text, Tools.notifMinsToString(this, event.getInterventionReminder())));
                break;
        }


        Calendar calTill = Calendar.getInstance(Locale.getDefault());
        switch (event.getRepeatMode()) {
            case Event.NO_REPEAT:
                repeatValueText.setText(getString(R.string.only_oncehihine));
                break;
            case Event.REPEAT_EVERYDAY:
                calTill.setTimeInMillis(event.getRepeatTill());
                repeatValueText.setText(String.format(getString(R.string.everyday_repeat), calTill.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()), calTill.get(Calendar.DAY_OF_MONTH)));
                break;
            case Event.REPEAT_WEEKLY:
                calTill.setTimeInMillis(event.getRepeatTill());
                repeatValueText.setText(String.format(getString(R.string.everyweek_repeat), calTill.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()), calTill.get(Calendar.DAY_OF_MONTH)));
                break;
            default:
                repeatValueText.setText("");
                break;
        }

        switch (event.getStressLevel()) {
            case 0:
                stressLevelValueText.setText(getResources().getString(R.string.not_at_all));
                break;
            case 1:
                stressLevelValueText.setText(getResources().getString(R.string.low));
                break;
            case 2:
                stressLevelValueText.setText(getResources().getString(R.string.normal));
                break;
            case 3:
                stressLevelValueText.setText(getResources().getString(R.string.high));
                break;
            case 4:
                stressLevelValueText.setText(getResources().getString(R.string.extreme));
                break;
            default:
                stressLevelValueText.setText("");
                break;
        }

        eventTitle.setSelection(eventTitle.length());
    }

    public void expandStressLevelClick(View view) {
        if (stressLevelDetails.getVisibility() == View.VISIBLE) {
            stressLevelDetails.setVisibility(View.GONE);
            stressLevelValueText.setCompoundDrawablesWithIntrinsicBounds(null, null, getDrawable(R.drawable.img_expand), null);
        } else {
            stressLevelDetails.setVisibility(View.VISIBLE);
            stressLevelValueText.setCompoundDrawablesWithIntrinsicBounds(null, null, getDrawable(R.drawable.img_collapse), null);
            findViewById(R.id.tab_interventions).getParent().requestChildFocus(findViewById(R.id.tab_interventions), findViewById(R.id.tab_interventions));
        }

        stressLvl.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                stressLvl.getProgressDrawable().setColorFilter(Tools.stressLevelToColor(getApplicationContext(), progress), PorterDuff.Mode.SRC_IN);
                stressLvl.getThumb().setColorFilter(Tools.stressLevelToColor(getApplicationContext(), progress), PorterDuff.Mode.SRC_IN);
                if (progress > 0) {
                    inactiveLayout.setVisibility(View.VISIBLE);
                    findViewById(R.id.tab_interventions).getParent().requestChildFocus(findViewById(R.id.tab_interventions), findViewById(R.id.tab_interventions));
                } else inactiveLayout.setVisibility(View.GONE);

                switch (progress) {
                    case 0:
                        stressLevelValueText.setText(getResources().getString(R.string.not_at_all));
                        break;
                    case 1:
                        stressLevelValueText.setText(getResources().getString(R.string.low));
                        break;
                    case 2:
                        stressLevelValueText.setText(getResources().getString(R.string.normal));
                        break;
                    case 3:
                        stressLevelValueText.setText(getResources().getString(R.string.high));
                        break;
                    case 4:
                        stressLevelValueText.setText(getResources().getString(R.string.extreme));
                        break;
                    default:
                        break;
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

    public void expandNotificationClick(View view) {
        if (notificationDetails.getVisibility() == View.VISIBLE) {
            notificationDetails.setVisibility(View.GONE);
            notificationValueText.setCompoundDrawablesWithIntrinsicBounds(null, null, getDrawable(R.drawable.img_expand), null);
        } else {
            notificationDetails.setVisibility(View.VISIBLE);
            notificationValueText.setCompoundDrawablesWithIntrinsicBounds(null, null, getDrawable(R.drawable.img_collapse), null);
            findViewById(R.id.tab_anticipated_strs_lvl).getParent().requestChildFocus(findViewById(R.id.tab_anticipated_strs_lvl), findViewById(R.id.tab_anticipated_strs_lvl));
        }

        eventNotificationGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                notificationValueText.setText(((RadioButton) group.findViewById(checkedId)).getText().toString());
            }
        });
    }

    public void expandRepeatClick(View view) {
        if (repeatDetails.getVisibility() == View.VISIBLE) {
            repeatDetails.setVisibility(View.GONE);
            repeatValueText.setCompoundDrawablesWithIntrinsicBounds(null, null, getDrawable(R.drawable.img_expand), null);
        } else {
            repeatDetails.setVisibility(View.VISIBLE);
            repeatValueText.setCompoundDrawablesWithIntrinsicBounds(null, null, getDrawable(R.drawable.img_collapse), null);
            findViewById(R.id.tab_anticipated_strs_lvl).getParent().requestChildFocus(findViewById(R.id.tab_anticipated_strs_lvl), findViewById(R.id.tab_anticipated_strs_lvl));
        }
    }

    public void expandInterventionsClick(View view) {
        if (interventionDetails.getVisibility() == View.VISIBLE) {
            interventionDetails.setVisibility(View.GONE);
            interventionTextView.setCompoundDrawablesWithIntrinsicBounds(getDrawable(R.drawable.icon_intervention), null, getDrawable(R.drawable.img_expand), null);
        } else {
            interventionDetails.setVisibility(View.VISIBLE);
            interventionTextView.setCompoundDrawablesWithIntrinsicBounds(getDrawable(R.drawable.icon_intervention), null, getDrawable(R.drawable.img_collapse), null);
            interventionDetails.getParent().requestChildFocus(interventionDetails, interventionDetails);

            if (event.getIntervention() != null && event.getIntervention().length() > 0) {
                switch (event.getInterventionReminder()) {
                    case -1440:
                        intervReminderTxt.setText(getResources().getString(R.string.intervention_reminder_text, getResources().getString(R.string._1_day_before)));
                        break;
                    case -60:
                        intervReminderTxt.setText(getResources().getString(R.string.intervention_reminder_text, getResources().getString(R.string._1_hour_before)));
                        break;
                    case -30:
                        intervReminderTxt.setText(getResources().getString(R.string.intervention_reminder_text, getResources().getString(R.string._30_minutes_before)));
                        break;
                    case -10:
                        intervReminderTxt.setText(getResources().getString(R.string.intervention_reminder_text, getResources().getString(R.string._10_minutes_before)));
                        break;
                    case 1440:
                        intervReminderTxt.setText(getResources().getString(R.string.intervention_reminder_text1, getResources().getString(R.string._1_day_after1)));
                        break;
                    case 60:
                        intervReminderTxt.setText(getResources().getString(R.string.intervention_reminder_text1, getResources().getString(R.string._1_hour_after1)));
                        break;
                    case 30:
                        intervReminderTxt.setText(getResources().getString(R.string.intervention_reminder_text1, getResources().getString(R.string._30_minutes_after1)));
                        break;
                    case 10:
                        intervReminderTxt.setText(getResources().getString(R.string.intervention_reminder_text1, getResources().getString(R.string._10_minutes_after1)));
                        break;
                    default:
                        if (event.getInterventionReminder() > 0) {
                            intervReminderTxt.setText(getString(R.string.intervention_reminder_text1, Tools.notifMinsToString(this, event.getInterventionReminder())));

                        } else
                            intervReminderTxt.setText(getString(R.string.intervention_reminder_text, Tools.notifMinsToString(this, event.getInterventionReminder())));
                        break;
                }
                intervReminderTxt.setVisibility(View.VISIBLE);
            } else
                intervReminderTxt.setVisibility(View.GONE);
        }

    }

    public void editInterventionClick(View view) {
        Intent intent = new Intent(this, InterventionsActivity.class);
        intent.putExtra("eventTitle", eventTitle.getText().toString());
        startActivityForResult(intent, INTERVENTION_ACTIVITY);
        overridePendingTransition(R.anim.activity_in, R.anim.activity_out);
    }

    public void evaluationClick(View view) {
        Intent intent = new Intent(this, EvaluationActivity.class);
        startActivityForResult(intent, EVALUATION_ACTIVITY);
        overridePendingTransition(R.anim.activity_in, R.anim.activity_out);
    }

    public void cancelClick(View view) {
        setResult(Activity.RESULT_CANCELED);
        finish();
        overridePendingTransition(R.anim.activity_in_reverse, R.anim.activity_out_reverse);
    }

    public void deleteClick(View view) {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        if (Tools.isNetworkAvailable(EventActivity.this))
                            Tools.execute(new MyRunnable(
                                    EventActivity.this,
                                    getString(R.string.url_event_delete, getString(R.string.server_ip)),
                                    SignInActivity.loginPrefs.getString(SignInActivity.username, null),
                                    SignInActivity.loginPrefs.getString(SignInActivity.password, null),
                                    event.getEventId()
                            ) {
                                @Override
                                public void run() {
                                    String url = (String) args[0];
                                    String username = (String) args[1];
                                    String password = (String) args[2];
                                    long eventId = (long) args[3];

                                    List<NameValuePair> params = new ArrayList<>();
                                    try {
                                        params.add(new BasicNameValuePair("username", username));
                                        params.add(new BasicNameValuePair("password", password));
                                        params.add(new BasicNameValuePair("eventId", String.valueOf(eventId)));

                                        JSONObject res = new JSONObject(Tools.post(url, params));
                                        switch (res.getInt("result")) {
                                            case Tools.RES_OK:
                                                runOnUiThread(new MyRunnable(activity, eventId) {
                                                    @Override
                                                    public void run() {
                                                        long eventId = (long) args[0];
                                                        Tools.cancelNotif(EventActivity.this, (int) eventId);
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
                                                        Toast.makeText(EventActivity.this, "Failed to create delete the event.", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                                break;
                                            case Tools.RES_SRV_ERR:
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        Toast.makeText(EventActivity.this, "Failure occurred while processing the request. (SERVER SIDE)", Toast.LENGTH_SHORT).show();
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
                                                Toast.makeText(EventActivity.this, "Failed to proceed due to an error in connection with server.", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }
                                    enableTouch();
                                }
                            });
                        else
                            Toast.makeText(EventActivity.this, "Please connect to a network first!", Toast.LENGTH_SHORT).show();
                        break;
                    case DialogInterface.BUTTON_NEUTRAL:
                        if (Tools.isNetworkAvailable(EventActivity.this))
                            Tools.execute(new MyRunnable(
                                    EventActivity.this,
                                    getString(R.string.url_event_delete, getString(R.string.server_ip)),
                                    SignInActivity.loginPrefs.getString(SignInActivity.username, null),
                                    SignInActivity.loginPrefs.getString(SignInActivity.password, null),
                                    event.getRepeatId()
                            ) {
                                @Override
                                public void run() {
                                    String url = (String) args[0];
                                    String username = (String) args[1];
                                    String password = (String) args[2];
                                    long repeatId = (long) args[3];

                                    List<NameValuePair> params = new ArrayList<>();
                                    try {
                                        params.add(new BasicNameValuePair("username", username));
                                        params.add(new BasicNameValuePair("password", password));
                                        params.add(new BasicNameValuePair("repeatId", String.valueOf(repeatId)));

                                        JSONObject res = new JSONObject(Tools.post(url, params));
                                        long[] deletedIds = null;
                                        if (res.has("deleted_event_ids")) {
                                            JSONArray array = res.getJSONArray("deleted_event_ids");
                                            deletedIds = new long[array.length()];
                                            for (int n = 0; n < deletedIds.length; n++)
                                                deletedIds[n] = array.getLong(n);
                                        }
                                        switch (res.getInt("result")) {
                                            case Tools.RES_OK:
                                                runOnUiThread(new MyRunnable(activity, (Object) deletedIds) {
                                                    @Override
                                                    public void run() {
                                                        long[] deletedIds = (long[]) args[0];

                                                        Toast.makeText(EventActivity.this, "Events have been deleted!", Toast.LENGTH_SHORT).show();
                                                        for (long deletedId : deletedIds)
                                                            Tools.cancelNotif(EventActivity.this, (int) deletedId);
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
                                                        Toast.makeText(EventActivity.this, "Failed to delete the recurring events.", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                                break;
                                            case Tools.RES_SRV_ERR:
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        Toast.makeText(EventActivity.this, "Failure occurred while processing the request. (SERVER SIDE)", Toast.LENGTH_SHORT).show();
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
                                                Toast.makeText(EventActivity.this, "Failed to proceed due to an error in connection with server.", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }
                                    enableTouch();
                                }
                            });
                        else
                            Toast.makeText(EventActivity.this, "Please connect to a network first!", Toast.LENGTH_SHORT).show();
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                    default:
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (event.getRepeatMode() == Event.NO_REPEAT)
            builder.setMessage("Are you sure you want to delete this event?").setPositiveButton("Yes", dialogClickListener).setNegativeButton("No", dialogClickListener).show();
        else {
            builder.setMessage("Delete recurring event")
                    .setPositiveButton("This event only", dialogClickListener)
                    .setNeutralButton("All recurring events", dialogClickListener)
                    .setNegativeButton("Cancel", dialogClickListener).show();
        }
    }

    public void saveClick(View view) {
        if (saveButton.getText().equals(getString(R.string.edit))) {
            // if edit is clicked
            saveButton.setText(getString(R.string.save));
            cancelButton.setVisibility(View.VISIBLE);
            deleteButton.setVisibility(View.GONE);

            switchAllDay.setEnabled(true);
            eventTitle.setEnabled(true);
            startDateText.setEnabled(true);
            startTimeText.setEnabled(true);
            endDateText.setEnabled(true);
            endTimeText.setEnabled(true);
            stressLvl.setEnabled(true);
            stressCause.setEnabled(true);
            stressTypeGroup.setEnabled(true);
            for (int n = 0; n < stressTypeGroup.getChildCount(); n++)
                stressTypeGroup.getChildAt(n).setEnabled(true);
            repeatModeGroup.setEnabled(true);
            for (int n = 0; n < repeatModeGroup.getChildCount(); n++)
                repeatModeGroup.getChildAt(n).setEnabled(true);

            eventNotificationGroup.setEnabled(true);
            for (int n = 0; n < eventNotificationGroup.getChildCount(); n++)
                eventNotificationGroup.getChildAt(n).setEnabled(true);
            selectedInterv.setEnabled(true);

            eventTitle.setFocusableInTouchMode(true);
            eventTitle.setFocusable(true);
            eventTitle.requestFocus();
            return;
        }

        // check the input values
        if (eventTitle.length() > 0)
            event.setTitle(eventTitle.getText().toString());
        else {
            Toast.makeText(this, "Please, type the event title", Toast.LENGTH_SHORT).show();
            return;
        }
        Calendar startTime = event.getStartTime();
        Calendar endTime = event.getEndTime();
        if (!startTime.before(endTime)) {
            Toast.makeText(this, "Event start-time must be after it's end-time. Please try again!", Toast.LENGTH_LONG).show();
            return;
        }

        event.setStressLevel(stressLvl.getProgress());

        if (switchAllDay.isChecked()) {
            startTime.set(Calendar.HOUR_OF_DAY, 0);
            startTime.set(Calendar.MINUTE, 0);
            startTime.set(Calendar.SECOND, 0);
            startTime.set(Calendar.MILLISECOND, 0);

            endTime.setTimeInMillis(startTime.getTimeInMillis());
            endTime.add(Calendar.DAY_OF_MONTH, 1);

            event.setStartTime(startTime);
            event.setEndTime(endTime);
        }

        switch (stressTypeGroup.getCheckedRadioButtonId()) {
            case R.id.stressor_positive:
                event.setStressType("positive");
                break;
            case R.id.stressor_negative:
                event.setStressType("negative");
                break;
            case R.id.stressor_unknown:
                event.setStressType("unknown");
                break;
            default:
                event.setStressType("unknown");
                break;
        }
        event.setStressCause(stressCause.getText().toString());
        switch (repeatModeGroup.getCheckedRadioButtonId()) {
            case R.id.no_repeat_radio:
                event.setRepeatMode(Event.NO_REPEAT);
                break;
            case R.id.everyday_repeat_radio:
                event.setRepeatMode(Event.REPEAT_EVERYDAY);
                break;
            case R.id.everyweek_repeat_radio:
                event.setRepeatMode(Event.REPEAT_WEEKLY);
                break;
            default:
                break;
        }

        switch (eventNotificationGroup.getCheckedRadioButtonId()) {
            case R.id.option_none:
                event.setEventReminder((short) 0);
                break;
            case R.id.option_day_before:
                event.setEventReminder((short) -1440);
                break;
            case R.id.option_hour_before:
                event.setEventReminder((short) -60);
                break;
            case R.id.option_30mins_before:
                event.setEventReminder((short) -30);
                break;
            case R.id.option_10mins_before:
                event.setEventReminder((short) -10);
                break;
            default:
                break;
        }

        event.setEvaluated(false);

        if (Tools.isNetworkAvailable(this))
            createEvent();
        else
            Toast.makeText(this, "No network! Please connect to network first!", Toast.LENGTH_SHORT).show();
    }

    private void createEvent() {
        Tools.execute(new MyRunnable(
                this,
                EventActivity.event.isNewEvent() ? getString(R.string.url_event_create, getString(R.string.server_ip)) : getString(R.string.url_event_edit, getString(R.string.server_ip)),
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
                    params.add(new BasicNameValuePair("eventId", String.valueOf(event.getEventId())));
                    params.add(new BasicNameValuePair("title", event.getTitle()));
                    params.add(new BasicNameValuePair("stressLevel", String.valueOf(event.getStressLevel())));
                    params.add(new BasicNameValuePair("startTime", String.valueOf(event.getStartTime().getTimeInMillis() / 1000)));
                    params.add(new BasicNameValuePair("endTime", String.valueOf(event.getEndTime().getTimeInMillis() / 1000)));
                    if (event.getIntervention() == null) {
                        params.add(new BasicNameValuePair("intervention", ""));
                        params.add(new BasicNameValuePair("interventionReminder", "0"));
                    } else {
                        params.add(new BasicNameValuePair("intervention", event.getIntervention()));
                        params.add(new BasicNameValuePair("interventionReminder", String.valueOf(event.getInterventionReminder())));
                    }
                    params.add(new BasicNameValuePair("stressType", event.getStressType()));
                    params.add(new BasicNameValuePair("stressCause", event.getStressCause()));
                    params.add(new BasicNameValuePair("repeatMode", String.valueOf(event.getRepeatMode())));
                    params.add(new BasicNameValuePair("repeatTill", String.valueOf(event.getRepeatTill())));
                    params.add(new BasicNameValuePair("eventReminder", String.valueOf(event.getEventReminder())));
                    params.add(new BasicNameValuePair("isEvaluated", String.valueOf(event.isEvaluated())));

                    params.add(new BasicNameValuePair("sun", String.valueOf(repeatWeeklDayChecks[0].isChecked())));
                    params.add(new BasicNameValuePair("mon", String.valueOf(repeatWeeklDayChecks[1].isChecked())));
                    params.add(new BasicNameValuePair("tue", String.valueOf(repeatWeeklDayChecks[2].isChecked())));
                    params.add(new BasicNameValuePair("wed", String.valueOf(repeatWeeklDayChecks[3].isChecked())));
                    params.add(new BasicNameValuePair("thu", String.valueOf(repeatWeeklDayChecks[4].isChecked())));
                    params.add(new BasicNameValuePair("fri", String.valueOf(repeatWeeklDayChecks[5].isChecked())));
                    params.add(new BasicNameValuePair("sat", String.valueOf(repeatWeeklDayChecks[6].isChecked())));

                    JSONObject res = new JSONObject(Tools.post(url, params));
                    switch (res.getInt("result")) {
                        case Tools.RES_OK:
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Intervention intervention = Intervention.getInterventionByDescription(event.getIntervention());
                                    if (intervention != null)
                                        intervention.increaseNumberOfSelections();
                                    Toast.makeText(EventActivity.this, EventActivity.event.isNewEvent() ? "Event successfully created!" : "Event has been edited.", Toast.LENGTH_SHORT).show();

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
                                    Toast.makeText(EventActivity.this, EventActivity.event.isNewEvent() ? "Failed to create the event." : "Failed to edit the event.", Toast.LENGTH_SHORT).show();
                                }
                            });
                            break;
                        case Tools.RES_SRV_ERR:
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(EventActivity.this, "Failure occurred while processing the request. (SERVER SIDE)", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(EventActivity.this, "Failed to proceed due to an error in connection with server.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                enableTouch();
            }
        });
    }

    public void pickStartDateClick(View view) {
        DatePickerDialog.OnDateSetListener listener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker picker, int year, int month, int dayOfMonth) {
                Calendar startDate = event.getStartTime();
                startDate.set(year, month, dayOfMonth);
                event.setStartTime(startDate);

                startDateText.setText(String.format(Locale.getDefault(),
                        "%d, %s %d, %s",
                        startDate.get(Calendar.YEAR),
                        startDate.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()),
                        startDate.get(Calendar.DAY_OF_MONTH),
                        startDate.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault())
                ));

                if (!event.getStartTime().before(event.getEndTime())) {
                    Calendar endCal = event.getStartTime();
                    endCal.add(Calendar.HOUR_OF_DAY, 1);
                    event.setEndTime(endCal);

                    endDateText.setText(String.format(Locale.getDefault(),
                            "%d, %s %d, %s",
                            endCal.get(Calendar.YEAR),
                            endCal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()),
                            endCal.get(Calendar.DAY_OF_MONTH),
                            endCal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault())
                    ));
                    endTimeText.setText(String.format(Locale.getDefault(),
                            "%02d:%02d",
                            endCal.get(Calendar.HOUR_OF_DAY),
                            endCal.get(Calendar.MINUTE))
                    );
                }
            }
        };
        Calendar cal = event.getStartTime();
        DatePickerDialog dialog = new DatePickerDialog(this, listener, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    public void pickStartTimeClick(View view) {
        TimePickerDialog.OnTimeSetListener listener = new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker picker, int hourOfDay, int minute) {
                Calendar startTime = event.getStartTime();
                startTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                startTime.set(Calendar.MINUTE, minute);
                event.setStartTime(startTime);

                startTimeText.setText(String.format(Locale.getDefault(),
                        "%02d:%02d",
                        startTime.get(Calendar.HOUR_OF_DAY),
                        startTime.get(Calendar.MINUTE))
                );

                Calendar endCal = event.getStartTime();
                endCal.add(Calendar.HOUR_OF_DAY, 1);
                event.setEndTime(endCal);

                endDateText.setText(String.format(Locale.getDefault(),
                        "%d, %s %d, %s",
                        endCal.get(Calendar.YEAR),
                        endCal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()),
                        endCal.get(Calendar.DAY_OF_MONTH),
                        endCal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault())
                ));
                endTimeText.setText(String.format(Locale.getDefault(),
                        "%02d:%02d",
                        endCal.get(Calendar.HOUR_OF_DAY),
                        endCal.get(Calendar.MINUTE))
                );
            }
        };
        Calendar cal = event.getStartTime();
        TimePickerDialog dialog = new TimePickerDialog(this, listener, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false);
        dialog.show();
    }

    public void pickEndDateClick(View view) {
        DatePickerDialog.OnDateSetListener listener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker picker, int year, int month, int dayOfMonth) {
                Calendar endDate = event.getEndTime();
                endDate.set(year, month, dayOfMonth);
                event.setEndTime(endDate);

                endDateText.setText(String.format(Locale.getDefault(),
                        "%d, %s %d, %s",
                        endDate.get(Calendar.YEAR),
                        endDate.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()),
                        endDate.get(Calendar.DAY_OF_MONTH),
                        endDate.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault())
                ));
            }
        };
        Calendar cal = event.getEndTime();
        DatePickerDialog dialog = new DatePickerDialog(this, listener, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    public void pickEndTimeClick(View view) {
        TimePickerDialog.OnTimeSetListener listener = new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker picker, int hourOfDay, int minute) {
                Calendar endTime = event.getEndTime();
                endTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                endTime.set(Calendar.MINUTE, minute);
                event.setEndTime(endTime);

                endTimeText.setText(String.format(Locale.getDefault(),
                        "%02d:%02d",
                        endTime.get(Calendar.HOUR_OF_DAY),
                        endTime.get(Calendar.MINUTE))
                );
            }
        };

        Calendar cal = event.getEndTime();
        TimePickerDialog dialog = new TimePickerDialog(this, listener, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false);
        dialog.show();
    }

    public void clickCustomNotification(View view) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("dialogCustomNotif");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        DialogFragment dialogFragment = new CustomNotificationDialog();
        Bundle args = new Bundle();
        args.putBoolean("isEventNotification", true);
        dialogFragment.setArguments(args);
        dialogFragment.show(ft, "dialogCustomNotif");

    }

    public void setCustomNotifParams(int minutes) {
        event.setEventReminder(minutes);
        notificationValueText.setText(Tools.notifMinsToString(this, minutes));

        switch (minutes) {
            case 0:
                eventNotificationGroup.check(R.id.option_none);
                break;
            case -1440:
                eventNotificationGroup.check(R.id.option_day_before);
                break;
            case -60:
                eventNotificationGroup.check(R.id.option_hour_before);
                break;
            case -30:
                eventNotificationGroup.check(R.id.option_30mins_before);
                break;
            case -10:
                eventNotificationGroup.check(R.id.option_10mins_before);
                break;
            default:
                eventNotificationGroup.check(R.id.radio_btn_custom);
                customNotifRadioButton.setText(Tools.notifMinsToString(this, minutes));
                notificationValueText.setText(customNotifRadioButton.getText().toString());
                customNotifRadioButton.setVisibility(View.VISIBLE);
                break;
        }
    }

    public void initResultView() {

        activityTitle.setText(getString(R.string.result));
        tabEvaluation.setText(getString(R.string.re_evaluation));
        ViewGroup intervView = findViewById(R.id.result_details_interv);
        SeekBar expectedStressLevelSeek = findViewById(R.id.expected_stresslvl_seekbar);
        TextView intervName = findViewById(R.id.intervention_name);
        TextView expectedStressReason = findViewById(R.id.expected_strs_reason_text);
        final SeekBar realStressLevelSeek = findViewById(R.id.real_stresslvl_seekbar);
        final SeekBar intervEffectiveness = findViewById(R.id.intervention_effectiveness);
        final TextView realStressReason = findViewById(R.id.real_strs_reason_text);
        final TextView journalTxt = findViewById(R.id.journal_text);

        // set expected stress level from event variable
        expectedStressLevelSeek.setEnabled(false);
        expectedStressLevelSeek.setProgress(EventActivity.event.getStressLevel());
        int expectedStressColor = Tools.stressLevelToColor(getApplicationContext(), EventActivity.event.getStressLevel());
        expectedStressLevelSeek.getProgressDrawable().setColorFilter(expectedStressColor, PorterDuff.Mode.SRC_IN);
        expectedStressLevelSeek.getThumb().setColorFilter(expectedStressColor, PorterDuff.Mode.SRC_IN);

        expectedStressReason.setText(event.getStressCause());
        ((ViewGroup) expectedStressReason.getParent()).setVisibility(expectedStressReason.length() == 0 ? View.GONE : View.VISIBLE);

        if (!event.getIntervention().equals("")) {
            intervView.setVisibility(View.VISIBLE);
            intervName.setText(getResources().getString(R.string.current_interv_title, event.getIntervention()));
        } else {
            intervView.setVisibility(View.GONE);
        }

        if (Tools.isNetworkAvailable(this))
            Tools.execute(new MyRunnable(
                    this,
                    getString(R.string.url_evaluation_fetch, getString(R.string.server_ip)),
                    SignInActivity.loginPrefs.getString(SignInActivity.username, null)
            ) {
                @Override
                public void run() {
                    String url = (String) args[0];
                    String username = (String) args[1];

                    List<NameValuePair> params = new ArrayList<>();
                    try {
                        params.add(new BasicNameValuePair("username", username));
                        params.add(new BasicNameValuePair("eventId", String.valueOf(event.getEventId())));

                        JSONObject res = new JSONObject(Tools.post(url, params));
                        switch (res.getInt("result")) {
                            case Tools.RES_OK:
                                JSONObject eventEval = res.getJSONObject("evaluation");
                                runOnUiThread(new MyRunnable(
                                        activity,
                                        eventEval.get("realStressLevel"),
                                        eventEval.get("intervEffectiveness"),
                                        eventEval.get("realStressCause"),
                                        eventEval.get("journal")

                                ) {
                                    @Override
                                    public void run() {
                                        int realStressLevel = (int) args[0];
                                        int interventionEffectiveness = (int) args[1];
                                        String realStressCause = (String) args[2];
                                        String journalString = (String) args[3];

                                        // set real stress level from evaluation
                                        realStressLevelSeek.setEnabled(false);
                                        realStressLevelSeek.setProgress(realStressLevel);
                                        realStressLevelSeek.getProgressDrawable().setColorFilter(Tools.stressLevelToColor(getApplicationContext(), realStressLevel), PorterDuff.Mode.SRC_IN);
                                        realStressLevelSeek.getThumb().setColorFilter(Tools.stressLevelToColor(getApplicationContext(), realStressLevel), PorterDuff.Mode.SRC_IN);

                                        intervEffectiveness.setEnabled(false);
                                        intervEffectiveness.setProgress(interventionEffectiveness);

                                        realStressReason.setText(realStressCause);
                                        ((ViewGroup) realStressReason.getParent()).setVisibility(realStressReason.length() == 0 ? View.GONE : View.VISIBLE);
                                        journalTxt.setText(journalString);
                                        journalTxt.setVisibility(journalTxt.length() == 0 ? View.GONE : View.VISIBLE);


                                    }
                                });
                                break;
                            case Tools.RES_FAIL:
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(EventActivity.this, "Failed to fetch evaluation for this event.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                                break;
                            case Tools.RES_SRV_ERR:
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(EventActivity.this, "Failure occurred while processing the request. (SERVER SIDE)", Toast.LENGTH_SHORT).show();
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
                                Toast.makeText(EventActivity.this, "Failed to proceed due to an error in connection with server.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    enableTouch();
                }
            });
        else {
            Toast.makeText(this, "Please connect to a network first!", Toast.LENGTH_SHORT).show();
        }


    }
}

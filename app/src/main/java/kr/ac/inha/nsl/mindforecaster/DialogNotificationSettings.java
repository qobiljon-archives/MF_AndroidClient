package kr.ac.inha.nsl.mindforecaster;

import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.TimePicker;

import java.util.Calendar;
import java.util.Locale;

public class DialogNotificationSettings extends DialogFragment {

    public DialogNotificationSettings() {
    }

    //region Variables
    static Calendar sunday = Calendar.getInstance(Locale.getDefault()), everyMorning = Calendar.getInstance(Locale.getDefault()), everyEvening = Calendar.getInstance(Locale.getDefault());
    static Calendar sundayPrev = Calendar.getInstance(Locale.getDefault()), everyMorningPrev = Calendar.getInstance(Locale.getDefault()), everyEveningPrev = Calendar.getInstance(Locale.getDefault());

    ViewGroup root;
    TextView sundayTxt, everyMorningTxt, everyEveningTxt;
    //endregion

    // region Override
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        root = (ViewGroup) inflater.inflate(R.layout.dialog_notif_settings, container, true);
        init();
        return root;
    }
    // endregion

    private void init() {
        sundayTxt = root.findViewById(R.id.txt_sunday_time);
        everyMorningTxt = root.findViewById(R.id.txt_everymorning_time);
        everyEveningTxt = root.findViewById(R.id.txt_everyevening_time);

        sunday.setTimeInMillis(ActivitySignIn.loginPrefs.getLong("SundayReminderTime", 0));
        everyMorning.setTimeInMillis(ActivitySignIn.loginPrefs.getLong("EveryMorningReminderTime", 0));
        everyEvening.setTimeInMillis(ActivitySignIn.loginPrefs.getLong("EveryEveningReminderTime", 0));

        sundayPrev = (Calendar) sunday.clone();
        everyMorningPrev = (Calendar) everyMorningPrev.clone();
        everyEveningPrev = (Calendar) everyEveningPrev.clone();

        sundayTxt.setTag(ActivitySignIn.loginPrefs.getLong("SundayReminderTime", 0));
        everyMorningTxt.setTag(ActivitySignIn.loginPrefs.getLong("EveryMorningReminderTime", 0));
        everyEveningTxt.setTag(ActivitySignIn.loginPrefs.getLong("EveryEveningReminderTime", 0));

        sundayTxt.setText(String.format(Locale.getDefault(),
                "%02d:%02d",
                sunday.get(Calendar.HOUR_OF_DAY),
                sunday.get(Calendar.MINUTE))
        );

        everyMorningTxt.setText(String.format(Locale.getDefault(),
                "%02d:%02d",
                everyMorning.get(Calendar.HOUR_OF_DAY),
                everyMorning.get(Calendar.MINUTE))
        );

        everyEveningTxt.setText(String.format(Locale.getDefault(),
                "%02d:%02d",
                everyEvening.get(Calendar.HOUR_OF_DAY),
                everyEvening.get(Calendar.MINUTE))
        );

        root.findViewById(R.id.txt_sunday_time).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickTimeClick(v.findViewById(R.id.txt_sunday_time));
            }
        });

        root.findViewById(R.id.txt_everymorning_time).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickTimeClick(v.findViewById(R.id.txt_everymorning_time));
            }
        });

        root.findViewById(R.id.txt_everyevening_time).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickTimeClick(v.findViewById(R.id.txt_everyevening_time));
            }
        });

        root.findViewById(R.id.btn_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deletePrevNotifications(sundayPrev, everyMorningPrev, everyEveningPrev);
                Tools.addSundayNotification(getActivity(), sunday);
                Tools.addDailyNotification(getActivity(), everyMorning, getString(R.string.daily_notif_question), false);
                Tools.addDailyNotification(getActivity(), everyEvening, getString(R.string.daily_notif_request), true);
                SharedPreferences.Editor editor = ActivitySignIn.loginPrefs.edit();
                editor.putLong("SundayReminderTime", sunday.getTimeInMillis());
                editor.putLong("EveryMorningReminderTime", everyMorning.getTimeInMillis());
                editor.putLong("EveryEveningReminderTime", everyEvening.getTimeInMillis());
                editor.apply();
                dismiss();
            }
        });

        root.findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }

    public void pickTimeClick(View view) {

        MyOnTimeSetListener listener = new MyOnTimeSetListener(view) {
            @Override
            public void onTimeSet(TimePicker picker, int hourOfDay, int minute) {
                Calendar cal = Calendar.getInstance(Locale.getDefault());
                cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
                cal.set(Calendar.MINUTE, minute);

                view.setTag(cal.getTimeInMillis());
                ((TextView) view).setText(String.format(Locale.getDefault(),
                        "%02d:%02d",
                        hourOfDay,
                        minute)
                );

                switch (view.getId()) {
                    case R.id.txt_sunday_time:
                        sunday.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        sunday.set(Calendar.MINUTE, minute);
                        sunday.set(Calendar.SECOND, 0);
                        sunday.set(Calendar.MILLISECOND, 0);
                        sunday.set(Calendar.DAY_OF_WEEK, 1);
                        break;
                    case R.id.txt_everymorning_time:
                        everyMorning.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        everyMorning.set(Calendar.MINUTE, minute);
                        everyMorning.set(Calendar.SECOND, 0);
                        everyMorning.set(Calendar.MILLISECOND, 0);
                        break;
                    case R.id.txt_everyevening_time:
                        everyEvening.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        everyEvening.set(Calendar.MINUTE, minute);
                        everyEvening.set(Calendar.SECOND, 0);
                        everyEvening.set(Calendar.MILLISECOND, 0);
                        break;
                    default:
                        break;
                }
            }
        };
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.setTimeInMillis((long) view.getTag());
        TimePickerDialog dialog = new TimePickerDialog(getActivity(), listener, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true);
        dialog.show();
    }

    private void deletePrevNotifications(Calendar sunday, Calendar morning, Calendar evening) {
        Tools.cancelNotification(getActivity(), (int) sunday.getTimeInMillis());
        Tools.cancelNotification(getActivity(), (int) morning.getTimeInMillis());
        Tools.cancelNotification(getActivity(), (int) evening.getTimeInMillis());
    }

    private abstract class MyOnTimeSetListener implements TimePickerDialog.OnTimeSetListener {
        View view;

        MyOnTimeSetListener(View view) {
            this.view = view;
        }
    }
}

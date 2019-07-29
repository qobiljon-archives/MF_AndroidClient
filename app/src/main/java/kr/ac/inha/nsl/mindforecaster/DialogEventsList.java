package kr.ac.inha.nsl.mindforecaster;

import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class DialogEventsList extends DialogFragment {

    public DialogEventsList() {
        onEventClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), ActivityEvent.class);
                intent.putExtra("eventId", (long) view.getTag());
                startActivityForResult(intent, ActivityMain.EVENT_ACTIVITY);
            }
        };
    }

    private View.OnClickListener onEventClickListener;
    // endregion

    // region Override
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.dialog_daily_eventlist, container, true);
        init(root);
        return root;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (getActivity() instanceof ActivityMain)
            ((ActivityMain) getActivity()).updateCalendarView();
        dismiss();
        super.onActivityResult(requestCode, resultCode, data);
    }
    // endregion

    private void init(ViewGroup root) {
        // region Variables
        root.findViewById(R.id.btn_add_from_dialog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), ActivityEvent.class);
                intent.putExtra("selectedDayMillis", getArguments().getLong("selectedDayMillis"));
                startActivityForResult(intent, 0);
                getActivity().overridePendingTransition(R.anim.activity_in, R.anim.activity_out);
            }
        });

        Calendar selectedDay = Calendar.getInstance(Locale.getDefault());
        selectedDay.setTimeInMillis(getArguments().getLong("selectedDayMillis"));

        TextView dateTxt = root.findViewById(R.id.cell_date);

        dateTxt.setText(String.format(Locale.getDefault(),
                "%02d, %s %02d, %s",
                selectedDay.get(Calendar.YEAR),
                selectedDay.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()),
                selectedDay.get(Calendar.DAY_OF_MONTH),
                selectedDay.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault())
        ));

        ViewGroup elementsLinearLayout = root.findViewById(R.id.daily_elements_linear_layout);
        ArrayList<Event> dayEvents = Event.getOneDayEvents(selectedDay);
        for (Event event : dayEvents) {
            getActivity().getLayoutInflater().inflate(R.layout.event_element_dailyview, elementsLinearLayout);
            ViewGroup view = (ViewGroup) elementsLinearLayout.getChildAt(elementsLinearLayout.getChildCount() - 1);
            view.setTag(event.getEventId());
            view.setOnClickListener(onEventClickListener);

            TextView eventTitleTextView = view.findViewById(R.id.text_event_title);
            TextView eventPeriodTextView = view.findViewById(R.id.text_event_period);
            TextView evaluationDoneTextView = view.findViewById(R.id.text_evaluation_done);
            TextView stressLevelTextView = view.findViewById(R.id.text_stress_level);

            if (selectedDay.before(Calendar.getInstance(Locale.getDefault()))) {
                if (event.isEvaluated()) {
                    evaluationDoneTextView.setText(getString(R.string.evaluated));
                    stressLevelTextView.setBackgroundColor(Tools.stressLevelToColor(getActivity(), event.getRealStressLevel()));
                    stressLevelTextView.setText(event.getRealStressLevel() == -1 ? "N/A" : String.valueOf(event.getRealStressLevel()));
                } else {
                    evaluationDoneTextView.setText(getString(R.string.not_evaluated));
                    stressLevelTextView.setBackgroundColor(Tools.stressLevelToColor(getActivity(), event.getStressLevel()));
                    stressLevelTextView.setText(event.getStressLevel() == -1 ? "N/A" : String.valueOf(event.getStressLevel()));
                }
            } else {
                evaluationDoneTextView.setVisibility(View.GONE);
                stressLevelTextView.setBackgroundColor(Tools.stressLevelToColor(getActivity(), event.getStressLevel()));
                stressLevelTextView.setText(event.getStressLevel() == -1 ? "N/A" : String.valueOf(event.getStressLevel()));
            }
            eventTitleTextView.setText(event.getTitle());
            eventPeriodTextView.setText(String.format(Locale.getDefault(),
                    "%02d:%02d - %02d:%02d",
                    event.getStartTime().get(Calendar.HOUR_OF_DAY),
                    event.getStartTime().get(Calendar.MINUTE),
                    event.getEndTime().get(Calendar.HOUR_OF_DAY),
                    event.getEndTime().get(Calendar.MINUTE))
            );
        }

        //Inflating a "Close" button to the end of dialog
        getActivity().getLayoutInflater().inflate(R.layout.button_close, elementsLinearLayout);
        Button btnClose = elementsLinearLayout.findViewById(R.id.close_button);
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }
}

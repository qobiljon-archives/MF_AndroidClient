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

public class EventsListDialog extends DialogFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        root = (ViewGroup) inflater.inflate(R.layout.dialog_daily_eventlist, container, true);
        init();
        return root;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (getActivity() instanceof MainActivity)
            ((MainActivity) getActivity()).updateCalendarView();
        dismiss();
        super.onActivityResult(requestCode, resultCode, data);
    }

    private ViewGroup root;

    private View.OnClickListener onEventClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent intent = new Intent(getActivity(), EventActivity.class);
            intent.putExtra("eventId", (long) view.getTag());
            startActivityForResult(intent, MainActivity.EVENT_ACTIVITY);
        }
    };

    public EventsListDialog() {

    }

    private void init() {
        root.findViewById(R.id.btn_add_from_dialog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), EventActivity.class);
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

        ArrayList<Event> dayEvents = Event.getOneDayEvents(selectedDay);
        for (Event event : dayEvents) {
            getActivity().getLayoutInflater().inflate(R.layout.event_element_dailyview, root);
            ViewGroup view = (ViewGroup) root.getChildAt(root.getChildCount() - 1);
            view.setTag(event.getEventId());
            view.setOnClickListener(onEventClickListener);
            TextView titleText = view.findViewById(R.id.event_title_text_view);
            TextView dateText = view.findViewById(R.id.event_date_text_view);
            TextView isEvaluated = view.findViewById(R.id.is_evaluated);
            TextView stressLevel = view.findViewById(R.id.stress_lvl_box);

            if (selectedDay.before(Calendar.getInstance(Locale.getDefault()))) {
                if (event.isEvaluated()) {
                    isEvaluated.setText(getString(R.string.evaluated));
                    stressLevel.setBackgroundColor(Tools.stressLevelToColor(getActivity(), event.getRealStressLevel()));
                    stressLevel.setText(String.valueOf(event.getRealStressLevel()));
                } else {
                    isEvaluated.setText(getString(R.string.not_evaluated));
                    stressLevel.setBackgroundColor(Tools.stressLevelToColor(getActivity(), event.getStressLevel()));
                    stressLevel.setText(String.valueOf(event.getStressLevel()));
                }
            } else {
                isEvaluated.setVisibility(View.GONE);
                stressLevel.setBackgroundColor(Tools.stressLevelToColor(getActivity(), event.getStressLevel()));
                stressLevel.setText(String.valueOf(event.getStressLevel()));
            }


            titleText.setText(event.getTitle());

            dateText.setText(String.format(Locale.getDefault(),
                    "%02d:%02d - %02d:%02d",
                    event.getStartTime().get(Calendar.HOUR_OF_DAY),
                    event.getStartTime().get(Calendar.MINUTE),
                    event.getEndTime().get(Calendar.HOUR_OF_DAY),
                    event.getEndTime().get(Calendar.MINUTE))
            );
        }

        //Inflating a "Close" button to the end of dialog
        getActivity().getLayoutInflater().inflate(R.layout.button_close, root);
        Button btnClose = root.findViewById(R.id.btn_close);
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }
}

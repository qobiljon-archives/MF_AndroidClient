package kr.ac.inha.nsl.mindforecaster;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ActivityDialogInterventionSuggestion extends AppCompatActivity {
    // region Override
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_intervention_suggestion);

        Intent intent = getIntent();
        String interventionReminderTitle = intent.getStringExtra("intervention_reminder_title");
        String interventionReminderDescription = intent.getStringExtra("intervention_reminder_description");
        String interventionReminderEventPeriod = intent.getStringExtra("intervention_reminder_event_period");

        TextView titleText = findViewById(R.id.intervention_title_text);
        TextView descriptionText = findViewById(R.id.intervention_description_text);
        TextView eventPeriodText = findViewById(R.id.event_period_text);

        titleText.setText(interventionReminderTitle);
        descriptionText.setText(interventionReminderDescription);
        eventPeriodText.setText(interventionReminderEventPeriod);
    }
    // endregion

    public void confirm_dialog_button(View view) {
        finish();
        overridePendingTransition(R.anim.activity_in_reverse, R.anim.activity_out_reverse);
    }
}

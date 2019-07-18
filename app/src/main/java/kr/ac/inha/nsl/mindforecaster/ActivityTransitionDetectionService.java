package kr.ac.inha.nsl.mindforecaster;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionEvent;
import com.google.android.gms.location.ActivityTransitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.io.IOException;
import java.util.Locale;

public class ActivityTransitionDetectionService extends IntentService {
    @SuppressWarnings("unused")
    public ActivityTransitionDetectionService() {
        super("ActivityTransitionDetectionService");
    }

    @SuppressWarnings("unused")
    public ActivityTransitionDetectionService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            if (ActivityTransitionResult.hasResult(intent)) {
                ActivityTransitionResult result = ActivityTransitionResult.extractResult(intent);
                assert result != null;
                for (ActivityTransitionEvent event : result.getTransitionEvents()) {
                    String activity;
                    String transition;

                    // extract activity type
                    switch (event.getActivityType()) {
                        case DetectedActivity.STILL:
                            activity = "STILL";
                            break;
                        case DetectedActivity.WALKING:
                            activity = "WALKING";
                            break;
                        case DetectedActivity.RUNNING:
                            activity = "RUNNING";
                            break;
                        case DetectedActivity.ON_BICYCLE:
                            activity = "ON_BICYCLE";
                            break;
                        case DetectedActivity.IN_VEHICLE:
                            activity = "IN_VEHICLE";
                            break;
                        default:
                            activity = "N/A";
                            break;
                    }

                    // extract transition type
                    switch (event.getTransitionType()) {
                        case ActivityTransition.ACTIVITY_TRANSITION_ENTER:
                            transition = "ENTER";
                            break;
                        case ActivityTransition.ACTIVITY_TRANSITION_EXIT:
                            transition = "EXIT";
                            break;
                        default:
                            transition = "N/A";
                            break;
                    }

                    long timestamp = Tools.LAST_REBOOT_TIMESTAMP + event.getElapsedRealTimeNanos() / 1000000;
                    try {
                        Tools.storeActivityRecognitionData(timestamp, activity, transition);
                        Log.e("ACTIVITY UPDATE", String.format(Locale.getDefault(), "(Activity,Transition)=(%s, %s)", activity, transition));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}

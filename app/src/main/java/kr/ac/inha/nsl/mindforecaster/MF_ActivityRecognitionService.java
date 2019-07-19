package kr.ac.inha.nsl.mindforecaster;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.io.IOException;
import java.util.Locale;

@SuppressWarnings("unused")
public class MF_ActivityRecognitionService extends IntentService {
    public MF_ActivityRecognitionService() {
        super("MF_ActivityRecognitionService");
    }

    public MF_ActivityRecognitionService(String name) {
        super(name);
    }

    // region Override
    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            if (ActivityRecognitionResult.hasResult(intent)) {
                ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
                assert result != null;

                DetectedActivity detectedActivity = result.getMostProbableActivity();
                String activity;
                float confidence = ((float) detectedActivity.getConfidence()) / 100;

                // extract activity type
                switch (detectedActivity.getType()) {
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
                    case DetectedActivity.ON_FOOT:
                        activity = "ON_FOOT";
                        break;
                    case DetectedActivity.TILTING:
                        activity = "TILTING";
                        break;
                    case DetectedActivity.UNKNOWN:
                        activity = "UNKNOWN";
                        break;
                    default:
                        activity = "N/A";
                        break;
                }

                try {
                    Tools.storeActivityRecognitionData(result.getTime(), activity, confidence);
                    Tools.checkAndSendActivityData();
                    Tools.checkAndSendUsageAccessStats();
                    Log.e("ACTIVITY UPDATE", String.format(Locale.getDefault(), "(Activity,Confidence)=(%s, %.3f)", activity, confidence));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    // endregion
}

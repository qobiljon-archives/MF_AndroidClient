package kr.ac.inha.nsl.mindforecaster;

import android.Manifest;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionEvent;
import com.google.android.gms.location.ActivityTransitionRequest;
import com.google.android.gms.location.ActivityTransitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class DataCollectorService extends Service {

    // region Variables
    static boolean forceClose = false;
    static boolean isServiceRunning = false;

    private final IBinder mBinder = new LocalBinder();
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Location previouslyStoredLocation;

    private ActivityRecognitionClient activityRecognitionClient;
    private ActivityTransitionRequest activityTransitionRequest;
    private PendingIntent transitionPendingIntent;
    // endregion

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        Log.e("DataCollectorService", "onCreate");

        // set up location callbacks
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }

                try {
                    Tools.locationDataFile = new File(getApplicationContext().getFilesDir(), "mf-locs.txt");
                    boolean fileAvailable = Tools.locationDataFile.exists() || Tools.locationDataFile.createNewFile();

                    if (!fileAvailable) {
                        Tools.locationDataFile = null;
                        return;
                    }

                    for (Location location : locationResult.getLocations()) {
                        if (previouslyStoredLocation != null && previouslyStoredLocation.equals(location))
                            continue;

                        long timestamp = Tools.LAST_REBOOT_TIMESTAMP + location.getElapsedRealtimeNanos() / 1000000;
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        double altitude = location.getAltitude();

                        Tools.storeLocationData(timestamp, latitude, longitude, altitude);
                        Log.e("LOCATION UPDATE", String.format(Locale.getDefault(), "(ts, lat, lon, alt)=(%d, %f, %f, %f)", timestamp, latitude, longitude, altitude));

                        if (previouslyStoredLocation == null)
                            previouslyStoredLocation = location;
                    }
                    if (Tools.isNetworkAvailable()) {
                        Tools.checkAndSendLocationData();
                        Tools.checkAndSendUsageAccessStats();
                        Tools.checkAndSendActivityData();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        // set up activity-transition tracker
        List<ActivityTransition> transitions = new ArrayList<>();
        Collections.addAll(
                transitions,
                new ActivityTransition.Builder().setActivityType(DetectedActivity.STILL).setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER).build(),
                new ActivityTransition.Builder().setActivityType(DetectedActivity.STILL).setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT).build(),
                new ActivityTransition.Builder().setActivityType(DetectedActivity.WALKING).setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER).build(),
                new ActivityTransition.Builder().setActivityType(DetectedActivity.WALKING).setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT).build(),
                new ActivityTransition.Builder().setActivityType(DetectedActivity.RUNNING).setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER).build(),
                new ActivityTransition.Builder().setActivityType(DetectedActivity.RUNNING).setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT).build(),
                new ActivityTransition.Builder().setActivityType(DetectedActivity.ON_BICYCLE).setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER).build(),
                new ActivityTransition.Builder().setActivityType(DetectedActivity.ON_BICYCLE).setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT).build(),
                new ActivityTransition.Builder().setActivityType(DetectedActivity.IN_VEHICLE).setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER).build(),
                new ActivityTransition.Builder().setActivityType(DetectedActivity.IN_VEHICLE).setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT).build()
        );
        Log.e("TRANSITIONS", transitions.size() + " overall");
        activityTransitionRequest = new ActivityTransitionRequest(transitions);

        activityRecognitionClient = ActivityRecognition.getClient(getApplicationContext());
        transitionPendingIntent = PendingIntent.getService(
                getApplicationContext(),
                2,
                new Intent(getApplicationContext(), ActivityTransitionDetectionService.class),
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        Log.e("DataCollectorService", "onDestroy");

        isServiceRunning = false;
        fusedLocationClient.removeLocationUpdates(locationCallback);
        activityRecognitionClient.removeActivityUpdates(transitionPendingIntent);

        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("DataCollectorService", "onStartCommand");

        if (!isServiceRunning && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationRequest locationRequest = new LocationRequest();
            locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
            locationRequest.setFastestInterval(10000); // fastest ==> 10 seconds
            locationRequest.setInterval(600000); // slowest ==> 10 minutes
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
            Log.e("DataCollectorService", "Location data collection started...");

            activityRecognitionClient.requestActivityTransitionUpdates(activityTransitionRequest, transitionPendingIntent).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    try {
                        Tools.activityRecognitionDataFile = new File(getApplicationContext().getFilesDir(), "mf-activity-recognition.txt");

                        boolean fileAvailable = Tools.activityRecognitionDataFile.exists() || Tools.activityRecognitionDataFile.createNewFile();
                        if (fileAvailable)
                            Toast.makeText(getApplicationContext(), "Activity tracking has successfully started!", Toast.LENGTH_LONG).show();
                        else {
                            Tools.locationDataFile = null;
                            Toast.makeText(getApplicationContext(), "Failed to start tracking activity. Please refer to the developer!", Toast.LENGTH_LONG).show();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(DataCollectorService.this, "Unable to start activity detection. Please kindly report this case to the developer of this application!", Toast.LENGTH_SHORT).show();
                }
            });
            Log.e("DataCollectorService", "Activity data collection started...");
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        if (forceClose) {
            forceClose = false;
            super.onTaskRemoved(rootIntent);
        } else {
            // Toast.makeText(this, "Restarting data collector service!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getApplicationContext(), DataCollectorService.class);
            intent.setPackage(Tools.PACKAGE_NAME);
            PendingIntent restartServicePendingIntent = PendingIntent.getService(getApplicationContext(), 1, intent, PendingIntent.FLAG_ONE_SHOT);

            AlarmManager alarmService = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
            alarmService.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 100, restartServicePendingIntent);
        }
    }

    private class LocalBinder extends Binder {
        //DataCollectorService getService() {
        //    return DataCollectorService.this;
        //}
    }

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
}

package kr.ac.inha.nsl.mindforecaster;

import android.location.Location;
import android.util.Log;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;

import java.io.IOException;
import java.util.Locale;

public class MF_LocationCallback extends LocationCallback {
    // region Variables
    private Location previouslyStoredLocation;
    // endregion

    // region Override
    @Override
    public void onLocationResult(LocationResult locationResult) {
        if (locationResult == null) {
            return;
        }

        try {
            for (Location location : locationResult.getLocations()) {
                if (previouslyStoredLocation != null && previouslyStoredLocation.equals(location))
                    continue;

                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                double altitude = location.getAltitude();

                Tools.storeLocationData(location.getTime(), latitude, longitude, altitude);
                Tools.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Tools.checkAndSendLocationData();
                            Tools.checkAndSendUsageAccessStats();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
                //Log.e("LOCATION UPDATE", String.format(Locale.getDefault(), "(ts, lat, lon, alt)=(%d, %f, %f, %f)", location.getTime(), latitude, longitude, altitude));

                if (previouslyStoredLocation == null)
                    previouslyStoredLocation = location;
            }
            Tools.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (Tools.isNetworkAvailable()) {
                            Tools.checkAndSendLocationData();
                            Tools.checkAndSendUsageAccessStats();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // endregion
}

package ir.map.tracker;

import android.content.Context;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

class LocationHelper {
    static LocationRequest getLocationRequest(int interval) {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(interval);
        locationRequest.setFastestInterval(interval);
        return locationRequest;
    }

    static FusedLocationProviderClient getLocationClient(Context context) {
        return LocationServices.getFusedLocationProviderClient(context);
    }
}

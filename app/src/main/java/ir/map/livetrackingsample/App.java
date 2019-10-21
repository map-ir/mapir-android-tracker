package ir.map.livetrackingsample;

import android.app.Application;

import com.mapbox.mapboxsdk.Mapbox;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Mapbox.getInstance(getApplicationContext(), null);
    }
}

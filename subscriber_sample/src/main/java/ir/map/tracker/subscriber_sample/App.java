package ir.map.tracker.subscriber_sample;

import android.app.Application;

import ir.map.sdk_map.Mapir;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Mapir.getInstance(this, "API_KEY");
    }
}

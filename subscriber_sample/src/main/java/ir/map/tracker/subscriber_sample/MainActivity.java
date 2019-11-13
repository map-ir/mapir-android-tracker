package ir.map.tracker.subscriber_sample;

import android.location.Location;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import ir.map.tracker.Subscriber;
import ir.map.tracker.SubscriberError;
import ir.map.tracker.TrackerEvent;

public class MainActivity extends AppCompatActivity {

    private Subscriber subscriber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        subscriber = Subscriber.getLiveTracker(getBaseContext(), "API_KEY", "track_id", new TrackerEvent.SubscribeListener() {
            @Override
            public void onLocationReceived(Location location) {
                Toast.makeText(MainActivity.this, "Latitude : " + location.getLatitude() + "\n" +
                                "Longitude : " + location.getLongitude()
                        , Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLiveTrackerDisconnected() {
                Toast.makeText(MainActivity.this, "Tracker Disconnected", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(SubscriberError error) {
                switch (error) {
                    case TELEPHONY_PERMISSION: // Telephony permission required

                        break;
                    case INITIALIZE_ERROR: // Initialize Error

                        break;
                    case API_KEY_NOT_AVAILABLE: // API_KEY not available

                        break;
                }
            }
        });

        subscriber.start();
    }
}

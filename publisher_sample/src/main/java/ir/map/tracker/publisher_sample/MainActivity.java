package ir.map.tracker.publisher_sample;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Objects;

import ir.map.tracker.Publisher;
import ir.map.tracker.PublisherError;
import ir.map.tracker.TrackerEvent;

public class MainActivity extends AppCompatActivity implements TrackerEvent.PublishListener {

    private final int REQUEST_PERMISSION_ACCESS_FINE_LOCATION = 1000;
    private final int REQUEST_PERMISSION_READ_PHONE_STATE = 1001;

    private String defaultTopic = "shiveh_live_tracking_test";
    private int countReceive = 1;

    private Publisher publisher;

    private EditText liveTrackingIdEdt;
    private EditText liveTrackingLogEdt;
    private ImageView playPauseImg;

    private boolean playPause = false; //true: play and false: pause

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        try {
            Objects.requireNonNull(this.getSupportActionBar()).hide();
        } catch (NullPointerException ignored) {
        }

        setContentView(R.layout.activity_main);

        liveTrackingIdEdt = findViewById(R.id.live_tracking_id_edt);
        liveTrackingLogEdt = findViewById(R.id.live_tracking_log_edt);

        playPauseImg = findViewById(R.id.play_pause_img);

        playPauseImg.setOnClickListener(v -> {
            hideKeyboard();

            if (permissionsGranted())
                if (playPause) {
                    liveTrackingIdEdt.setEnabled(true);

                    if (publisher != null)
                        publisher.stop();

                    Toast.makeText(this, "Sending location stopped.", Toast.LENGTH_LONG).show();
                    liveTrackingLogEdt.setText("STATUS would logged here");

                    playPause = false;
                    playPauseImg.setImageResource(R.drawable.ic_start);
                    countReceive = 1;
                } else {
                    liveTrackingIdEdt.setEnabled(false);

                    if (liveTrackingIdEdt.getText().toString().isEmpty())
                        setupPublisher(defaultTopic);
                    else
                        setupPublisher(liveTrackingIdEdt.getText().toString());

                    liveTrackingLogEdt.setText("Sending location started.");

                    playPause = true;
                    playPauseImg.setImageResource(R.drawable.ic_stop);
                }
        });
    }

    @Override
    public void onFailure(PublisherError error) {
        switch (error) {
            case INITIALIZE_ERROR:
                liveTrackingLogEdt.setText("INITIALIZE_ERROR");
                break;
            case LOCATION_PERMISSION:
                liveTrackingLogEdt.setText("LOCATION_PERMISSION");
                break;
            case TELEPHONY_PERMISSION:
                liveTrackingLogEdt.setText("TELEPHONY_PERMISSION");
                break;
            case MISSING_API_KEY:
                liveTrackingLogEdt.setText("Missing apikey");
                break;
            case MISSING_TRACK_ID:
                liveTrackingLogEdt.setText("Missing track_id");
                break;
            case MISSING_CONTEXT:
                liveTrackingLogEdt.setText("Missing context");
                break;
        }
    }

    @Override
    public void publishedLocation(Location location) {
        liveTrackingLogEdt.setText("N" + countReceive + "; Lat: " + location.getLatitude() + ", Lon: " + location.getLongitude());
        countReceive++;

        Log.i("TRACKING711", "N" + countReceive + "; Lat: " + location.getLatitude() + ", Lon: " + location.getLongitude());
    }

    @Override
    public void onLiveTrackerDisconnected() {
        liveTrackingLogEdt.setText("Live Tracker Disconnected");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                }
            }
            case REQUEST_PERMISSION_READ_PHONE_STATE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                }
            }
        }
    }

    @Override
    protected void onPause() {
        if (publisher != null)
            publisher.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        if (publisher != null)
            publisher.onResume();
        super.onResume();
    }

    @SuppressLint({"MissingPermission"})
    private void setupPublisher(String topic) {
        if (permissionsGranted()) {
            if (publisher != null)
                publisher.stop();

            publisher = Publisher.getLiveTracker(
                    getBaseContext(),
                    getString(R.string.api_key), topic,
                    true,
                    this);

            // Start publish
            if (publisher != null)
                publisher.start(1000);
        }
    }

    private boolean permissionsGranted() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSION_ACCESS_FINE_LOCATION);
            return false;
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.READ_PHONE_STATE},
                    REQUEST_PERMISSION_READ_PHONE_STATE);
            return false;
        }

        return true;
    }

    public void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null)
            view = new View(this);

        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}

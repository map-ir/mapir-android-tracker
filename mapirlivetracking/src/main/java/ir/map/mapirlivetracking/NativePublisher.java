package ir.map.mapirlivetracking;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.TelephonyManager;

import androidx.core.content.ContextCompat;

import ir.map.mapirlivetracking.network.MapirRegistrationResponseListener;
import ir.map.mapirlivetracking.network.model.Register;

import static ir.map.mapirlivetracking.LiveTrackerError.INITIALIZE_ERROR;
import static ir.map.mapirlivetracking.LiveTrackerError.LOCATION_PERMISSION;

class NativePublisher implements MapirRegistrationResponseListener {

    private Context context;
    private String xApiKey;
    private String trackId;
    private boolean isTrackerReady = false;
    private boolean shouldRunInBackground = false;
    private String deviceId;
    private int interval = 10000;
    private TrackerEvent.PublishListener trackerPublishListener;
    private boolean shouldRestart = true;
    private boolean shouldStart = false;
    private Register register;

    NativePublisher(Context context, String xApiKey, String trackId, boolean shouldRunInBackground, TrackerEvent.PublishListener trackerPublishListener) {
        this.context = context;
        this.xApiKey = xApiKey;
        this.trackId = trackId;
        this.shouldRunInBackground = shouldRunInBackground;
        this.trackerPublishListener = trackerPublishListener;
        init();
    }

    @SuppressLint({"MissingPermission", "HardwareIds"})
    private void init() {
        deviceId = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
        new Services().registerClient(xApiKey, deviceId, trackId, this);
    }

    @Override
    public void onRegisterSuccess(Register register) {
        this.register = register;
        isTrackerReady = true;
        if (shouldStart) {
            startService(this.register);
        }
    }

    @Override
    public void onRegisterFailure(Throwable error) {
        trackerPublishListener.onFailure(INITIALIZE_ERROR);
    }

    boolean isTrackerReady() {
        return isTrackerReady;
    }

    void start(int interval) {
        shouldStart = true;
        shouldRestart = true;
        this.interval = interval;
        if (Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            trackerPublishListener.onFailure(LOCATION_PERMISSION);
        } else {
            shouldStart = true;
            if (isTrackerReady && register != null) {
                startService(this.register);
            }
        }
    }

    void stop() {
        shouldRestart = false;
        stopService();
    }

    private void startService(Register register) {
        stopService();
        Intent intent = new Intent(context, PublisherService.class);
        intent.putExtra("background_running", shouldRunInBackground);
        intent.putExtra("interval", interval);
        intent.putExtra("topic", register.getData().getTopic());
        intent.putExtra("restart", shouldRestart);
        context.startService(intent);
    }

    private void stopService() {
        context.stopService(new Intent(context, PublisherService.class));
    }
}

package ir.map.mapirlivetracking;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;

import static ir.map.mapirlivetracking.Constants.BROADCAST_ERROR_ACTION_NAME;
import static ir.map.mapirlivetracking.Constants.BROADCAST_INFO_ACTION_NAME;
import static ir.map.mapirlivetracking.LiveTrackerError.ACCESS_TOKEN_NOT_AVAILABLE;

public class Publisher {

    private boolean shouldRestart = true;
    private boolean shouldRunInBackground = false;
    private int interval = 10000;

    private NativePublisher nativePublisher;

    private Publisher(Context context, @NonNull String xApiKey, @NonNull String trackId, boolean shouldRunInBackground, TrackerEvent.PublishListener trackerPublishListener) {
        this.shouldRunInBackground = shouldRunInBackground;
        context.registerReceiver(new ReceiverCall(),new IntentFilter(BROADCAST_INFO_ACTION_NAME));
        nativePublisher = new NativePublisher(context, xApiKey, trackId, shouldRunInBackground, trackerPublishListener);
    }

    @SuppressLint("HardwareIds")
    @RequiresPermission(allOf = {Manifest.permission.READ_PHONE_STATE, Manifest.permission.ACCESS_FINE_LOCATION})
    public static Publisher getLiveTracker(@NonNull Context context,@NonNull String token,@NonNull String trackId, boolean shouldRunInBackground,@NonNull TrackerEvent.PublishListener trackerPublishListener) {
        if (token == null || token.isEmpty())
            trackerPublishListener.onFailure(ACCESS_TOKEN_NOT_AVAILABLE);
        else
            return new Publisher(context, token, trackId, shouldRunInBackground, trackerPublishListener);
        return null;
    }

    /**
     * Start LiveTracking Engine.
     * @param interval interval to fetch location updates in milliseconds
     */
    public void start(int interval) {
        this.interval = interval;
        nativePublisher.start(interval);
    }

    /**
     * Start LiveTracking Engine.
     */
    public void start() {
        nativePublisher.start(interval);
    }

    public void onPause() {
        if (!shouldRunInBackground)
            nativePublisher.stop();
    }

    public void onResume() {
        if (shouldRestart)
            nativePublisher.start(interval);
    }

    /**
     * Stop LiveTracking Engine.
     */
    public void stop() {
        nativePublisher.stop();
    }

    /**
     * Return true if LiveTracking is ready to start.
     */
    public boolean isTrackerReady() {
        return nativePublisher.isTrackerReady();
    }

    public class ReceiverCall extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null) {
                switch (intent.getAction()) {
                    case BROADCAST_INFO_ACTION_NAME:
                        if (intent.hasExtra("restart")) {
                            if (intent.getBooleanExtra("restart", true) && shouldRestart) {
                                Intent startIntent = new Intent(context, PublisherService.class);
                                intent.putExtra("interval", intent.getIntExtra("interval", 1000));
                                intent.putExtra("topic", intent.getStringExtra("topic"));
                                context.startService(startIntent);
                            }
                        }

                        break;
                    case BROADCAST_ERROR_ACTION_NAME:
                        break;
                }
            }
        }
    }
}

package ir.map.tracker;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;

import static ir.map.tracker.Constants.BROADCAST_ERROR_ACTION_NAME;
import static ir.map.tracker.Constants.BROADCAST_INFO_ACTION_NAME;
import static ir.map.tracker.Constants.CONNECTION_LOST;
import static ir.map.tracker.Constants.DEFAULT_INTERVAL;
import static ir.map.tracker.PublisherError.MISSING_API_KEY;
import static ir.map.tracker.PublisherError.MISSING_CONTEXT;
import static ir.map.tracker.PublisherError.MISSING_TRACK_ID;
import static ir.map.tracker.ServiceUtils.isLiveServiceRunning;

public class Publisher {

    private static boolean shouldRestart = true;
    private static TrackerEvent.PublishListener publishListener;
    private boolean runInBackground;
    private int interval = DEFAULT_INTERVAL;
    private NativePublisher nativePublisher;

    private Publisher(Context context, @NonNull String xApiKey, @NonNull String trackId, boolean runInBackground, TrackerEvent.PublishListener publishListener) {
        this.runInBackground = runInBackground;
        Publisher.publishListener = publishListener;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BROADCAST_INFO_ACTION_NAME);
        intentFilter.addAction(BROADCAST_ERROR_ACTION_NAME);
        context.registerReceiver(new LiveBroadcastReceiver(), intentFilter);
        nativePublisher = new NativePublisher(context, xApiKey, trackId, runInBackground, publishListener);
    }

    @RequiresPermission(allOf = {Manifest.permission.READ_PHONE_STATE, Manifest.permission.ACCESS_FINE_LOCATION})
    public static Publisher getLiveTracker(@NonNull Context context, @NonNull String apiKey, @NonNull String trackId, boolean runInBackground, @NonNull TrackerEvent.PublishListener trackerPublishListener) {
        if (context == null) {
            trackerPublishListener.onFailure(MISSING_CONTEXT);
            return null;
        }
        if (apiKey == null || apiKey.isEmpty()) {
            trackerPublishListener.onFailure(MISSING_API_KEY);
            return null;
        }
        if (trackId == null || trackId.isEmpty()) {
            trackerPublishListener.onFailure(MISSING_TRACK_ID);
            return null;
        }
        return new Publisher(context, apiKey, trackId, runInBackground, trackerPublishListener);
    }

    /**
     * Start LiveTracking Engine.
     *
     * @param interval interval to fetch location updates in milliseconds
     */
    public void start(int interval) {
        this.interval = interval;
        nativePublisher.start(interval);
    }

    /**
     * Stop LiveTracking Engine.
     */
    public void stop() {
        shouldRestart = false;
        nativePublisher.stop();
    }

    /**
     * Start LiveTracking Engine.
     */
    public void start() {
        nativePublisher.start(interval);
    }

    public void onPause() {
        if (!runInBackground)
            nativePublisher.stop();
    }

    public void onResume() {
        if (shouldRestart)
            nativePublisher.start(interval);
    }

    /**
     * Return true if LiveTracking is ready to start.
     */
    public boolean isReady() {
        return nativePublisher.isReady();
    }

    public static class LiveBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null) {
                switch (intent.getAction()) {
                    case BROADCAST_INFO_ACTION_NAME:
                        if (intent.hasExtra("restart")) {
                            if (intent.getBooleanExtra("restart", true) && shouldRestart) {
                                Intent startIntent = new Intent(context, PublisherService.class);
                                intent.putExtra("interval", intent.getIntExtra("interval", DEFAULT_INTERVAL));
                                intent.putExtra("topic", intent.getStringExtra("topic"));
                                if (!isLiveServiceRunning(context))
                                    context.startService(startIntent);
                            }
                        } else if (intent.hasExtra("live_location")) {
                            if (publishListener != null)
                                publishListener.publishedLocation((Location) intent.getParcelableExtra("live_location"));
                        }

                        break;
                    case BROADCAST_ERROR_ACTION_NAME:
                        if (intent.hasExtra("status")) {
                            try {
                                switch (intent.getStringExtra("status")) {
                                    case CONNECTION_LOST:
                                        if (publishListener != null)
                                            publishListener.onLiveTrackerDisconnected();
                                        break;
                                }
                            } catch (Exception e) {
                                if (publishListener != null)
                                    publishListener.onLiveTrackerDisconnected();
                            }
                        }
                        break;
                }
            }
        }
    }
}

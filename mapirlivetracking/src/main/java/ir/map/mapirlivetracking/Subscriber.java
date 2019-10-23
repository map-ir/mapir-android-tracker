package ir.map.mapirlivetracking;

import android.Manifest;
import android.content.Context;

import androidx.annotation.RequiresPermission;

public class Subscriber {

    private boolean shouldRestart = true;

    private NativeSubscriber nativeSubscriber;

    private Subscriber(Context context, String xApiKey, String trackId, TrackerEvent.SubscribeListener trackerSubscribeListener) {
        nativeSubscriber = new NativeSubscriber(context, xApiKey, trackId, trackerSubscribeListener);
    }

    public static Subscriber getLiveTracker(Context context, String token, String trackId, TrackerEvent.SubscribeListener trackerSubscribeListener) {
        return new Subscriber(context, token, trackId, trackerSubscribeListener);
    }

    /**
     * Start LiveTracking Engine.
     */
    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    public void start() {
        nativeSubscriber.start();
    }

    /**
     * Stop LiveTracking Engine.
     */
    public void stop() {
        nativeSubscriber.stop();
    }

    /**
     * Return true if LiveTracking is ready to start.
     */
    public boolean isSubscriberReady() {
        return nativeSubscriber.isSubscriberReady();
    }
}

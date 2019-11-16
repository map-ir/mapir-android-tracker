package ir.map.tracker;

import android.Manifest;
import android.content.Context;

import androidx.annotation.RequiresPermission;

import static ir.map.tracker.SubscriberError.MISSING_API_KEY;
import static ir.map.tracker.SubscriberError.MISSING_CONTEXT;
import static ir.map.tracker.SubscriberError.MISSING_TRACK_ID;

public class Subscriber {

    private NativeSubscriber nativeSubscriber;

    private Subscriber(Context context, String xApiKey, String trackId, TrackerEvent.SubscribeListener trackerSubscribeListener) {
        nativeSubscriber = new NativeSubscriber(context, xApiKey, trackId, trackerSubscribeListener);
    }

    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    public static Subscriber getLiveTracker(Context context, String apiKey, String trackId, TrackerEvent.SubscribeListener trackerSubscribeListener) {
        if (context == null) {
            trackerSubscribeListener.onFailure(MISSING_CONTEXT);
            return null;
        }
        if (apiKey == null || apiKey.isEmpty()) {
            trackerSubscribeListener.onFailure(MISSING_API_KEY);
            return null;
        }
        if (trackId == null || trackId.isEmpty()) {
            trackerSubscribeListener.onFailure(MISSING_TRACK_ID);
            return null;
        }
        return new Subscriber(context, apiKey, trackId, trackerSubscribeListener);
    }

    /**
     * Start LiveTracking Engine.
     */
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
    public boolean isReady() {
        return nativeSubscriber.isReady();
    }
}

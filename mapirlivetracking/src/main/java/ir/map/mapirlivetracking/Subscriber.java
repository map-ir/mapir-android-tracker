package ir.map.mapirlivetracking;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.RequiresPermission;

import static ir.map.mapirlivetracking.Constants.BROADCAST_ERROR_ACTION_NAME;
import static ir.map.mapirlivetracking.Constants.BROADCAST_INFO_ACTION_NAME;

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
    public void connect() {
        nativeSubscriber.start();
    }

    /**
     * Stop LiveTracking Engine.
     */
    public void disconnect() {
        nativeSubscriber.stop();
    }

    /**
     * Return true if LiveTracking is ready to start.
     */
    public boolean isSubscriberReady() {
        return nativeSubscriber.isSubscriberReady();
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

package ir.map.tracker;

import android.location.Location;

public interface TrackerEvent {
    interface PublishListener {
        /**
         * Notify that failed to send location
         */
        void onFailure(LiveTrackerError error);

        /**
         * Notify that engine is disconnected
         */
        void onLiveTrackerDisconnected();
    }

    interface SubscribeListener {
        /**
         * Notify that location received
         */
        void onLocationReceived(Location location);

        /**
         * Notify that engine is disconnected
         */
        void onLiveTrackerDisconnected();

        /**
         * Notify that failed to fetch
         */
        void onFailure(LiveTrackerError error);
    }
}

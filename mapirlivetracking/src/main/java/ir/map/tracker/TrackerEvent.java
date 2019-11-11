package ir.map.tracker;

import android.location.Location;

public interface TrackerEvent {
    interface PublishListener {
        /**
         * Notify that failed to send location
         */
        void onFailure(PublisherError error);

        /**
         * Notify that engine is disconnected
         */
        void onLiveTrackerDisconnected();

        /**
         * Notify Every Location Sent
         *
         * @param location
         */
        void publishedLocation(Location location);
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
        void onFailure(SubscriberError error);
    }
}

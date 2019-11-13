package ir.map.tracker;

class Constants {
    static final String BROKER_SERVER_URL = "tcp://tracking.map.ir:1883";
    static final String SERVER_URL = "https://tracking.map.ir/";
    static final String BROADCAST_INFO_ACTION_NAME = "TrackerServiceInfo";
    static final String BROADCAST_ERROR_ACTION_NAME = "TrackerServiceError";
    static final String CONNECTION_LOST = "CONNECTION_LOST";
    static final int DEFAULT_INTERVAL = 3000;
}

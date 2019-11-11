package ir.map.tracker;

class Constants {
    static final String BROKER_SERVER_URL = "tcp://dev.map.ir:1883";
    static final String SERVER_URL = "https://tracking-dev.map.ir/";
    static final String BROADCAST_INFO_ACTION_NAME = "LiveTrackingServiceInfo";
    static final String BROADCAST_ERROR_ACTION_NAME = "LiveTrackingServiceError";
    static final String CONNECTION_LOST = "CONNECTION_LOST";
    static final int DEFAULT_INTERVAL = 3000;
}

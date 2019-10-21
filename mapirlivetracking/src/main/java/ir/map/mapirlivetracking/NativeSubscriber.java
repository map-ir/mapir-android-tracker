package ir.map.mapirlivetracking;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.telephony.TelephonyManager;

import com.google.protobuf.InvalidProtocolBufferException;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import ir.map.mapirlivetracking.network.MapirSubscriptionResponseListener;
import ir.map.mapirlivetracking.network.model.Subscription;
import tutorial.Dataformat;

import static ir.map.mapirlivetracking.Constants.BROKER_SERVER_URL;
import static ir.map.mapirlivetracking.LiveTrackerError.CONNECTION_LOST;
import static ir.map.mapirlivetracking.LiveTrackerError.INITIALIZE_ERROR;

class NativeSubscriber implements MqttCallback, MapirSubscriptionResponseListener {

    private MqttAndroidClient client;
    private Context context;
    private String xApiKey;
    private String trackId;
    private boolean isSubscriberReady = false;
    private boolean shouldStart = false;
    private String deviceId;
    private TrackerEvent.SubscribeListener trackerSubscribeListener;
    private Subscription subscription;
    private int mqttConnectRetryCount = 0;

    NativeSubscriber(Context context, String xApiKey, String trackId, TrackerEvent.SubscribeListener trackerSubscribeListener) {
        this.context = context;
        this.xApiKey = xApiKey;
        this.trackId = trackId;
        this.trackerSubscribeListener = trackerSubscribeListener;
        init();
    }

    @SuppressLint({"MissingPermission", "HardwareIds"})
    private void init() {
        deviceId = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
        new Services().fetchClient(xApiKey, deviceId, trackId, this);
    }

    @Override
    public void connectionLost(Throwable cause) {
        trackerSubscribeListener.onLiveTrackerDisconnected();
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        try {
            Location location = new Location("");
            Dataformat.LiveV1 dataformat = Dataformat.LiveV1.parseFrom(message.getPayload());
            location.setLongitude(dataformat.getLocationList().get(0));
            location.setLatitude(dataformat.getLocationList().get(1));
            location.setSpeed((float) dataformat.getSpeed());
            location.setTime(Long.parseLong(dataformat.getRtimestamp()));
            location.setBearing(dataformat.getDirection());
            trackerSubscribeListener.onLocationReceived(location);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }

    boolean isSubscriberReady() {
        return isSubscriberReady;
    }

    private void initMqtt() {
        client = new MqttAndroidClient(context, BROKER_SERVER_URL, trackId);
        client.setCallback(this);
    }

    private void connectMqtt() {
        if (mqttConnectRetryCount < 3) {
            try {
                client.connect().setActionCallback(new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        isSubscriberReady = true;
                        mqttConnectRetryCount = 0;
                        if (shouldStart) {
                            subscribe();
                        }
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        isSubscriberReady = false;
                        mqttConnectRetryCount++;
                        connectMqtt();
                    }
                });
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    void start() {
        shouldStart = true;
        if (client != null && !client.isConnected()) {
            connectMqtt();
        }
    }

    private void subscribe() {
        try {
            client.subscribe(subscription.getData().getTopic(), 0);
        } catch (MqttException e) {
            trackerSubscribeListener.onFailure(INITIALIZE_ERROR);
        }
    }

    void stop() {
        shouldStart = false;
        if (client.isConnected()) {
            try {
                client.disconnect();
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onSubscribeSuccess(Subscription subscription) {
        this.subscription = subscription;
        initMqtt();
        connectMqtt();
    }

    @Override
    public void onSubscribeFailure(Throwable error) {
        isSubscriberReady = false;
        trackerSubscribeListener.onFailure(CONNECTION_LOST);
    }
}

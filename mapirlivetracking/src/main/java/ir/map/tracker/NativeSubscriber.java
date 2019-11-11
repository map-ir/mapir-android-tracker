package ir.map.tracker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.telephony.TelephonyManager;

import androidx.core.content.ContextCompat;

import com.google.protobuf.InvalidProtocolBufferException;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import ir.map.tracker.network.SubscriptionResponseListener;
import ir.map.tracker.network.model.Subscription;
import tutorial.Dataformat;

import static ir.map.tracker.Constants.BROKER_SERVER_URL;
import static ir.map.tracker.SubscriberError.INITIALIZE_ERROR;
import static ir.map.tracker.SubscriberError.TELEPHONY_PERMISSION;

class NativeSubscriber implements MqttCallback, SubscriptionResponseListener {

    private MqttAndroidClient client;
    private Context context;
    private String xApiKey;
    private String trackId;
    private boolean isReady = false;
    private boolean shouldStart = false;
    private TrackerEvent.SubscribeListener subscribeListener;
    private Subscription subscription;
    private int mqttConnectRetryCount = 0;

    NativeSubscriber(Context context, String xApiKey, String trackId, TrackerEvent.SubscribeListener subscribeListener) {
        this.context = context;
        this.xApiKey = xApiKey;
        this.trackId = trackId;
        this.subscribeListener = subscribeListener;
        init();
    }

    @SuppressLint({"MissingPermission", "HardwareIds"})
    private void init() {
        if (Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            subscribeListener.onFailure(TELEPHONY_PERMISSION);
        } else {
            try {
                new Services().subscribeClient(xApiKey, ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId(), trackId, this);
            } catch (Exception e) {
                subscribeListener.onFailure(INITIALIZE_ERROR);
            }
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        subscribeListener.onLiveTrackerDisconnected();
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
            subscribeListener.onLocationReceived(location);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }

    boolean isReady() {
        return isReady;
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
                        isReady = true;
                        mqttConnectRetryCount = 0;
                        if (shouldStart) {
                            subscribe();
                        }
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        isReady = false;
                        mqttConnectRetryCount++;
                        connectMqtt();
                    }
                });
            } catch (MqttException e) {
                subscribeListener.onFailure(INITIALIZE_ERROR);
                e.printStackTrace();
            }
        } else {
            subscribeListener.onFailure(INITIALIZE_ERROR);
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
            if (client != null)
                client.subscribe(subscription.getData().getTopic(), 0);
        } catch (MqttException e) {
            subscribeListener.onFailure(INITIALIZE_ERROR);
        }
    }

    void stop() {
        shouldStart = false;
        if (client != null && client.isConnected()) {
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
        isReady = false;
        subscribeListener.onFailure(INITIALIZE_ERROR);
    }
}

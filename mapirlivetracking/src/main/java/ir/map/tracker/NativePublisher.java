package ir.map.tracker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Looper;
import android.telephony.TelephonyManager;

import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.tasks.OnSuccessListener;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import ir.map.tracker.network.RegistrationResponseListener;
import ir.map.tracker.network.model.Register;
import tutorial.Dataformat;

import static ir.map.tracker.Constants.BROKER_SERVER_URL;
import static ir.map.tracker.Constants.DEFAULT_INTERVAL;
import static ir.map.tracker.LocationHelper.getLocationClient;
import static ir.map.tracker.LocationHelper.getLocationRequest;
import static ir.map.tracker.PublisherError.INITIALIZE_ERROR;
import static ir.map.tracker.PublisherError.TELEPHONY_PERMISSION;
import static ir.map.tracker.ServiceHelper.isLiveServiceRunning;

class NativePublisher implements RegistrationResponseListener, MqttCallback {

    private Context context;
    private String apiKey;
    private String trackId;
    private boolean isReady = false;
    private boolean runInBackground;
    private int interval = DEFAULT_INTERVAL;
    private TrackerEvent.PublishListener trackerPublishListener;
    private boolean shouldRestart = true;
    private boolean shouldStart = false;
    private Register register;
    private MqttConnectOptions mqttOptions = new MqttConnectOptions();
    private FusedLocationProviderClient locationClient;
    private MqttAndroidClient mqttClient;
    private int mqttConnectRetryCount = 0;
    private LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (locationResult == null) {
                return;
            }
            if (mqttClient != null && mqttClient.isConnected()) {
                Location location = locationResult.getLocations().get(locationResult.getLocations().size() - 1);
                publish(location);
            }
        }
    };

    NativePublisher(Context context, String apiKey, String trackId, boolean runInBackground, TrackerEvent.PublishListener trackerPublishListener) {
        this.context = context;
        this.apiKey = apiKey;
        this.trackId = trackId;
        this.runInBackground = runInBackground;
        this.trackerPublishListener = trackerPublishListener;
        init();
    }

    @SuppressLint({"MissingPermission", "HardwareIds"})
    private void init() {
        if (Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            trackerPublishListener.onFailure(TELEPHONY_PERMISSION);
        } else {
            try {
                new Services().publishClient(apiKey, ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId(), trackId, this);
            } catch (Exception e) {
                trackerPublishListener.onFailure(INITIALIZE_ERROR);
            }
        }
    }

    @Override
    public void onRegisterSuccess(Register register) {
        this.register = register;
        mqttOptions.setAutomaticReconnect(false);
        isReady = true;
        if (shouldStart) {
            if (runInBackground)
                startService(this.register);
            else {
                mqttClient = new MqttAndroidClient(context, BROKER_SERVER_URL, register.getData().getTopic());
                mqttClient.setCallback(this);
                connectMqtt();
            }
        }
    }

    private void connectMqtt() {
        if (!mqttClient.isConnected()) {
            if (mqttConnectRetryCount < 3) {
                try {
                    mqttClient.connect(mqttOptions).setActionCallback(new IMqttActionListener() {
                        @Override
                        public void onSuccess(IMqttToken asyncActionToken) {
                            mqttConnectRetryCount = 0;
                            initFusedLocation();
                        }

                        @Override
                        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                            mqttConnectRetryCount++;
                            connectMqtt();
                        }
                    });
                } catch (MqttException e) {
                    mqttConnectRetryCount++;
                    connectMqtt();
                }
            } else {
                trackerPublishListener.onLiveTrackerDisconnected();
            }
        }
    }

    private void initFusedLocation() {
        locationClient = getLocationClient(context);

        LocationRequest locationRequest = getLocationRequest(interval);

        locationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (mqttClient != null && mqttClient.isConnected()) {
                    if (location != null) {
                        publish(location);
                    }
                }
            }
        });

        locationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
    }

    @Override
    public void onRegisterFailure(Throwable error) {
        trackerPublishListener.onFailure(INITIALIZE_ERROR);
    }

    private void publish(Location location) {
        if (mqttClient.isConnected()) {
            try {
                JSONArray locationArray = new JSONArray();
                locationArray.put(0, location.getLongitude());
                locationArray.put(1, location.getLatitude());

                List<Double> locations = new ArrayList<>();
                locations.add(locationArray.getDouble(0));
                locations.add(locationArray.getDouble(1));

                mqttClient.publish(register.getData().getTopic(), new MqttMessage(
                        Dataformat.LiveV1.newBuilder()
                                .setDirection(location.getBearing())
                                .addAllLocation(locations)
                                .setRtimestamp(String.valueOf(location.getTime()))
                                .setSpeed(location.getSpeed())
                                .build()
                                .toByteArray()));

                trackerPublishListener.publishedLocation(location);

            } catch (MqttException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    boolean isReady() {
        return isReady;
    }

    void start(int interval) {
        shouldStart = true;
        shouldRestart = true;
        this.interval = interval;
        if (Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            trackerPublishListener.onFailure(PublisherError.LOCATION_PERMISSION);
        } else {
            shouldStart = true;
            if (isReady && register != null) {
                if (runInBackground)
                    startService(this.register);
                else
                    connectMqtt();
            }
        }
    }

    void stop() {
        shouldRestart = false;
        if (runInBackground) {
            if (isLiveServiceRunning(context))
                stopService();
        } else {
            removeLocationUpdate();
            disconnectMqtt();
        }
    }

    private void startService(Register register) {
        stopService();
        Intent intent = new Intent(context, PublisherService.class);
        intent.putExtra("background_running", runInBackground);
        intent.putExtra("interval", interval);
        intent.putExtra("topic", register.getData().getTopic());
        intent.putExtra("restart", shouldRestart);
        context.startService(intent);
    }

    void stopService() {
        ServiceHelper.stopService(context);
    }

    private void disconnectMqtt() {
        if (mqttClient != null) {
            try {
                if (mqttClient.isConnected())
                    mqttClient.disconnect();
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    private void removeLocationUpdate() {
        if (locationClient != null && locationCallback != null) {
            locationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        if (shouldRestart)
            connectMqtt();
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {

    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }
}

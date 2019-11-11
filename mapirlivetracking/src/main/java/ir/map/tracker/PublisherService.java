package ir.map.tracker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

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

import tutorial.Dataformat;

import static ir.map.tracker.Constants.BROADCAST_ERROR_ACTION_NAME;
import static ir.map.tracker.Constants.BROADCAST_INFO_ACTION_NAME;
import static ir.map.tracker.Constants.BROKER_SERVER_URL;
import static ir.map.tracker.Constants.CONNECTION_LOST;
import static ir.map.tracker.Constants.DEFAULT_INTERVAL;
import static ir.map.tracker.LocationHelper.getLocationClient;
import static ir.map.tracker.LocationHelper.getLocationRequest;

public class PublisherService extends Service implements MqttCallback {

    private static final int NOTIFICATION_ID = 1001;
    private LocationRequest locationRequest;
    private FusedLocationProviderClient locationClient;
    private MqttAndroidClient mqttClient;
    private int interval = DEFAULT_INTERVAL;
    private String topic;
    private int mqttConnectRetryCount = 0;
    private boolean shouldRestart = true;
    private boolean shouldRunInBackground = false;
    private MqttConnectOptions mqttOptions = new MqttConnectOptions();

    public PublisherService() {

    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

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


    public int onStartCommand(Intent intent, int flags, int startId) {
        shouldRestart = intent.getBooleanExtra("restart", true);
        interval = intent.getIntExtra("interval", DEFAULT_INTERVAL);
        topic = intent.getStringExtra("topic");
        shouldRunInBackground = intent.getBooleanExtra("background_running", false);
        mqttOptions.setAutomaticReconnect(false);

        if (shouldRestart) {
            mqttClient = new MqttAndroidClient(getApplicationContext(), BROKER_SERVER_URL, topic);
            mqttClient.setCallback(this);
            connectMqtt();
            if (shouldRunInBackground) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    startMyOwnForeground();
                else
                    startForeground();
            }
        } else {
            removeLocationUpdate();
            disconnectMqtt();
            stopForeground();
            stopSelf();
        }
        return START_STICKY;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startMyOwnForeground() {
        String NOTIFICATION_CHANNEL_ID = "live";
        String channelName = "live";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setContentTitle("App is running in background")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(2, notification);
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
                Intent intent = new Intent(BROADCAST_ERROR_ACTION_NAME);
                intent.putExtra("status", CONNECTION_LOST);
                sendBroadcast(intent);
            }
        }
    }

    private void initFusedLocation() {
        locationClient = getLocationClient(PublisherService.this);

        locationRequest = getLocationRequest(interval);

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
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        removeLocationUpdate();
        disconnectMqtt();
        if (shouldRestart) {
            Intent intent = new Intent(BROADCAST_INFO_ACTION_NAME);
            intent.putExtra("restart", shouldRestart);
            intent.putExtra("interval", interval);
            intent.putExtra("topic", topic);
            sendBroadcast(intent);
        }
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
    public void messageArrived(String topic, MqttMessage message) {

    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

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

                mqttClient.publish(topic, new MqttMessage(
                        Dataformat.LiveV1.newBuilder()
                                .setDirection(location.getBearing())
                                .addAllLocation(locations)
                                .setRtimestamp(String.valueOf(location.getTime()))
                                .setSpeed(location.getSpeed())
                                .build()
                                .toByteArray()));

                Intent intent = new Intent(BROADCAST_INFO_ACTION_NAME);
                intent.putExtra("live_location", location);
                sendBroadcast(intent);

            } catch (MqttException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        removeLocationUpdate();
        disconnectMqtt();
        if (shouldRestart) {
            Intent intent = new Intent(BROADCAST_INFO_ACTION_NAME);
            intent.putExtra("restart", shouldRestart);
            intent.putExtra("interval", interval);
            intent.putExtra("topic", topic);
            sendBroadcast(intent);
        }
        super.onTaskRemoved(rootIntent);
    }

    public void startForeground() {
        Notification notificationCompat = createBuilderNotification().build();
        startForeground(NOTIFICATION_ID, notificationCompat);
    }

    private void stopForeground() {
        removeLocationUpdate();
        disconnectMqtt();
        stopForeground(true);
    }

    private NotificationCompat.Builder createBuilderNotification() {
        return new NotificationCompat.Builder(this, "mapir-tracker")
                .setContentText("Tracking");
    }
}

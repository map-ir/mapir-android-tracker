package ir.map.mapirlivetracking;

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
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import tutorial.Dataformat;

import static ir.map.mapirlivetracking.Constants.BROADCAST_ERROR_ACTION_NAME;
import static ir.map.mapirlivetracking.Constants.BROADCAST_INFO_ACTION_NAME;
import static ir.map.mapirlivetracking.Constants.BROKER_SERVER_URL;
import static ir.map.mapirlivetracking.LiveTrackerError.CONNECTION_LOST;

public class PublisherService extends Service implements MqttCallback, IMqttActionListener {

    private static final int NOTIFICATION_ID = 1001;
    private LocationRequest locationRequest;
    private FusedLocationProviderClient mFusedLocationClient;
    private Location lastLocation;
    private MqttAndroidClient mqttClient;
    private int interval = -1;
    private String topic;
    private int mqttConnectRetryCount = 0;
    private boolean shouldRestart = true;
    private boolean shouldRunInBackground = false;

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
            Location location = locationResult.getLocations().get(locationResult.getLocations().size() - 1);
            if (location != lastLocation) {
                lastLocation = location;
                publish(lastLocation);
            }
        }
    };


    public int onStartCommand(Intent intent, int flags, int startId) {
        shouldRestart = intent.getBooleanExtra("restart", true);
        interval = intent.getIntExtra("interval", 1000);
        topic = intent.getStringExtra("topic");
        shouldRunInBackground = intent.getBooleanExtra("background_running", false);

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
//                MqttConnectOptions options = new MqttConnectOptions();
//                options.setUserName(username);
//                options.setPassword(password.toCharArray());
                    mqttClient.connect().setActionCallback(new IMqttActionListener() {
                        @Override
                        public void onSuccess(IMqttToken asyncActionToken) {
                            mqttConnectRetryCount = 0;

                            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(PublisherService.this);

                            locationRequest = LocationRequest.create();
                            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                            locationRequest.setInterval(interval);
                            locationRequest.setFastestInterval(interval);

                            mFusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                                @Override
                                public void onSuccess(Location location) {
                                    if (mqttClient.isConnected()) {
                                        if (location != null) {
                                            publish(location);
                                        }
                                    }
                                }
                            });

                            mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
                        }

                        @Override
                        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                            mqttConnectRetryCount++;
                            connectMqtt();
                        }
                    });
                } catch (MqttException e) {
                    mqttConnectRetryCount++;
                    Log.e("LIVE_TRACKER", e.getMessage());
                    connectMqtt();
                }
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

//    @Override
//    protected void onHandleIntent(@Nullable Intent intent) {
//        if (intent != null) {
//            final String action = intent.getAction();
//            if (ACTION_PROCESS_UPDATES.equals(action)) {
//                LocationResult locationResult = LocationResult.extractResult(intent);
//                Location location = locationResult.getLocations().get(locationResult.getLocations().size() - 1);
//                if (location != lastLocation) {
//                    lastLocation = location;
//                    publish(lastLocation);
//                }
//            }
//        }
//    }

    @Override
    public void onDestroy() {
        removeLocationUpdate();
        if (shouldRestart) {
            Intent intent = new Intent(BROADCAST_INFO_ACTION_NAME);
            intent.putExtra("restart", shouldRestart);
            intent.putExtra("interval", interval);
            intent.putExtra("topic", topic);
            sendBroadcast(intent);
        }
    }

    private void removeLocationUpdate() {
        if (mFusedLocationClient != null && locationCallback != null) {
            mFusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }


    @Override
    public void onSuccess(IMqttToken asyncActionToken) {
        mqttConnectRetryCount = 0;

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(PublisherService.this);

        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(interval);
        locationRequest.setFastestInterval(interval);

        mFusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (mqttClient.isConnected()) {
                    if (location != null) {
                        publish(location);
                    }
                }
            }
        });

        mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
    }

    @Override
    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
        mqttConnectRetryCount++;
        connectMqtt();
    }

    @Override
    public void connectionLost(Throwable cause) {
        if (mqttConnectRetryCount < 3) {
            mqttConnectRetryCount++;
            connectMqtt();
        } else {
            Intent intent = new Intent(BROADCAST_ERROR_ACTION_NAME);
            intent.putExtra("mode", "error");
            intent.putExtra("status", CONNECTION_LOST);
            sendBroadcast(intent);
        }
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
        stopForeground(true);
    }

    private NotificationCompat.Builder createBuilderNotification() {
        return new NotificationCompat.Builder(this, "live")
                .setContentText("Content Text Service");
    }

}

package ir.map.livetrackingsample;

import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.location.Location;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;

import ir.map.tracker.Publisher;
import ir.map.tracker.PublisherError;
import ir.map.tracker.Subscriber;
import ir.map.tracker.SubscriberError;
import ir.map.tracker.TrackerEvent;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconRotate;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, TrackerEvent.PublishListener {

    private Publisher publisher;
    private Subscriber subscriber;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private MapboxMap map;
    private MapView mapView;
    private Style loadedStyle;
    private String trackId = "rte4";
    private GeoJsonSource source;
    private SymbolLayer symbolLayer;
    private ValueAnimator animator;
    private LatLng firstPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
    }

    @Override
    public void onFailure(PublisherError code) {
        switch (code) {
            case LOCATION_PERMISSION:

                break;
            case API_KEY_NOT_AVAILABLE:

                break;
            case INITIALIZE_ERROR:

                break;
            case TELEPHONY_PERMISSION:

                break;
        }
    }

//    @SuppressLint("SetTextI18n")
//    @Override
//    public void onLocationReceived(Location location) {
//        if (loadedStyle.getLayer(trackId) != null) {
//            animateSymbolToPosition(location);
//        } else {
//            firstPosition = new LatLng(location.getLatitude(), location.getLongitude());
//            source = new GeoJsonSource(
//                    trackId,
//                    Feature.fromGeometry(
//                            Point.fromLngLat(
//                                    location.getLatitude(),
//                                    location.getLongitude())));
//            loadedStyle.addSource(source);
//
//            symbolLayer = new SymbolLayer(trackId, trackId).withProperties(
//                    PropertyFactory.iconSize(1f),
//                    PropertyFactory.iconImage(trackId),
//                    PropertyFactory.iconAllowOverlap(true),
//                    PropertyFactory.iconIgnorePlacement(true)
//            );
//
//            loadedStyle.addLayer(symbolLayer);
//        }
////        trackText.setText("Lat : " + location.getLatitude() + "\n" + "Lon : " + location.getLongitude() + "\n" + "Speed : " + location.getSpeed()
////                + "\n" + "Date : " + getDateCurrentTimeZone(location.getTime()));
//    }

    private void animateSymbolToPosition(Location location) {
        if (animator != null && animator.isStarted()) {
            animator.cancel();
        }

        LatLng target = new LatLng(location.getLatitude(), location.getLongitude());

        animator =
                ObjectAnimator.ofObject(latLngTypeEvaluator, firstPosition, target)
                        .setDuration(700);
        animator.addUpdateListener(valueAnimator -> {
            LatLng animatedPosition = (LatLng) valueAnimator.getAnimatedValue();
            source.setGeoJson(
                    Feature.fromGeometry(
                            Point.fromLngLat(
                                    animatedPosition.getLongitude(),
                                    animatedPosition.getLatitude()
                            )
                    )
            );
        });
        animator.start();

        firstPosition = target;

        if (location.getBearing() != 0.0f) {
            loadedStyle.getLayer(trackId).setProperties(
                    iconRotate(location.getBearing())
            );
        }
    }

    private TypeEvaluator<LatLng> latLngTypeEvaluator = (fraction, startValue, endValue) -> {
        LatLng latLng = new LatLng();
        latLng.setLatitude(startValue.getLatitude() + (endValue.getLatitude() - startValue.getLatitude()) * fraction);
        latLng.setLongitude(startValue.getLongitude() + (endValue.getLongitude() - startValue.getLongitude()) * fraction);
        return latLng;
    };

    @Override
    public void onLiveTrackerDisconnected() {
        Toast.makeText(this, "onLiveTrackerDisconnected", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void publishedLocation(Location location) {
        Toast.makeText(MainActivity.this, location.getLatitude() + " : " + location.getLongitude(), Toast.LENGTH_SHORT).show();
    }

    public String getDateCurrentTimeZone(long timestamp) {
        try {
            Calendar calendar = Calendar.getInstance();
            TimeZone tz = TimeZone.getTimeZone("ir");
            calendar.setTimeInMillis(timestamp * 1000);
            calendar.add(Calendar.MILLISECOND, tz.getOffset(calendar.getTimeInMillis()));
            Date currentTimeZone = calendar.getTime();
            return sdf.format(currentTimeZone);
        } catch (Exception ignored) {
        }
        return "";
    }

    @Override
    protected void onPause() {
        mapView.onPause();
        if (publisher != null)
            publisher.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        mapView.onResume();
        if (publisher != null)
            publisher.onResume();
        super.onResume();
    }

    @SuppressLint({"MissingPermission", "WrongConstant"})
    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMap) {
        map = mapboxMap;
        map.setStyle("https://map.ir/vector/styles/main/main_mobile_style.json", style -> {
            loadedStyle = style;

            loadedStyle.addImage(trackId, Objects.requireNonNull(AppCompatResources.getDrawable(getBaseContext(), R.drawable.mapbox_marker_icon_default)));

            // Publisher
//            publisher = Publisher.getLiveTracker(getBaseContext(), getString(R.string.api_key), trackId, false, this);
//            publisher.start(1000);
//
//            new Handler().postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    publisher.stop();
//                    new Handler().postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            publisher.start(1000);
//                        }
//                    }, 15000);
//                }
//            }, 10000);

            // Subscriber
            Subscriber subscriber = Subscriber.getLiveTracker(getBaseContext(), getString(R.string.api_key), trackId, new TrackerEvent.SubscribeListener() {
                @Override
                public void onLocationReceived(Location location) {
                    Toast.makeText(MainActivity.this, location.getLatitude() + " : " + location.getLongitude(), Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onLiveTrackerDisconnected() {
                    Toast.makeText(MainActivity.this, "onLiveTrackerDisconnected", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(SubscriberError error) {
                    Toast.makeText(MainActivity.this, error.toString(), Toast.LENGTH_SHORT).show();
                }
            });
            subscriber.start();
        });
    }
}

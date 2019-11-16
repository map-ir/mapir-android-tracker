package ir.map.tracker.subscriber_sample;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.transition.ChangeBounds;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.RotateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import ir.map.sdk_map.camera.CameraUpdateFactory;
import ir.map.sdk_map.geometry.LatLng;
import ir.map.sdk_map.maps.MapView;
import ir.map.sdk_map.maps.MapirMap;
import ir.map.sdk_map.maps.OnMapReadyCallback;
import ir.map.sdk_map.style.layers.PropertyFactory;
import ir.map.sdk_map.style.layers.SymbolLayer;
import ir.map.sdk_map.style.sources.GeoJsonSource;
import ir.map.tracker.Subscriber;
import ir.map.tracker.SubscriberError;
import ir.map.tracker.TrackerEvent;

import static ir.map.sdk_map.style.layers.PropertyFactory.iconRotate;

public class MainActivity extends AppCompatActivity implements
        TrackerEvent.SubscribeListener,
        OnMapReadyCallback {

    private final int REQUEST_PERMISSION_READ_PHONE_STATE = 1000;

    private String defaultTrackId = "shiveh_live_tracking_test";
    private int receiveCount = 1;

    private Subscriber subscriber;
    private MapirMap mapirMap;
    private MapView mapView;
    private GeoJsonSource source;
    private ValueAnimator animator;
    private LatLng firstPosition;

    private ConstraintLayout rootView;
    private EditText liveTrackingIdEdt;
    private EditText liveTrackingLogEdt;
    private ImageView playPauseImg;
    private View mapCoverView;

    private boolean playPause = false; //true: play and false: pause

    private ConstraintSet constraintSet = new ConstraintSet();
    private TypeEvaluator<LatLng> latLngTypeEvaluator = (fraction, startValue, endValue) -> {
        LatLng latLng = new LatLng();
        latLng.setLatitude(startValue.getLatitude() + (endValue.getLatitude() - startValue.getLatitude()) * fraction);
        latLng.setLongitude(startValue.getLongitude() + (endValue.getLongitude() - startValue.getLongitude()) * fraction);
        return latLng;
    };

    @SuppressLint({"MissingPermission"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        try {
            Objects.requireNonNull(this.getSupportActionBar()).hide();
        } catch (NullPointerException ignored) {
        }

        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.mapView_map);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        rootView = findViewById(R.id.main_activity_root);

        liveTrackingIdEdt = findViewById(R.id.live_tracking_id_fedt);
        playPauseImg = findViewById(R.id.play_pause_img);
        liveTrackingLogEdt = findViewById(R.id.live_tracking_log_fedt);
        mapCoverView = findViewById(R.id.map_cover_view);

        playPauseImg.setOnClickListener(v -> {
            hideKeyboard();

            if (permissionsGranted())
                if (playPause) {
                    liveTrackingIdEdt.setEnabled(true);
                    destroyStartAnimation();
                    destroyStopAnimation();
                    playStopAnimation();
                    if (subscriber != null)
                        subscriber.stop();

                    Toast.makeText(this, "Receiving location stopped.", Toast.LENGTH_LONG).show();
                    liveTrackingLogEdt.setText("STATUS would logged here");

                    playPause = false;
                    playPauseImg.setImageResource(R.drawable.ic_start);

                    mapirMap.removeLayer(defaultTrackId);
                    mapirMap.removeSource(defaultTrackId);
                    receiveCount = 1;
                } else {
                    liveTrackingIdEdt.setEnabled(false);
                    destroyStartAnimation();
                    destroyStopAnimation();
                    playStartAnimation();
                    if (liveTrackingIdEdt.getText().toString().isEmpty())
                        setupSubscriber(defaultTrackId);
                    else
                        setupSubscriber(liveTrackingIdEdt.getText().toString());

                    playPause = true;
                    new Handler().postDelayed(() -> playPauseImg.setImageResource(R.drawable.ic_stop), 1000);

                    liveTrackingLogEdt.setText("Receiving location started");
                }
        });
    }

    @Override
    public void onMapReady(MapirMap mapirMap) {
        this.mapirMap = mapirMap;
        mapirMap.addImage(defaultTrackId, getBitmapFromDrawable());
    }

    @Override
    public void onFailure(SubscriberError error) {
        switch (error) {
            case INITIALIZE_ERROR:
                liveTrackingLogEdt.setText("Initialize error");
                break;
            case TELEPHONY_PERMISSION:
                liveTrackingLogEdt.setText("Missing TELEPHONY permission");
                break;
            case MISSING_API_KEY:
                liveTrackingLogEdt.setText("Missing apikey");
                break;
            case MISSING_TRACK_ID:
                liveTrackingLogEdt.setText("Missing track_id");
                break;
            case MISSING_CONTEXT:
                liveTrackingLogEdt.setText("Missing context");
                break;
        }
    }

    @Override
    public void onLiveTrackerDisconnected() {
        liveTrackingLogEdt.setText("Live Tracker Disconnected");
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onLocationReceived(Location location) {
        liveTrackingLogEdt.setText("N" + receiveCount + "; Lat: " + location.getLatitude() + ", Lon: " + location.getLongitude());
        receiveCount++;

        if (mapirMap.getLayer(defaultTrackId) != null) {
            animateSymbolToPosition(location);
        } else {
            firstPosition = new LatLng(location.getLatitude(), location.getLongitude());
            source = new GeoJsonSource(
                    defaultTrackId,
                    Feature.fromGeometry(
                            Point.fromLngLat(
                                    location.getLatitude(),
                                    location.getLongitude())));
            mapirMap.addSource(source);

            SymbolLayer symbolLayer = new SymbolLayer(defaultTrackId, defaultTrackId).withProperties(
                    PropertyFactory.iconSize(1f),
                    PropertyFactory.iconImage(defaultTrackId),
                    PropertyFactory.iconAllowOverlap(true),
                    PropertyFactory.iconIgnorePlacement(true)
            );

            mapirMap.addLayer(symbolLayer);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NotNull String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_READ_PHONE_STATE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                }
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    protected void onPause() {
        mapView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        mapView.onResume();
        super.onResume();
    }

    @SuppressLint({"MissingPermission"})
    private void setupSubscriber(String trackId) {
        if (permissionsGranted()) {
            if (subscriber != null)
                subscriber.stop();

            subscriber = Subscriber.getLiveTracker(
                    getBaseContext(), getString(R.string.api_key),
                    trackId,
                    this);
            subscriber.start();
        }
    }

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

        if (location.getBearing() != 0.0f && mapirMap.getLayer(defaultTrackId) != null) {
            (mapirMap.getLayer(defaultTrackId)).setProperties(iconRotate(location.getBearing()));
        }

        int zoom = 16;
        if (mapirMap.getCameraPosition().zoom < 16) zoom = 16;

        mapirMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), zoom));
    }

    private void playStartAnimation() {

        constraintSet.clone(this, R.layout.activity_main_after);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            Transition transition = new ChangeBounds();
            transition.setInterpolator(new AnticipateOvershootInterpolator(1.0f));
            transition.setDuration(1000);

            TransitionManager.beginDelayedTransition(rootView, transition);
            constraintSet.applyTo(rootView);
        }

        ValueAnimator alphaAnimation = ValueAnimator.ofInt(1, 0);
        alphaAnimation.setDuration(1000);
        alphaAnimation.addUpdateListener(animator -> mapCoverView.setAlpha((int) animator.getAnimatedValue()));
        alphaAnimation.start();

        new Handler().postDelayed(() -> {
            RotateAnimation rotate = new RotateAnimation(
                    0, 432000,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f
            );
            rotate.setDuration(600000);
            rotate.setRepeatCount(Animation.INFINITE);

            playPauseImg.startAnimation(rotate);
        }, 1500);
    }

    private void destroyStartAnimation() {
        playPauseImg.clearAnimation();
    }

    private void playStopAnimation() {
        playPauseImg.clearAnimation();

        mapCoverView.setAlpha(1);

        constraintSet.clone(this, R.layout.activity_main_before);

        Transition transition;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            transition = new ChangeBounds();
            transition.setInterpolator(new AnticipateOvershootInterpolator(1.0f));
            transition.setDuration(1000);

            TransitionManager.beginDelayedTransition(rootView, transition);
            constraintSet.applyTo(rootView);
        }
    }

    private void destroyStopAnimation() {
    }

    private boolean permissionsGranted() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.READ_PHONE_STATE},
                    REQUEST_PERMISSION_READ_PHONE_STATE);
            return false;
        }

        return true;
    }

    public void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null)
            view = new View(this);

        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    Bitmap getBitmapFromDrawable() {
        Drawable drawable = AppCompatResources.getDrawable(getBaseContext(), R.drawable.ic_direction_arrow);
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        } else {
            Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(),
                    Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            return bitmap;
        }
    }
}

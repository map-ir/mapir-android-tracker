package ir.map.tracker;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;

class ServiceUtils {
    static boolean isLiveServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (PublisherService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    static void stopService(Context context) {
        context.stopService(new Intent(context, PublisherService.class));
    }
}

package ir.map.tracker;

import android.content.Context;
import android.os.Build;

class HeaderUtils {

    static final String SDK_HEADER_KEY = "MapIr-SDK";
    static final String SDK_API_KEY_KEY = "x-api-key";

    static String getUserAgent(Context context) {
        return String.format("Android-(%s)-%s-%s-(%s)",
                BuildConfig.VERSION_NAME,
                context.getPackageName(),
                Build.VERSION.SDK_INT,
                Build.CPU_ABI);
    }
}

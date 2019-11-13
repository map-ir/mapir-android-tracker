package ir.map.tracker;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

import static ir.map.tracker.Constants.SERVER_URL;
import static ir.map.tracker.HeaderUtils.SDK_API_KEY_KEY;
import static ir.map.tracker.HeaderUtils.SDK_HEADER_KEY;

class NetworkUtils {
    static HttpURLConnection getHttpConnection(HashMap<String, String> parameters) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(SERVER_URL).openConnection();

            conn.setReadTimeout(15000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty(SDK_API_KEY_KEY, parameters.get(SDK_API_KEY_KEY));
            conn.setRequestProperty(SDK_HEADER_KEY, parameters.get(SDK_HEADER_KEY));

        } catch (IOException e) {
            e.printStackTrace();
        }
        return conn;
    }
}

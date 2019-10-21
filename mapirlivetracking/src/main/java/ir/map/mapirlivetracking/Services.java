package ir.map.mapirlivetracking;

import android.annotation.SuppressLint;
import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;

import javax.net.ssl.HttpsURLConnection;

import ir.map.mapirlivetracking.network.MapirRegistrationResponseListener;
import ir.map.mapirlivetracking.network.MapirSubscriptionResponseListener;
import ir.map.mapirlivetracking.network.model.Data;
import ir.map.mapirlivetracking.network.model.Register;
import ir.map.mapirlivetracking.network.model.Subscription;

import static ir.map.mapirlivetracking.Constants.SERVER_URL;

class Services {
    private MapirRegistrationResponseListener registrationResponseListener;
    private MapirSubscriptionResponseListener subscriptionResponseListener;
    private String errorMessage = null;

    void registerClient(String xApiKey, String imei, String trackId, MapirRegistrationResponseListener listener) {
        errorMessage = null;
        this.registrationResponseListener = listener;
        new RegisterClient().execute(xApiKey, imei, trackId);
    }

    void fetchClient(String xApiKey, String imei, String trackId, MapirSubscriptionResponseListener listener) {
        errorMessage = null;
        this.subscriptionResponseListener = listener;
        new FetchClient().execute(xApiKey, imei, trackId);
    }

    @SuppressLint("StaticFieldLeak")
    private class RegisterClient extends AsyncTask<String, String, String> {

        private HttpURLConnection conn;

        @Override
        protected String doInBackground(String... args) {
            try {
                URL url = new URL(SERVER_URL);
                conn = (HttpURLConnection) url.openConnection();

                conn.setReadTimeout(5000);
                conn.setConnectTimeout(10000);
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("x-api-key", args[0]);

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("device_id", args[1]);
                jsonParam.put("track_id", args[2]);
                jsonParam.put("type", "publisher");

                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter( new OutputStreamWriter(os, "UTF-8"));
                writer.write(jsonParam.toString());
                writer.flush();
                writer.close();
                os.close();

                int responseCode=conn.getResponseCode();
                if (responseCode == HttpsURLConnection.HTTP_CREATED) {

                    BufferedReader in=new BufferedReader( new InputStreamReader(conn.getInputStream()));
                    StringBuffer sb = new StringBuffer();
                    String line;
                    while((line = in.readLine()) != null) {
                        sb.append(line);
                        break;
                    }
                    in.close();
                    return sb.toString();
                } else {
                    errorMessage = "Could Not Get Data : Response Code : " + conn.getResponseCode();
                    registrationResponseListener.onRegisterFailure(new Throwable("Failed To Register"));
                    return null;
                }


            } catch (Exception e) {
                e.printStackTrace();
                if (errorMessage == null)
                    errorMessage = e.getMessage();
                return null;
            } finally {
                conn.disconnect();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null && isJSONValid(result)) {
                try {
                    JSONObject baseObject = new JSONObject(result);
                    JSONObject dataObject = baseObject.getJSONObject("data");
                    Data data = new Data(dataObject.getString("topic"), dataObject.getString("username"), dataObject.getString("password"));
                    Register register = new Register(baseObject.getString("message"), data);

                    registrationResponseListener.onRegisterSuccess(register);
                } catch (JSONException e) {
                    e.printStackTrace();
                    registrationResponseListener.onRegisterFailure(new Throwable(e.getMessage()));
                }
            } else {
                if (errorMessage != null) {
                    registrationResponseListener.onRegisterFailure(new Throwable(errorMessage));
                    errorMessage = null;
                } else {
                    registrationResponseListener.onRegisterFailure(new Throwable("Unknown Problem"));
                }
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class FetchClient extends AsyncTask<String, String, String> {

        private HttpURLConnection conn;

        @Override
        protected String doInBackground(String... args) {
            try {
                URL url = new URL("https://tracking-dev.map.ir/");
                conn = (HttpURLConnection) url.openConnection();

                conn.setReadTimeout(5000);
                conn.setConnectTimeout(10000);
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("x-api-key", args[0]);

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("device_id", args[1]);
                jsonParam.put("track_id", args[2]);
                jsonParam.put("type", "subscriber");

                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter( new OutputStreamWriter(os, "UTF-8"));
                writer.write(jsonParam.toString());
                writer.flush();
                writer.close();
                os.close();

                int responseCode=conn.getResponseCode();
                if (responseCode == HttpsURLConnection.HTTP_CREATED) {

                    BufferedReader in=new BufferedReader( new InputStreamReader(conn.getInputStream()));
                    StringBuffer sb = new StringBuffer();
                    String line;
                    while((line = in.readLine()) != null) {
                        sb.append(line);
                        break;
                    }
                    in.close();
                    return sb.toString();
                } else {
                    errorMessage = "Could Not Get Data : Response Code : " + conn.getResponseCode();
                    subscriptionResponseListener.onSubscribeFailure(new Throwable("Failed To Register"));
                    return null;
                }


            } catch (Exception e) {
                e.printStackTrace();
                if (errorMessage == null)
                    errorMessage = e.getMessage();
                return null;
            } finally {
                conn.disconnect();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null && isJSONValid(result)) {
                try {
                    JSONObject baseObject = new JSONObject(result);
                    JSONObject dataObject = baseObject.getJSONObject("data");
                    Data data = new Data(dataObject.getString("topic"), dataObject.getString("username"), dataObject.getString("password"));
                    Subscription subscription = new Subscription(baseObject.getString("message"), data);

                    subscriptionResponseListener.onSubscribeSuccess(subscription);
                } catch (JSONException e) {
                    e.printStackTrace();
                    subscriptionResponseListener.onSubscribeFailure(new Throwable(e.getMessage()));
                }
            } else {
                if (errorMessage != null) {
                    subscriptionResponseListener.onSubscribeFailure(new Throwable(errorMessage));
                    errorMessage = null;
                } else {
                    subscriptionResponseListener.onSubscribeFailure(new Throwable("Unknown Problem"));
                }
            }
        }
    }

    private boolean isJSONValid(String test) {
        try {
            new JSONObject(test);
        } catch (JSONException ex) {
            // edited, to include @Arthur's comment
            // e.g. in case JSONArray is valid as well...
            try {
                new JSONArray(test);
            } catch (JSONException ex1) {
                return false;
            }
        }
        return true;
    }
}

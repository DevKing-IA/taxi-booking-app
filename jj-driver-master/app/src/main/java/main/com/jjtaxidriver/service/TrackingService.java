package main.com.jjtaxidriver.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import main.com.jjtaxidriver.R;
import main.com.jjtaxidriver.activity.LoginAct;
import main.com.jjtaxidriver.app.Config;
import main.com.jjtaxidriver.constant.BaseUrl;
import main.com.jjtaxidriver.constant.GPSTracker;
import main.com.jjtaxidriver.constant.MySession;
import main.com.jjtaxidriver.utils.NotificationUtils;



/**
 * Created by technorizen on 4/5/17.
 */

public class TrackingService extends Service implements LocationListener {
    private static final String TAG = "MyLocationService";
    private LocationManager mLocationManager = null;
    private static final int LOCATION_INTERVAL = 3000;
    private static final float LOCATION_DISTANCE = 10f;



    GPSTracker gps;
    NotificationManager notificationManager;
    CountDownTimer yourCountDownTimer;
    int mStartMode;
    double latitude, longitude;
    private MySession mySession;
    IBinder mBinder;
    String user_id="",register_id="";
    boolean mAllowRebind;
    private String user_log_data="";
    Location location;
 //   private String driverid="";
    @Override
    public void onCreate() {
        Log.e("hello", "ssssss");



        mySession = new MySession(this);
        user_log_data = mySession.getKeyAlldata();
        if (user_log_data == null) {

        } else {
            try {
                JSONObject jsonObject = new JSONObject(user_log_data);
                String message = jsonObject.getString("status");
                if (message.equalsIgnoreCase("1")) {
                    JSONObject jsonObject1 = jsonObject.getJSONObject("result");
                    user_id = jsonObject1.getString("id");
                    //register_id = jsonObject1.getString("register_id");
                   }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        int NOTIFICATION_ID = (int) (System.currentTimeMillis()%10000);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            /*startForeground(NOTIFICATION_ID, new Notification.Builder(this).build());*/
                notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                String channelId = getString(R.string.app_name);
                NotificationChannel notificationChannel = new NotificationChannel(channelId, channelId, NotificationManager.IMPORTANCE_DEFAULT);
                notificationChannel.setDescription(channelId);
                notificationChannel.setSound(null, null);

                notificationManager.createNotificationChannel(notificationChannel);


                Notification notification = new Notification.Builder(this, channelId)
                        .setContentTitle(getString(R.string.app_name))
                        .setContentText("Connected through SDL")
                        .build();
            startForeground(NOTIFICATION_ID, notification );
        }
        initializeLocationManager();

        try {
            if (mLocationManager!=null){
                Log.e("COME ","GPS");
                if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                    mLocationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            LOCATION_INTERVAL,
                            LOCATION_DISTANCE,
                            mLocationListeners[0]
                    );

                }
                else if (mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
                    Log.e("COME ","NETWORK");
                    mLocationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            LOCATION_INTERVAL,
                            LOCATION_DISTANCE,
                            mLocationListeners[0]
                    );

                }
                else {
                    Log.e("COME ","ELSE");
                    mLocationManager.requestLocationUpdates(
                            LocationManager.PASSIVE_PROVIDER,
                            LOCATION_INTERVAL,
                            LOCATION_DISTANCE,
                            mLocationListeners[0]
                    );

                }
            }


        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "network provider does not exist, " + ex.getMessage());
        }
        gps = new GPSTracker(getApplicationContext());
        //   getActivity().registerReceiver(broadcastReceiver, new IntentFilter("driver_alloted"));

        if (gps.canGetLocation()) {

            latitude = gps.getLatitude();
            longitude = gps.getLongitude();

            Log.e("lat===", "" + latitude);
            Log.e("lon===", "" + longitude);
        }
        BackGroundFunction();

    }


    private void BackGroundFunction() {
        TimerTask timerTask2 = new TimerTask() {
            @Override
            public void run() {


                if (mySession.IsLoggedIn()) {


                    user_log_data = mySession.getKeyAlldata();
                    if (user_log_data == null) {

                    } else {
                        try {
                            JSONObject jsonObject = new JSONObject(user_log_data);
                            String message = jsonObject.getString("status");
                            if (message.equalsIgnoreCase("1")) {
                                JSONObject jsonObject1 = jsonObject.getJSONObject("result");

                                //register_id = jsonObject1.getString("register_id");
                                user_id = jsonObject1.getString("id");
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                    if (user_id==null||user_id.equalsIgnoreCase("")){

                    }
                    else {
                        SharedPreferences pref = getApplicationContext().getSharedPreferences(Config.SHARED_PREF, 0);
                        register_id= pref.getString("regId", null);

                        Log.e("TRACK >>>prof", " >> " + register_id);
                        new ChgStatus().execute();
                    }



                }



            }
        };

        Timer mTimer2 = new Timer();
        mTimer2.schedule(timerTask2, 5000, 120000);
    }

    private void initializeLocationManager() {
        Log.e("Track", "initializeLocationManager - LOCATION_INTERVAL: "+ LOCATION_INTERVAL + " LOCATION_DISTANCE: " + LOCATION_DISTANCE);
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }

    /**
     * The service is starting, due to a call to startService()
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {




        //   }
        return mStartMode;
    }

    /**
     * A client is binding to the service with bindService()
     */

    @Override
    public void onLocationChanged(Location locations) {
location = locations;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }


    private void sendNotification(String result) {
        // if (!(NotificationUtils.isAppIsInBackground(getApplicationContext()))){
        if ((NotificationUtils.isAppIsInBackground(getApplicationContext()))) {

            Intent intent = new Intent(this, LoginAct.class);
            intent.putExtra("message", result);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            int requestCode = 0;
            PendingIntent pendingIntent = PendingIntent.getActivity(this, requestCode, intent, PendingIntent.FLAG_ONE_SHOT);
            Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                String channelId = getString(R.string.app_name);

                NotificationChannel notificationChannel = new NotificationChannel(channelId, channelId, NotificationManager.IMPORTANCE_DEFAULT);
                notificationChannel.setDescription(channelId);
                notificationChannel.setSound(null, null);

                NotificationCompat.Builder noBuilder = new NotificationCompat.Builder(this, channelId )
                        .setSmallIcon(R.drawable.logo)
                        .setContentText("Your Login Session is expire,Please login again")
                        .setAutoCancel(true)
                        .setContentTitle(getResources().getString(R.string.app_name))
                        .setSound(sound)
                        .setContentIntent(pendingIntent);

                notificationManager.notify(0, noBuilder.build()); //0 = ID of notification
            }
            // removeNotification(0);
            mySession.logoutUser();

            Log.e("", "");
        } else {


        }
    }
    private class ChgStatus extends AsyncTask<String, String, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            try {
                super.onPreExecute();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        @Override
        protected String doInBackground(String... strings) {
//http://mobileappdevelop.co/NAXCAN/webservice/online_change?user_id=34&status=online&register_id=1425
            try {
                String postReceiverUrl = BaseUrl.baseurl + "online_change?";
                URL url = new URL(postReceiverUrl);
                Map<String, Object> params = new LinkedHashMap<>();
                params.put("user_id", user_id);
                params.put("status", "ONLINE");
                params.put("register_id", register_id);
                StringBuilder postData = new StringBuilder();
                for (Map.Entry<String, Object> param : params.entrySet()) {
                    if (postData.length() != 0) postData.append('&');
                    postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
                    postData.append('=');
                    postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
                }
                String urlParameters = postData.toString();
                URLConnection conn = url.openConnection();
                conn.setDoOutput(true);
                OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
                writer.write(urlParameters);
                writer.flush();
                String response = "";
                String line;
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line = reader.readLine()) != null) {
                    response += line;
                }
                writer.close();
                reader.close();
                Log.e("Online Back Res", ">>>>>>>>>>>>" + response);
                return response;
            } catch (UnsupportedEncodingException e1) {

                e1.printStackTrace();
            } catch (IOException e1) {

                e1.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result == null) {
            } else if (result.isEmpty()) {

            } else {
                try {
                    JSONArray jsonArray = new JSONArray(result);
                    for (int i=0;i<jsonArray.length();i++){
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        if (jsonObject.getString("status").equalsIgnoreCase("notmach")) {
                            Log.e("COME > state ---notmach","--------------------Yes");
                            Intent j = new Intent("New Request");
                            j.putExtra("result", "notmach");
                            sendBroadcast(j);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                sendNotification(result);
                            } else {
                                sendNotification(result);
                            }




                        }

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }


        }
    }
    private class LocationListener implements android.location.LocationListener {
        Location mLastLocation;

        public LocationListener(String provider) {
            Log.e("Track ", "LocationListener " + provider);
            mLastLocation = new Location(provider);
        }

        @Override
        public void onLocationChanged(Location location) {
            Log.e("Track ", "onLocationChanged: " + location);
            if (mLastLocation==null){

            }
            else {
                mLastLocation.set(location);
                String latitudes= String.valueOf(location.getLatitude());
                String longitudes= String.valueOf(location.getLongitude());
                new GetDriverLat().execute(latitudes,longitudes);
            }


        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.e("Track ", "onProviderDisabled: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.e("Track ", "onProviderEnabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.e("Track ", "onStatusChanged: " + provider);
        }
    }

    LocationListener[] mLocationListeners = new LocationListener[]{
            new LocationListener(LocationManager.PASSIVE_PROVIDER)
    };

    /**
     * A client is binding to the service with bindService()
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * Called when all clients have unbound with unbindService()
     */
    @Override
    public boolean onUnbind(Intent intent) {
        return mAllowRebind;
    }

    /**
     * Called when a client is binding to the service with bindService()
     */
    @Override
    public void onRebind(Intent intent) {

    }

    /**
     * Called when The service is no longer used and is being destroyed
     */
    @Override
    public void onDestroy() {

        try {
            stopForeground(true);
        }
        catch (Exception e){

        }

        if (mLocationManager != null) {
            for (int i = 0; i < mLocationListeners.length; i++) {
                try {
                    if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    mLocationManager.removeUpdates(mLocationListeners[i]);
                } catch (Exception ex) {
                    Log.i(TAG, "fail to remove location listener, ignore", ex);
                }
            }
        }
    }



    private class GetDriverLat extends AsyncTask<String, String, String> {


        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            try {
                super.onPreExecute();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        protected String doInBackground(String... strings) {
//http://mobileappdevelop.co/NAXCAN/webservice/update_lat_lon?lat=123&lon=321&user_id=1
            try {
                String postReceiverUrl = BaseUrl.baseurl + "update_lat_lon?";
                URL url = new URL(postReceiverUrl);
                Map<String, Object> params = new LinkedHashMap<>();
                params.put("user_id", user_id);
                params.put("lat", strings[0]);
                params.put("lon", strings[1]);



                StringBuilder postData = new StringBuilder();
                for (Map.Entry<String, Object> param : params.entrySet()) {
                    if (postData.length() != 0) postData.append('&');
                    postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
                    postData.append('=');
                    postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
                }
                String urlParameters = postData.toString();
                URLConnection conn = url.openConnection();
                conn.setDoOutput(true);
                OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
                writer.write(urlParameters);
                writer.flush();
                String response = "";
                String line;
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line = reader.readLine()) != null) {
                    response += line;
                }
                writer.close();
                reader.close();
                Log.e("Update Tracking Lat", ">>>>>>>>>>>>" + response);
                return response;
            } catch (UnsupportedEncodingException e1) {

                e1.printStackTrace();
            } catch (IOException e1) {

                e1.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            if (result == null) {

            } else if (result.isEmpty()) {


            } else {

            }


        }
    }
}
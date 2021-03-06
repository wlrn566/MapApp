package com.wlrn566.gpsTracker.Service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.wlrn566.gpsTracker.Activity.MainActivity;
import com.wlrn566.gpsTracker.R;

public class FusedLocationService extends Service {
    private double latitude, longitude;
    private String provider;
    private float accuracy;
    private LocationManager locationManager;
    private String TAG = getClass().getName();
    private FusedLocationProviderClient fusedLocationClient;
    private String action;
    private Thread updateThread;

    public FusedLocationService() {
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Log.d(TAG, "onStartCommand");
        if (intent != null) {
            action = intent.getAction();
            if (action != null) {
                if (action.equals(MainActivity.Constants.ACTION_PUSH_LOCATION_SERVICE) || action.equals(MainActivity.Constants.ACTION_REALTIME_LOCATION_SERVICE)) {
                    startLocation();
                } else if (action.equals(MainActivity.Constants.ACTION_STOP_LOCATION_SERVICE)) {
                    stopLocation();
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startLocation() {
        Log.d(TAG, "FusedLocationService start");
        // fusedLocation ??????
        try {
            // ?????? ??????
            setNotification();

            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setInterval(5000);
            locationRequest.setFastestInterval(3000);
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            LocationServices.getFusedLocationProviderClient(this).requestLocationUpdates(locationRequest, mLocationCallback, Looper.getMainLooper());
        } catch (SecurityException e) {
            Log.d(TAG, "error = " + e);
        }
    }

    private void stopLocation() {
        Log.d(TAG, "FusedLocationService stop");
        LocationServices.getFusedLocationProviderClient(this).removeLocationUpdates(mLocationCallback);

        // ????????? ??????
        if (updateThread != null) {
            Log.d(TAG, "updateThread stop");
            updateThread.interrupt();
        } else {
            Log.d(TAG, "updateThread null");
        }
    }

    private void setNotification() {
//        String message = "????????? : " + provider + "\n" + "?????? : " + latitude + "\n" + "?????? : " + longitude + "\n" + "????????? : " + accuracy;
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "location_notification_channel";
        String channelName = "Location Service";

        // ?????? ??????
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId);

        // ????????? ?????? ??? ????????? ???????????? -----------------------------------------------------------------------------------------
        Intent intent = new Intent(this, MainActivity.class)
                .setAction(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
//        intent.putExtra("latitude", latitude);
//        intent.putExtra("longitude", longitude);
//        intent.putExtra("accuracy", accuracy);
//        intent.putExtra("provider", provider);
        // ------------------------------------------------------------------------------------------------------------------
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        builder.setContentIntent(pendingIntent)  /* ?????? ??? Intent ?????? */
                .setSmallIcon(R.mipmap.ic_launcher)  /* ?????? ????????? (??????) */
                .setContentTitle("Location Service")  /* ?????? ?????? */
                .setDefaults(NotificationCompat.PRIORITY_LOW)  /* ?????? */
//                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))  /* ?????? ?????? ?????? ?????? */
                .setContentText("Gps On")  /* ?????? ?????? */
                .setAutoCancel(true)  /* ?????? ??? ?????? ?????? */
                .setPriority(NotificationCompat.PRIORITY_LOW);  /* ????????? */

        //Oreo ??????(API26 ??????)??????????????? ???????????? NotificationChannel ????????? ????????? ?????? ??????????????? ???.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager != null && notificationManager.getNotificationChannel(channelId) == null) {
                NotificationChannel notificationChannel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
                notificationChannel.setVibrationPattern(new long[]{0});  /* ?????? */
                notificationChannel.enableVibration(true);  /* ?????? */
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }

        //??????????????? ?????? ?????? ???????????????
        Notification notification = builder.build();

        // ?????????????????? ????????? : ????????? ??????????????? ???????????? ????????????????????? ?????? ?????? ??????(?????????) / ????????? ?????? ????????? ??????????????????
        // ??????????????? ???????????? ????????? ????????? ?????? -> work Manager ??? Alarm Manager ??? ????????? ??????????????? ?????????
        // ?????? ???????????? ?????? ?????????(??????) ?????????(???) ?????? ??????
        startForeground(1, notification);
    }

    // Google gps
    private LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationAvailability(@NonNull LocationAvailability locationAvailability) {
            super.onLocationAvailability(locationAvailability);
        }

        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            super.onLocationResult(locationResult);
            if (locationResult != null && locationResult.getLastLocation() != null) {
                latitude = locationResult.getLastLocation().getLatitude();
                longitude = locationResult.getLastLocation().getLongitude();
                provider = locationResult.getLastLocation().getProvider();
                accuracy = locationResult.getLastLocation().getAccuracy();
                Log.d(TAG, "????????? : " + provider + " / ?????? : " + latitude + " / ?????? : " + longitude + " / ????????? : " + accuracy);

                // ?????? ????????????
                if (action.equals(MainActivity.Constants.ACTION_REALTIME_LOCATION_SERVICE)) {
                    // ???????????? ???????????? ??????????????? ???????????? ??????
                    updateThread = new updateThread(provider, latitude, longitude, accuracy);
                    updateThread.start();
                }
            }
        }
    };

    // ?????? ?????? ?????????
    class updateThread extends Thread {
        private String provider;
        double latitude;
        double longitude;
        float accuracy;

        updateThread(String provider, double latitude, double longitude, float accuracy) {
            this.provider = provider;
            this.latitude = latitude;
            this.longitude = longitude;
            this.accuracy = accuracy;
        }

        public void run() {
            Log.d(TAG, "updateThread start");

            // ??????????????? ????????????????????? ????????? ?????????
            Intent intent = new Intent();
            intent.setAction("update");
            intent.putExtra("provider", provider);
            intent.putExtra("latitude", latitude);
            intent.putExtra("longitude", longitude);
            intent.putExtra("accuracy", accuracy);
            sendBroadcast(intent);
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Log.e(TAG, e.toString());
            }
        }
    }
}
package com.wlrn566.pushapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.concurrent.Executor;

public class GpsService2 extends Service {
    private double latitude, longitude;
    private String provider;
    private float accuracy;
    private LocationManager locationManager;
    private String TAG = getClass().getName();
    private FusedLocationProviderClient fusedLocationClient;

    public GpsService2() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    // Location 콜백 리스너 클래스
    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            double latitude = location.getLatitude();  // 위도
            double longitude = location.getLongitude();  // 경도
            float accuracy = location.getAccuracy();
            String provider = location.getProvider();
            Log.d(TAG, "제공자 : " + provider + " / 위도 : " + latitude + " / 경도 : " + longitude + " / 정확도 : " + accuracy);
            sendNotification(provider, latitude, longitude, accuracy);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.d(TAG, "onStatusChanged -> provider : " + provider + " / status : " + status + " / Bundle extras : " + extras);
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {
            System.out.println("onProviderEnabled : " + provider);
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
            System.out.println("onProviderDisabled : " + provider);
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Log.d(TAG, "GpsService onStartCommand");
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                if (action.equals(MainActivity.Constants.ACTION_START_LOCATION_SERVICE)) {

                    fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

                    startLocation();
                } else if (action.equals(MainActivity.Constants.ACTION_STOP_LOCATION_SERVICE)) {
                    stopLocation();
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void startLocation() {
        Log.d(TAG, "GpsService startLocation On");
        // GSP 제공자의 변경에 따른 콜백 리스너 등록 (제공자, 갱신 최소시간, 갱신 최소거리, 리스너)
        try {
            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setInterval(4000);
            locationRequest.setFastestInterval(2000);
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            LocationServices.getFusedLocationProviderClient(this).requestLocationUpdates(locationRequest, mLocationCallback, Looper.getMainLooper());

//            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 1, mLocationListener);
//            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 1, mLocationListener);
        } catch (SecurityException e) {
            Log.d(TAG, "error = " + e);
        }
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
                double latitude = locationResult.getLastLocation().getLatitude();
                double longitude = locationResult.getLastLocation().getLongitude();
                String provider = locationResult.getLastLocation().getProvider();
                float accuracy = locationResult.getLastLocation().getAccuracy();
                Log.d(TAG, "제공자 : " + provider + " / 위도 : " + latitude + " / 경도 : " + longitude + " / 정확도 : " + accuracy);

                sendNotification(provider, latitude, longitude, accuracy);

            }
        }
    };

    private void sendNotification(String provider, double latitude, double longitude, float accuracy) {
        String message = "제공자 : " + provider + "\n" + "위도 : " + latitude + "\n" + "경도 : " + longitude + "\n" + "정확도 : " + accuracy;
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "location_notification_channel";
        String channelName = "Location Service";
        // 알림창 클릭 시 실행할 액티비티 -----------------------------------------------------------------------------------------
        Intent intent = new Intent(this, SplashActivity.class)
                .setAction(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId);
        builder.setSmallIcon(R.mipmap.ic_launcher);  // 알림 아이콘 (상단)
        builder.setContentTitle("Location Service");  // 제목
        builder.setDefaults(NotificationCompat.DEFAULT_ALL);  // 진동
        builder.setStyle(new NotificationCompat.BigTextStyle().bigText(message));  // 알림 내용 개행 지원
//        builder.setContentText(message);  // 내용
        builder.setContentIntent(pendingIntent);  // 클릭 시 Intent 호출
        builder.setAutoCancel(false);
        builder.setPriority(NotificationCompat.PRIORITY_MAX);  // 중요도

        //Oreo 버전(API26 버전)이상에서는 알림시에 NotificationChannel 이라는 개념이 필수 구성요소가 됨.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager != null && notificationManager.getNotificationChannel(channelId) == null) {
                NotificationChannel notificationChannel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
//                notificationChannel.setDescription("This channel is used by location service");
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }
        //건축가에게 알림 객체 생성하도록
        Notification notification = builder.build();

        //알림매니저에게 알림(Notify) 요청
        notificationManager.notify(1, notification);

    }

    private void stopLocation() {
        Log.d(TAG, "GpsService stopLocation On");
        locationManager.removeUpdates(mLocationListener);
    }
}
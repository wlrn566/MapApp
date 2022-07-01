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
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

public class LocationManagerService extends Service {
    private double latitude, longitude;
    private String provider;
    private float accuracy;
    private LocationManager locationManager;
    private String TAG = getClass().getName();
    boolean click_b;

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    // Location 콜백 리스너 클래스
    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            String provider = location.getProvider();  // 제공자
            double latitude = location.getLatitude();  // 위도
            double longitude = location.getLongitude();  // 경도
            float accuracy = location.getAccuracy();  // 정확도

            Log.d(TAG, "제공자 : " + provider + " / 위도 : " + latitude + " / 경도 : " + longitude + " / 정확도 : " + accuracy);
            sendNotification(latitude, longitude, accuracy, provider);
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

    private void sendNotification(double latitude, double longitude, float accuracy, String provider) {
        String message = "제공자 : " + provider + "\n" + "위도 : " + latitude + "\n" + "경도 : " + longitude + "\n" + "정확도 : " + accuracy;
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "location_update_notification_channel";
        String channelName = "Location Service";
        // 알림창 클릭 시 실행할 액티비티 -----------------------------------------------------------------------------------------
        Intent intent = new Intent(this, MainActivity.class)
                .setAction(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("latitude", latitude);
        intent.putExtra("longitude", longitude);
        intent.putExtra("accuracy", accuracy);
        intent.putExtra("provider", provider);
//        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);   // 브로드캐스트리시브로 데이터 넘기기 (실패)
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId);
        builder.setContentIntent(pendingIntent)  /* 클릭 시 Intent 호출 */
                .setSmallIcon(R.mipmap.ic_launcher)  /* 알림 아이콘 (상단) */
                .setContentTitle("Location Service")  /* 알림 제목 */
                .setDefaults(NotificationCompat.DEFAULT_ALL)  /* 진동 */
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))  /* 알림 내용 개행 지원 */
                .setAutoCancel(true)  /* 클릭 시 알림 삭제 */
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);  /* 중요도 */

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

        // 포어그라운드 서비스 : 사용자 인터페이스 제공없이 백그라운드에서 작업 수행 가능(서비스) / 노티로 실행 중임을 노출해줘야함
        // 백그라운드 서비스는 실행에 제한이 많음 -> work Manager나 Alarm Manager 를 이용해 예약작업을 해야함
        // 앱과 상호작용 하고 있는지(포어) 없는지(백) 으로 구분
        startForeground(1, notification);

//        //알림매니저에게 알림(Notify) 요청
//        notificationManager.notify(1, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Log.d(TAG, "LocationManagerService onStartCommand");
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                if (action.equals(MainActivity.Constants.ACTION_PUSH_LOCATION_SERVICE)) {
                    startLocation();
                } else if (action.equals(MainActivity.Constants.ACTION_STOP_LOCATION_SERVICE)) {
                    stopLocation();
                }
            }
        }
        return START_STICKY;
    }

    private void startLocation() {
        Log.d(TAG, "LocationManagerService start");
        // GSP 제공자의 변경에 따른 콜백 리스너 등록 (제공자, 갱신 최소시간, 갱신 최소거리, 리스너)
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 1, mLocationListener);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 1, mLocationListener);
        } catch (SecurityException e) {
            Log.d(TAG, "error = " + e);
        }
    }

    private void stopLocation() {
        Log.d(TAG, "LocationManagerService stop");
        locationManager.removeUpdates(mLocationListener);
    }


    private void setNotification() {
        String message = "Location Service On";
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "location_notification_channel";
        String channelName = "Location Service";
        // 알림창 클릭 시 실행할 액티비티 -----------------------------------------------------------------------------------------
        Intent intent = new Intent(this, MainActivity.class)
                .setAction(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("latitude", latitude);
        intent.putExtra("longitude", longitude);
        intent.putExtra("accuracy", accuracy);
        intent.putExtra("provider", provider);
        intent.putExtra("click_b", click_b);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId);
        builder.setSmallIcon(R.mipmap.ic_launcher);  // 알림 아이콘 (상단)
        builder.setContentTitle("Location Service");  // 제목
        builder.setDefaults(NotificationCompat.DEFAULT_ALL);  // 진동
        builder.setContentText(message);  // 내용
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
        // 포어그라운드 서비스 : 사용자 인터페이스 제공없이 백그라운드에서 작업 수행 가능(서비스) / 노티로 실행 중임을 노출해줘야함
        // 백그라운드 서비스는 실행에 제한이 많음 -> work Manager나 Alarm Manager 를 이용해 예약작업을 해야함
        // 앱과 상호작용 하고 있는지(포어) 없는지(백) 으로 구분
        startForeground(1, builder.build());
    }

}
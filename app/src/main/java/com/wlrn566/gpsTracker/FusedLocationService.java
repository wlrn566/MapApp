package com.wlrn566.gpsTracker;

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
        // fusedLocation 실행
        try {
            // 알림 표시
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

        // 스레드 종료
        if (updateThread != null) {
            Log.d(TAG, "updateThread stop");
            updateThread.interrupt();
        } else {
            Log.d(TAG, "updateThread null");
        }
    }

    private void setNotification() {
//        String message = "제공자 : " + provider + "\n" + "위도 : " + latitude + "\n" + "경도 : " + longitude + "\n" + "정확도 : " + accuracy;
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "location_notification_channel";
        String channelName = "Location Service";

        // 알림 생성
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId);

        // 알림창 클릭 시 실행할 액티비티 -----------------------------------------------------------------------------------------
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
        builder.setContentIntent(pendingIntent)  /* 클릭 시 Intent 호출 */
                .setSmallIcon(R.mipmap.ic_launcher)  /* 알림 아이콘 (상단) */
                .setContentTitle("Location Service")  /* 알림 제목 */
                .setDefaults(NotificationCompat.PRIORITY_LOW)  /* 진동 */
//                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))  /* 알림 내용 개행 지원 */
                .setContentText("Gps On")  /* 알림 내용 */
                .setAutoCancel(true)  /* 클릭 시 알림 삭제 */
                .setPriority(NotificationCompat.PRIORITY_LOW);  /* 중요도 */

        //Oreo 버전(API26 버전)이상에서는 알림시에 NotificationChannel 이라는 개념이 필수 구성요소가 됨.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager != null && notificationManager.getNotificationChannel(channelId) == null) {
                NotificationChannel notificationChannel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
                notificationChannel.setVibrationPattern(new long[]{0});  /* 진동 */
                notificationChannel.enableVibration(true);  /* 진동 */
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }

        //건축가에게 알림 객체 생성하도록
        Notification notification = builder.build();

        // 포어그라운드 서비스 : 사용자 인터페이스 제공없이 백그라운드에서 작업 수행 가능(서비스) / 노티로 실행 중임을 노출해줘야함
        // 백그라운드 서비스는 실행에 제한이 많음 -> work Manager 나 Alarm Manager 를 이용해 예약작업을 해야함
        // 앱과 상호작용 하고 있는지(포어) 없는지(백) 으로 구분
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
                Log.d(TAG, "제공자 : " + provider + " / 위도 : " + latitude + " / 경도 : " + longitude + " / 정확도 : " + accuracy);

                // 알림 분기처리
                if (action.equals(MainActivity.Constants.ACTION_REALTIME_LOCATION_SERVICE)) {
                    // 스레드를 이용해서 액티비티에 데이터를 넘김
                    updateThread = new updateThread(provider, latitude, longitude, accuracy);
                    updateThread.start();
                }
            }
        }
    };

    // 위치 갱신 스레드
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

            // 액티비티의 브로드캐스트에 데이터 보내기
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
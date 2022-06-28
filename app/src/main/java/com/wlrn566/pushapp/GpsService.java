package com.wlrn566.pushapp;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;

public class GpsService extends Service {
    private double latitude, longitude;
    private LocationManager locationManager;
    private String TAG = getClass().getName();

    public GpsService() {
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
            latitude = location.getLatitude();  // 위도
            longitude = location.getLongitude();  // 경도
            float accuracy = location.getAccuracy();
            String provider = location.getProvider();
            System.out.println("제공자 : " + provider + " / 위도 : " + latitude + " / 경도 : " + longitude + " / 정확도 : " + accuracy);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            System.out.println("onStatusChanged -> provider : " + provider + " / status : " + status + " / Bundle extras : " + extras);
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
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 1, mLocationListener);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 1, mLocationListener);
        } catch (SecurityException e) {
            Log.d(TAG, "error = " + e);
        }
    }

    private void stopLocation() {
        Log.d(TAG, "GpsService stopLocation On");
        locationManager.removeUpdates(mLocationListener);
    }
}
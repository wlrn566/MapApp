package com.wlrn566.pushapp;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapView;

import org.w3c.dom.Text;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity2 extends AppCompatActivity {
    private String TAG = getClass().getName();
    private Location location;
    private LocationManager locationManager;
    private List<String> listProviders;
    private static final int GPS_ENABLE_REQUEST_CODE = 2001, PERMISSIONS_REQUEST_CODE = 100;
    private long backPressedTime = 0;
    private String[] REQUIRED_PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION
            , Manifest.permission.ACCESS_COARSE_LOCATION};
    private MapView mapView;
    private ViewGroup mapViewContainer;
    private TextView add_tv, lat_tv, lng_tv, accuracy_tv;
    private TextView provider_tv;
    private double latitude, longitude;
    private ArrayList<MapPOIItem> markerList = new ArrayList<>();
    private MapPOIItem marker;
    boolean click_b = false;

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_main);

        // 지도 띄우기
        mapView = new MapView(this);
        mapViewContainer = (ViewGroup) findViewById(R.id.map_view);
        mapViewContainer.addView(mapView);

        add_tv = findViewById(R.id.add_tv);
        provider_tv = findViewById(R.id.provider_tv);
        lat_tv = findViewById(R.id.lat_tv);
        lng_tv = findViewById(R.id.lng_tv);
        accuracy_tv = findViewById(R.id.accuracy_tv);
        Button gps = (Button) findViewById(R.id.gps);
        Button setCenter = (Button) findViewById(R.id.setCenter);
        Button fragment = (Button) findViewById(R.id.fragment);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        gps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!click_b) {
                    click_b = true;
                    gps.setText("ON");
                    // 권한  체크
                    if (!checkLocationServiceStatus()) {  // GPS
                        Log.d(TAG, "check Location Permission");
                        showDialogForLocationServiceSetting();
                    } else {
                        checkRunTimePermission();
                    }
                    Log.d(TAG, "gps start");
                    // GSP 제공자의 변경에 따른 콜백 리스터 등록 (제공자, 갱신 최소시간, 갱신 최소거리, 리스너)
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 1, mLocationListener);
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 1, mLocationListener);
                } else {
                    Log.d(TAG, "gps end");
                    gps.setText("OFF");
                    click_b = false;
                    locationManager.removeUpdates(mLocationListener);
                }
            }

        });
        setCenter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "center latitude : " + latitude + " / longitude : " + longitude);
                mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(latitude, longitude), true);
                mapView.setZoomLevel(1, true);
            }
        });

        fragment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainFragment mainFragment = new MainFragment();
                getSupportFragmentManager().beginTransaction().replace(R.id.main, mainFragment, "map").commit();
            }
        });
    }

    private void setInfo(double latitude, double longitude) {
        String address = getCurrentAddress(latitude, longitude);
        add_tv.setText("현재 주소 : " + address);
        lat_tv.setText("현재 위도 : " + String.valueOf(latitude));
        lng_tv.setText("현재 경도 : " + String.valueOf(longitude));

        // 기존에 마커가 찍혀져 있으면 지우고 다시 생성
        if (markerList.size() > 1) {
            for (int i = 0; i < markerList.size(); i++) {
                mapView.removePOIItem(markerList.get(i));
            }
            updateMarker(latitude, longitude);
        } else {
            setMarker(latitude, longitude);
        }
    }

    private void setMarker(double latitude, double longitude) {
        Log.d(TAG, "setMarker");
        // 중심점 변경
        mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(latitude, longitude), true);

        // 줌 레벨 변경 - 낮을 수록 가까워짐
        mapView.setZoomLevel(3, true);

        // 줌 인
        mapView.zoomIn(true);

        // 줌 아웃
        mapView.zoomOut(true);
        marker = new MapPOIItem();
        marker.setItemName("Default Marker");
        marker.setTag(0);
        marker.setMapPoint(MapPoint.mapPointWithGeoCoord(latitude, longitude));
        marker.setMarkerType(MapPOIItem.MarkerType.BluePin); // 기본으로 제공하는 BluePin 마커 모양.

        // 마커 관리를 위해 리스트에 추가
        mapView.addPOIItem(marker);
        markerList.add(marker);
    }

    public void updateMarker(double latitude, double longitude) {
        Log.d(TAG, "marker update success");

        marker = new MapPOIItem();
        marker.setItemName("Default Marker");
        marker.setTag(0);
        marker.setMapPoint(MapPoint.mapPointWithGeoCoord(latitude, longitude));
        marker.setMarkerType(MapPOIItem.MarkerType.BluePin); // 기본으로 제공하는 BluePin 마커 모양.

        // 마커 관리를 위해 리스트에 추가
        mapView.addPOIItem(marker);
        markerList.add(marker);
    }

    void checkRunTimePermission() {
        // 위치 권한 있는지 확인하기
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(MainActivity2.this, Manifest.permission.ACCESS_FINE_LOCATION);
        int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(MainActivity2.this, Manifest.permission.ACCESS_COARSE_LOCATION);
        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED && hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED) {
            // 가지고 있다면 위치값 가져올 수 있음
            Log.d(TAG, "PERMISSION_GRANTED");
            Log.d(TAG, "hasFineLocationPermission " + hasFineLocationPermission + " hasCoarseLocationPermission " + hasCoarseLocationPermission);

        } else {  // 권한을 요청한적이 있는데 거부한적이 있음
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity2.this, REQUIRED_PERMISSIONS[0])) {  // 거부한 적 있을 때
                Toast.makeText(MainActivity2.this, "이 앱을 실행하려면 위치 접근 권한이 필요합니다.", Toast.LENGTH_LONG).show();  // 요청 이유
                ActivityCompat.requestPermissions(MainActivity2.this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE);  // 요청
            } else {  // 처음 요청 시
                ActivityCompat.requestPermissions(MainActivity2.this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE);  // 요청
            }
        }
    }

    // checkRunTimePermission의 request 결과값
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        // 요청코드가 맞고 요청개수만큼 request 되었을 때
        if (requestCode == PERMISSIONS_REQUEST_CODE && grantResults.length == REQUIRED_PERMISSIONS.length) {
            boolean check_result = true;
            // 모든 권한을 허용했는지 확인
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    check_result = false;
                    break;
                }
            }
            if (check_result) {
                // 위치 값 가져오기 가능
                Log.d(TAG, "permission success");
            } else {
                // 거부된 권한이 있을 때
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0]) || ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[1])) {
                    Toast.makeText(MainActivity2.this, "권한이 거부 되었습니다. 다시 실행해주세요", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(MainActivity2.this, "권한이 거부 되었습니다. 다시 실행해주세요", Toast.LENGTH_LONG).show();
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public String getCurrentAddress(double latitude, double longitude) {
        //Geocoder - GPS를 주소로 변환
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        List<Address> addresses;
        try {
            addresses = geocoder.getFromLocation(
                    latitude,
                    longitude,
                    7);
        } catch (IOException ioException) {
            //네트워크 문제
            Toast.makeText(this, "Geocoder failed", Toast.LENGTH_LONG).show();
            return "지오코더 서비스 사용불가";
        } catch (IllegalArgumentException illegalArgumentException) {
            Toast.makeText(this, "wrong GPS coordinate", Toast.LENGTH_LONG).show();
            return "잘못된 GPS 좌표";
        }
        if (addresses == null || addresses.size() == 0) {
            Toast.makeText(this, "address undetected", Toast.LENGTH_LONG).show();
            return "주소 미발견";
        }
        Address address = addresses.get(0);
        return address.getAddressLine(0).toString();
    }

    private void showDialogForLocationServiceSetting() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity2.this);
        builder.setTitle("위치 서비스 비활성화");
        builder.setMessage("앱을 사용하기 위해 위치 서비스가 필요합니다.");
        builder.setCancelable(true);
        builder.setPositiveButton("설정", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent callGPSSettingIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        builder.create().show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case GPS_ENABLE_REQUEST_CODE:
                // GPS 활성화 여부 검사
                if (checkLocationServiceStatus()) {
                    if (checkLocationServiceStatus()) {
                        Log.d("Active", "onActivityResult : GPS ON");
                        checkRunTimePermission();
                        return;
                    }
                }
                break;
        }
    }

    public boolean checkLocationServiceStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        mapViewContainer.removeView(mapView);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
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

            provider_tv.setText("현재 제공자 : " + provider);
            accuracy_tv.setText("정확도 : " + accuracy);
            setInfo(longitude, latitude);
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


    // 액티비티 자체에 LocationListener 등록 시
//    private Location setLocationManager() {
//        // 권한 요청
//        checkRunTimePermission();
//
//        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//
//        // 위치를 얻는 provider 에 따라 분기처리
//        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
//        if (location != null) {
//            double lat = location.getLatitude();
//            double lng = location.getLongitude();
//            Log.d(TAG, "lat = " + lat + " lng = " + lng);
//        }
//
//        location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
//        if (location != null) {
//            double lat = location.getLatitude();
//            double lng = location.getLongitude();
//            Log.d(TAG, "lat = " + lat + " lng = " + lng);
//        }
//
//        location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
//        if (location != null) {
//            double lat = location.getLatitude();
//            double lng = location.getLongitude();
//            Log.d(TAG, "lat = " + lat + " lng = " + lng);
//        }
//
//        listProviders = locationManager.getAllProviders();
//        boolean[] isEnable = new boolean[3];
//        for (int i = 0; i < listProviders.size(); i++) {
//            if (listProviders.get(i).equals(LocationManager.GPS_PROVIDER)) {
//                isEnable[0] = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
//                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
//            } else if (listProviders.get(i).equals(LocationManager.NETWORK_PROVIDER)) {
//                isEnable[1] = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
//                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
//            } else if (listProviders.get(i).equals(LocationManager.PASSIVE_PROVIDER)) {
//                isEnable[1] = locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER);
//                locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 0, 0, this);
//            }
//
//        }
//        Log.d(TAG, listProviders.get(0) + " / " + String.valueOf(isEnable[0]));
//        Log.d(TAG, listProviders.get(1) + " / " + String.valueOf(isEnable[1]));
//        Log.d(TAG, listProviders.get(2) + " / " + String.valueOf(isEnable[2]));
//
//        provider_tv1.setText("GPS_PROVIDER : " + isEnable[0]);
//        provider_tv2.setText("NETWORK_PROVIDER : " + isEnable[1]);
//        provider_tv3.setText("PASSIVE_PROVIDER : " + isEnable[2]);
//
//        return location;
//    }

//    @Override
//    public void onLocationChanged(@NonNull Location location) {
//        if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
//            latitude = location.getLatitude();
//            longitude = location.getLongitude();
//            Log.d(TAG, "GPS_PROVIDER GPS = " + Double.toString(latitude) + " / " + Double.toString(longitude));
//        }
//        if (location.getProvider().equals(LocationManager.NETWORK_PROVIDER)) {
//            latitude = location.getLatitude();
//            longitude = location.getLongitude();
//            Log.d(TAG, "NETWORK_PROVIDER GPS = " + Double.toString(latitude) + " / " + Double.toString(longitude));
//        }
//        if (location.getProvider().equals(LocationManager.PASSIVE_PROVIDER)) {
//            latitude = location.getLatitude();
//            longitude = location.getLongitude();
//            Log.d(TAG, "PASSIVE_PROVIDER GPS = " + Double.toString(latitude) + " / " + Double.toString(longitude));
//        }
//        setInfo(latitude, longitude);
//    }
//
//    @Override
//    public void onStatusChanged(String provider, int status, Bundle extras) {
//        Log.d(TAG, "onStatusChanged provider = " + provider + ", status = " + status + ", Bundle extras = " + extras);
//        LocationListener.super.onStatusChanged(provider, status, extras);
//    }
//
//    @Override
//    public void onProviderEnabled(@NonNull String provider) {
//        Log.d(TAG, "onProviderEnabled = " + provider);
//        LocationListener.super.onProviderEnabled(provider);
//    }
//
//    @Override
//    public void onProviderDisabled(@NonNull String provider) {
//        Log.d(TAG, "onProviderDisabled = " + provider);
//        LocationListener.super.onProviderDisabled(provider);
//    }

    // 키해시 값
    private void getAppKeyHash() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md;
                md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                String something = new String(Base64.encode(md.digest(), 0));
                Log.d(TAG, "Hash key = " + something);
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            Log.e(TAG, "name not found" + e.toString());
        }
    }

    @Override
    public void onBackPressed() {
        // 뒤로가기 키를 누른 후 2초가 지났으면 Toast 출력
        if (System.currentTimeMillis() > backPressedTime + 2000) {
            backPressedTime = System.currentTimeMillis();
            Toast toast = Toast.makeText(this, "한번 더 누르시면 종료됩니다.", Toast.LENGTH_SHORT);
            toast.show();
            return;
        }
        // 뒤로가기 키를 누른 후 2초가 안지났으면 종료
        if (System.currentTimeMillis() <= backPressedTime + 2000) {
            finish();
        }
    }
}
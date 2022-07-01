package com.wlrn566.pushapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.location.Address;
import android.location.Geocoder;
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

import net.daum.mf.map.api.MapView;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.List;
import java.util.Locale;

public class MainActivity2 extends AppCompatActivity implements View.OnClickListener {
    private String TAG = getClass().getName();
    private long backPressedTime = 0;
    private String[] REQUIRED_PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION
            , Manifest.permission.ACCESS_COARSE_LOCATION
    };
    private static final int GPS_ENABLE_REQUEST_CODE = 2001, PERMISSIONS_REQUEST_CODE = 100;
    private double latitude;
    private double longitude;
    private float accuracy;
    private String provider;
    private TextView provider_tv, add_tv, lat_tv, lng_tv, accuracy_tv;
    private Button gps_btn;

    private MapView mapView;
    private ViewGroup mapViewContainer;

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }

    public class Constants {
        static final int LOCATION_SERVICE_ID = 175;
        static final String ACTION_START_LOCATION_SERVICE = "startLocationService";
        static final String ACTION_STOP_LOCATION_SERVICE = "stopLocationService";
        static final String ACTION_UPDATE_LOCATION_SERVICE = "updateLocationService";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_main);

        // 알림창에서 값 가져오기
        Intent intent = getIntent();
        if (intent != null) {
            Log.d(TAG, "getIntent not null");
            latitude = intent.getDoubleExtra("latitude", 0);
            longitude = intent.getDoubleExtra("longitude", 0);
            accuracy = intent.getFloatExtra("accuracy", 0);
            provider = intent.getStringExtra("provider");
            Log.d(TAG, "provider = " + provider + " / latitude = " + latitude + " / longitude = " + longitude + " / accuracy = " + accuracy);
        } else {
            Log.d(TAG, "getIntent is null");
        }

        provider_tv = findViewById(R.id.provider_tv);
        add_tv = findViewById(R.id.add_tv);
        lat_tv = findViewById(R.id.lat_tv);
        lng_tv = findViewById(R.id.lng_tv);
        accuracy_tv = findViewById(R.id.accuracy_tv);
        gps_btn = findViewById(R.id.gps_btn);
        gps_btn.setOnClickListener(this);
        if (isGpsServiceRunning(FusedLocationService2.class)) {  // 알림으로 들어왔을 때 Gps 멈춰줌(알림 중지)
            gps_btn.setText("STOP");
            stopLocation();
//            updateLocation();
        } else {  // 처음 실행 또는 알림이 아닌 직접 들어왔을 때
            gps_btn.setText("OFF");
        }
    }

    @SuppressLint("SetTextI18n")
    private void setPage() {
        // 텍스트 출력
        provider_tv.setText("제공자 : " + (provider != null ? provider : "정보없음"));
        add_tv.setText("주소 : " + (latitude != 0 && longitude != 0 ? getCurrentAddress(latitude, longitude) : "정보없음"));
        lat_tv.setText("위도 : " + (latitude != 0 ? latitude : "정보없음"));
        lng_tv.setText("경도 : " + (longitude != 0 ? longitude : "정보없음"));
        accuracy_tv.setText("정확도 : " + (accuracy != 0 ? accuracy : "정보없음"));

        // 지도 띄우기
        mapView = new MapView(this);
        mapViewContainer = (ViewGroup) findViewById(R.id.map_view);
        mapViewContainer.addView(mapView);
    }

    private void startLocation() {
        Intent intent = new Intent(getApplicationContext(), FusedLocationService2.class);
        intent.setAction(Constants.ACTION_START_LOCATION_SERVICE);
        startService(intent);
    }

    private void stopLocation() {
        Intent intent = new Intent(getApplicationContext(), FusedLocationService2.class);
        intent.setAction(Constants.ACTION_STOP_LOCATION_SERVICE);
        startService(intent);
        stopService(intent);
    }

//    // 실시간으로 지도를 움직이기 위해 update 데이터 가져옴
//    private void updateLocation() {
//        Intent intent = new Intent(getApplicationContext(), LocationManagerService.class);
//        stopService(intent);
//        intent.setAction(Constants.ACTION_UPDATE_LOCATION_SERVICE);
//        startService(intent);
//
////        if (isGpsServiceRunning(LocationManagerService.class)) {
////            LocationManagerService gpsService = new LocationManagerService();
////            provider = gpsService.getProvider();
////            latitude = gpsService.getLatitude();
////            longitude = gpsService.getLongitude();
////            accuracy = gpsService.getAccuracy();
////            Log.d(TAG, "update -> provider = " + provider + " / latitude = " + latitude + " / longitude = " + longitude + " / accuracy = " + accuracy);
////        }
//    }

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

    private boolean isGpsServiceRunning(Class<?> serviceClass) {
        try {
            ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            return false;
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        setPage();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        mapViewContainer.removeView(mapView);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.gps_btn:
                Log.d(TAG, "click");
                Log.d(TAG, "isGpsServiceRunning ? " + isGpsServiceRunning(FusedLocationService2.class));
                if (isGpsServiceRunning(FusedLocationService2.class)) {  // Gps 사용중일 때
                    gps_btn.setText("OFF");
                    stopLocation();
                } else {  // Gps 껐을 떄
                    gps_btn.setText("ON");
                    if (!checkLocationServiceStatus()) {  // GPS
                        Log.d(TAG, "go Location Setting");
                        showDialogForLocationServiceSetting();
                    } else {
                        Log.d(TAG, "check RunTime Permission");
                        checkRunTimePermission();
                    }
                }
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

    void checkRunTimePermission() {
        // 위치 권한 있는지 확인하기
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(MainActivity2.this, Manifest.permission.ACCESS_FINE_LOCATION);
        int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(MainActivity2.this, Manifest.permission.ACCESS_COARSE_LOCATION);
        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED && hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED
        ) {
            // 가지고 있다면 위치값 가져올 수 있음
            Log.d(TAG, "PERMISSION_GRANTED");
            Log.d(TAG, "hasFineLocationPermission : " + hasFineLocationPermission + " hasCoarseLocationPermission : " + hasCoarseLocationPermission);
            startLocation();
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
//             모든 권한을 허용했는지 확인
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    check_result = false;
                    break;
                }
            }
            if (check_result) {
                // 위치 값 가져오기 가능
                Log.d(TAG, "permission success");
                startLocation();
            } else {
                ActivityCompat.requestPermissions(MainActivity2.this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE);  // 요청

                // 거부된 권한이 있을 때
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0]) || ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[1]) || ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[2])) {
                    Toast.makeText(MainActivity2.this, "권한이 거부 되었습니다. 다시 실행해주세요", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(MainActivity2.this, "권한이 거부 되었습니다. 다시 실행해주세요", Toast.LENGTH_LONG).show();
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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


    // 브로드캐스트리시버를 onCreate 에 등록 -> 서비스에서 데이터가 오면 mReceiver 호출됨  (실패)
//        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter("GpsServiceFilter"));

    // 서비스에서 값을 받기 위함  ---> (실패)
//    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            if (intent != null) {
//                latitude = intent.getDoubleExtra("latitude", 0);
//                longitude = intent.getDoubleExtra("longitude", 0);
//                accuracy = intent.getFloatExtra("accuracy", 0);
//                provider = intent.getStringExtra("provider");
//                Log.d(TAG, "provider = " + provider + " / latitude = " + latitude + " / longitude = " + longitude + " / accuracy = " + accuracy);
//            }
//        }
//    };
}
package com.wlrn566.gpsTracker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.net.Uri;
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

import net.daum.mf.map.api.BuildConfig;
import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapView;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private String TAG = getClass().getName();
    private long backPressedTime = 0;
    private String[] REQUIRED_PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION
            , Manifest.permission.ACCESS_COARSE_LOCATION
    };
    private ArrayList<MapPOIItem> poiItems = new ArrayList<>();
    private static final int GPS_ENABLE_REQUEST_CODE = 2001, PERMISSIONS_REQUEST_CODE = 100;
    private double latitude;
    private double longitude;
    private float accuracy;
    private String provider;

    private TextView provider_tv, add_tv, lat_tv, lng_tv, accuracy_tv;
    private Button gps_btn, setCenter_btn, btn;
    private MapView mapView;
    private ViewGroup mapViewContainer;

    private BroadcastReceiver mBroadcastReceiver;

    // startService / bindService
    // startService : 액티비티와 서비스 간에 통신을 하지 않음 (ex. 액티비티->서비스 시작->서비스 결과 알림표시 끝)
    // bindService : 액티비티와 서비스 간에 통신 가능 (ex. 액티비티->서비스 시작->서비스의 결과값을 주기적으로 액티비티에 전달)

    public class Constants {
        static final int LOCATION_SERVICE_ID = 175;
        static final String ACTION_PUSH_LOCATION_SERVICE = "pushLocationService";
        static final String ACTION_STOP_LOCATION_SERVICE = "stopLocationService";
        static final String ACTION_REALTIME_LOCATION_SERVICE = "realTimeLocationService";
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_main);

        // 권한 확인
        if (!checkLocationServiceStatus()) {  // GPS
            Log.d(TAG, "go Location Setting");
            showDialogForLocationServiceSetting();
        } else {
            Log.d(TAG, "check RunTime Permission");
            checkRunTimePermission();
        }

        ApplicationInfo ai = null;
        try {
            ai = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (ai.metaData != null) {
            String metaData = ai.metaData.getString("com.kakao.sdk.AppKe");
            Log.e("metaData",metaData+"");
        }

//        // 알림창에서 값 가져오기
//        Intent intent = getIntent();
//        if (intent != null) {
//            Log.d(TAG, "getIntent not null");
//            latitude = intent.getDoubleExtra("latitude", 0);
//            longitude = intent.getDoubleExtra("longitude", 0);
//            accuracy = intent.getFloatExtra("accuracy", 0);
//            provider = intent.getStringExtra("provider");
//            Log.d(TAG, "provider = " + provider + " / latitude = " + latitude + " / longitude = " + longitude + " / accuracy = " + accuracy);
//        } else {
//            Log.d(TAG, "getIntent is null");
//        }

        provider_tv = findViewById(R.id.provider_tv);
        add_tv = findViewById(R.id.add_tv);
        lat_tv = findViewById(R.id.lat_tv);
        lng_tv = findViewById(R.id.lng_tv);
        accuracy_tv = findViewById(R.id.accuracy_tv);
        gps_btn = findViewById(R.id.gps_btn);
        setCenter_btn = findViewById(R.id.setCenter_btn);
        btn = findViewById(R.id.btn);

        gps_btn.setOnClickListener(this);
        setCenter_btn.setOnClickListener(this);
        btn.setOnClickListener(this);

        if (isGpsServiceRunning(FusedLocationService.class)) {  // 앱 들어왔을 때
            gps_btn.setText("ON");
            realTimeLocation();  // 실시간으로 지도에 위치 띄우기
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

        setCenter();
    }

    private void pushLocation() {
        Intent intent = new Intent(getApplicationContext(), FusedLocationService.class);
        intent.setAction(Constants.ACTION_PUSH_LOCATION_SERVICE);
        startService(intent);
    }

    private void stopLocation() {
        Intent intent = new Intent(getApplicationContext(), FusedLocationService.class);
        intent.setAction(Constants.ACTION_STOP_LOCATION_SERVICE);
        startService(intent);
        stopService(intent);

        // 리시버 등록해제
        if (mBroadcastReceiver != null) {
            unregisterReceiver(mBroadcastReceiver);
        }
        if (poiItems.size() > 0) {
            for (int i = 0; i < poiItems.size(); i++) {
                mapView.removePOIItem(poiItems.get(i));
                poiItems.remove(poiItems.get(i));
            }
        }
    }

    private void realTimeLocation() {
        // updateLocation 이면 브로드캐스트로 위치값을 받아서 UI에 뿌림
        mBroadcastReceiver = new BroadcastReceiver();  // 선언
        IntentFilter filter = new IntentFilter("update");  // 필터
        registerReceiver(mBroadcastReceiver, filter);  // 등록

        Intent intent = new Intent(getApplicationContext(), FusedLocationService.class);
        intent.setAction(Constants.ACTION_REALTIME_LOCATION_SERVICE);
        startService(intent);
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
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
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
        if (isGpsServiceRunning(FusedLocationService.class)) {
            stopLocation();
            pushLocation();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.gps_btn:
                Log.d(TAG, "click");
                Log.d(TAG, "isGpsServiceRunning ? " + isGpsServiceRunning(FusedLocationService.class));
                if (isGpsServiceRunning(FusedLocationService.class)) {  // 서비스가 실행 중이면
                    gps_btn.setText("OFF");
                    stopLocation();
                } else {  // 서비스가 실행중이 아닐 때
                    // updateLocation 이면 브로드캐스트로 위치값을 받아서 UI에 뿌림
                    mBroadcastReceiver = new BroadcastReceiver();  // 선언
                    IntentFilter filter = new IntentFilter("update");  // 필터
                    registerReceiver(mBroadcastReceiver, filter);  // 등록

                    gps_btn.setText("ON");
                    realTimeLocation();
                }
                break;
            case R.id.setCenter_btn:
                Log.d(TAG, "setCenter click");
                setCenter();
                break;
            case R.id.btn:
                Log.d(TAG, "btn click");

                Uri url = Uri.parse("kakaomap://look?p=" + latitude + "," + longitude);
                Intent intent = new Intent(Intent.ACTION_VIEW, url);
                startActivity(intent);
                break;
            default:
                break;
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
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION);
        int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION);
        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED && hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED
        ) {
            // 가지고 있다면 위치값 가져올 수 있음
            Log.d(TAG, "PERMISSION_GRANTED");
            Log.d(TAG, "hasFineLocationPermission : " + hasFineLocationPermission + " hasCoarseLocationPermission : " + hasCoarseLocationPermission);
            if (isGpsServiceRunning(FusedLocationService.class)) {
                stopLocation();
                realTimeLocation();
            }
        } else {  // 권한을 요청한적이 있는데 거부한적이 있음
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, REQUIRED_PERMISSIONS[0])) {  // 거부한 적 있을 때
                Toast.makeText(MainActivity.this, "이 앱을 실행하려면 위치 접근 권한이 필요합니다.", Toast.LENGTH_LONG).show();  // 요청 이유
                ActivityCompat.requestPermissions(MainActivity.this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE);  // 요청
            } else {  // 처음 요청 시
                ActivityCompat.requestPermissions(MainActivity.this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE);  // 요청
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
                if (isGpsServiceRunning(FusedLocationService.class)) {
                    stopLocation();
                    realTimeLocation();
                }
            } else {
                ActivityCompat.requestPermissions(MainActivity.this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE);  // 요청

                // 거부된 권한이 있을 때
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0]) || ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[1]) || ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[2])) {
                    Toast.makeText(MainActivity.this, "권한이 거부 되었습니다. 다시 실행해주세요", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(MainActivity.this, "권한이 거부 되었습니다. 다시 실행해주세요", Toast.LENGTH_LONG).show();
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void showDialogForLocationServiceSetting() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
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

    // 브로드캐스트리시버로 서비스에서 업데이트 되는 좌표를 UI에 뿌려줌
    private class BroadcastReceiver extends android.content.BroadcastReceiver {
        @SuppressLint("SetTextI18n")
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                Log.d(TAG, "broadcastReceiver success");
                if (intent.getAction().equals("update")) {
                    provider = intent.getStringExtra("provider");
                    latitude = intent.getDoubleExtra("latitude", 0);
                    longitude = intent.getDoubleExtra("longitude", 0);
                    accuracy = intent.getFloatExtra("accuracy", 0);

                    provider_tv.setText("제공자 : " + provider);
                    add_tv.setText("주소 : " + getCurrentAddress(latitude, longitude));
                    lat_tv.setText("위도 : " + latitude);
                    lng_tv.setText("경도 : " + longitude);
                    accuracy_tv.setText("정확도 : " + accuracy);

                    setMarker(latitude, longitude);
                }
            }
        }
    }

    // 지도 중심 변경 해주기
    private void setCenter() {
        Log.d(TAG, "setCenter");
        if (longitude != 0 && latitude != 0) {
            mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(latitude, longitude), true);
        } else {
            Toast.makeText(this, "위치정보가 없습니다.", Toast.LENGTH_SHORT);
        }
    }

    // 마커 찍어주기
    private void setMarker(double latitude, double longitude) {
        Log.d(TAG, "setMarker");
        MapPoint mapPoint = MapPoint.mapPointWithGeoCoord(latitude, longitude);
        MapPOIItem marker = new MapPOIItem();
        marker.setItemName("Default Marker");
        marker.setTag(0);
        marker.setMapPoint(mapPoint);
        marker.setMarkerType(MapPOIItem.MarkerType.RedPin);  // 마커 모양.
        marker.setSelectedMarkerType(null);  // 마커를 클릭했을때 마커 모양.

        Log.d(TAG, "poiItems = " + poiItems);
        if (poiItems.size() > 0) {
            for (int i = 0; i < poiItems.size(); i++) {
                mapView.removePOIItem(poiItems.get(i));
                poiItems.remove(poiItems.get(i));
            }
        }
        mapView.addPOIItem(marker);
        poiItems.add(marker);
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
}
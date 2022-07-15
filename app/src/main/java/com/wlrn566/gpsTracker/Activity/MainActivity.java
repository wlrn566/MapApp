package com.wlrn566.gpsTracker.Activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
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
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.wlrn566.gpsTracker.BuildConfig;
import com.wlrn566.gpsTracker.Fragment.ShowCoordinatesDialogFragment;
import com.wlrn566.gpsTracker.Public.RetrofitClient;
import com.wlrn566.gpsTracker.Service.FusedLocationService;
import com.wlrn566.gpsTracker.R;
import com.wlrn566.gpsTracker.VO.RestaurantVO;

import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Array;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    // 1. gps on 을 하면 백그라운드에서도 사용자의 위치를 파악하게 함
    // 2. 앱 실행 중에는 마커를 찍어가며 사용자의 위치를 보여줌
    // 3. 중심이동 버튼 클릭 시 마커 위치로 이동 (처음 위치는 시청)
    // 4. 주제 선택 시 주제에 맞는 위치를 마커로 표시 -> 액티비티 호출 시 api 연동 후 마커 저장
    // 5. 주제 선택 시 사용자가 마커 위치 접근 시 알림 전송 (fcm)

    private String TAG = getClass().getName();
    private long backPressedTime = 0;
    private String[] REQUIRED_PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION
            , Manifest.permission.ACCESS_COARSE_LOCATION};
    private ArrayList<MapPOIItem> poiItems_restaurant = new ArrayList<>();
    private static final int GPS_ENABLE_REQUEST_CODE = 2001, PERMISSIONS_REQUEST_CODE = 100;
    private double latitude, longitude;
    private String provider, add;
    private float accuracy;

    //    private TextView provider_tv, add_tv, lat_tv, lng_tv, accuracy_tv;
    private Button show, gps_btn, setCenter_btn, kakaoMap_btn, select_btn;
    private MapView mapView;
    private ViewGroup mapViewContainer;

    private BroadcastReceiver mBroadcastReceiver;

    private RestaurantVO restaurantVO;

    // startService / bindService
    // startService : 액티비티와 서비스 간에 통신을 하지 않음 (ex. 액티비티->서비스 시작->서비스 결과 알림표시 끝)
    // bindService : 액티비티와 서비스 간에 통신 가능 (ex. 액티비티->서비스 시작->서비스의 결과값을 주기적으로 액티비티에 전달)

    public class Constants {
        public static final int LOCATION_SERVICE_ID = 175;
        public static final String ACTION_PUSH_LOCATION_SERVICE = "pushLocationService";
        public static final String ACTION_STOP_LOCATION_SERVICE = "stopLocationService";
        public static final String ACTION_REALTIME_LOCATION_SERVICE = "realTimeLocationService";
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
            Log.e("metaData", metaData + "");
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

        show = findViewById(R.id.show);
        gps_btn = findViewById(R.id.gps_btn);
        setCenter_btn = findViewById(R.id.setCenter_btn);
        kakaoMap_btn = findViewById(R.id.kakaoMap_btn);
        select_btn = findViewById(R.id.select_btn);

        show.setOnClickListener(this);
        gps_btn.setOnClickListener(this);
        setCenter_btn.setOnClickListener(this);
        kakaoMap_btn.setOnClickListener(this);
        select_btn.setOnClickListener(this);

        if (isGpsServiceRunning(FusedLocationService.class)) {  // 앱 들어왔을 때
            gps_btn.setText("ON");
            realTimeLocation();  // 실시간으로 지도에 위치 띄우기
        } else {  // 처음 실행 또는 알림이 아닌 직접 들어왔을 때
            gps_btn.setText("OFF");
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.show:
                Log.d(TAG, "showCoordinatesDialogFragment");
                Bundle bundle = new Bundle();
                if (provider != null) {
                    bundle.putString("provider", provider);
                    bundle.putDouble("lat", latitude);
                    bundle.putDouble("lng", longitude);
                    bundle.putString("add", add);
                    ShowCoordinatesDialogFragment showCoordinatesDialogFragment = new ShowCoordinatesDialogFragment();
                    showCoordinatesDialogFragment.setArguments(bundle);
                    showCoordinatesDialogFragment.show(this.getSupportFragmentManager(), null);
                } else {
                    Toast.makeText(this, "확인된 위치가 없습니다.", Toast.LENGTH_SHORT).show();
                }
                break;
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
            case R.id.kakaoMap_btn:
                Log.d(TAG, "kakaoMap_btn click");
                Uri url = Uri.parse("kakaomap://look?p=" + latitude + "," + longitude);
                Intent intent = new Intent(Intent.ACTION_VIEW, url);
                startActivity(intent);
                break;
            case R.id.select_btn:
                Log.d(TAG, "select_btn click");
                showDialog();
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

    //    ------------------------------------------------------------------권한-------------------------------------------------------------------
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

    //    ----------------------------------------------------------------------GPS-------------------------------------------------------------------
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
            Log.d(TAG, "unregisterReceiver");
            unregisterReceiver(mBroadcastReceiver);

            MapPOIItem[] marker_before = mapView.findPOIItemByName("Gps Marker");
            if (marker_before != null) {
                Log.d(TAG, "Gps Marker remove");
                mapView.removePOIItems(marker_before);
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
        add = address.getAddressLine(0).toString();
        return add;
    }

    // 브로드캐스트리시버로 서비스에서 업데이트 되는 좌표받아옴
    private class BroadcastReceiver extends android.content.BroadcastReceiver {
        @SuppressLint("SetTextI18n")
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                Log.d(TAG, "broadcastReceiver success");
                if (intent.getAction().equals("update")) {
                    provider = intent.getStringExtra("provider");
                    add = getCurrentAddress(latitude, longitude);
                    latitude = intent.getDoubleExtra("latitude", 0);
                    longitude = intent.getDoubleExtra("longitude", 0);
                    accuracy = intent.getFloatExtra("accuracy", 0);

                    setMarker(latitude, longitude);
                }
            }
        }
    }

    // 다이얼로그에 뿌려줄 업데이트 좌표
    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getProvider() {
        return provider;
    }

    public String getAdd() {
        return add;
    }

    //    --------------------------------------------------------------------------세팅-------------------------------------------------------------------
    @SuppressLint("SetTextI18n")
    private void setPage() {
        Log.d(TAG, "setPage");
        // 지도 띄우기
        mapView = new MapView(this);
        mapViewContainer = (ViewGroup) findViewById(R.id.map_view);
        mapViewContainer.addView(mapView);
        setCenter();
    }

    // 지도 중심 변경 해주기
    private void setCenter() {
        Log.d(TAG, "setCenter");
        if (longitude != 0 && latitude != 0) {
            mapView.setMapCenterPointAndZoomLevel(MapPoint.mapPointWithGeoCoord(latitude, longitude), 3, true);
        } else {
            mapView.setMapCenterPointAndZoomLevel(MapPoint.mapPointWithGeoCoord(35.1595454, 126.8526012), 5, true);
        }
    }

    // 마커 찍어주기
    private void setMarker(double latitude, double longitude) {
        Log.d(TAG, "setMarker");
        MapPoint mapPoint = MapPoint.mapPointWithGeoCoord(latitude, longitude);
        MapPOIItem marker = new MapPOIItem();
        marker.setItemName("Gps Marker");
        marker.setTag(0);
        marker.setMapPoint(mapPoint);
        marker.setMarkerType(MapPOIItem.MarkerType.RedPin);  // 마커 모양.
        marker.setSelectedMarkerType(null);  // 마커를 클릭했을때 마커 모양.
        marker.setShowCalloutBalloonOnTouch(false);  // 마커 클릭 유무

        MapPOIItem[] marker_before = mapView.findPOIItemByName("Gps Marker");
        if (marker_before != null) {
            mapView.removePOIItems(marker_before);
        }
        mapView.addPOIItem(marker);
//        poiItems.add(marker);
    }

    // 아이템 선택 시 저장한 마커 찍어주기
    private void setItemMarker(String selected) {
        Log.d(TAG, "setItemMarker");
        if (selected.equals("맛집")) {
            for (int i = 0; i < poiItems_restaurant.size(); i++) {
                mapView.addPOIItem(poiItems_restaurant.get(i));
            }
        } else if (selected.equals("관광지")) {
            Toast.makeText(this, "준비중입니다...", Toast.LENGTH_SHORT).show();
        } else if (selected.equals("선택")) {
            MapPOIItem[] mapPOIItems = mapView.findPOIItemByName("Restaurant Marker");
            if (mapPOIItems != null) {
                Log.d(TAG, "Restaurant Marker count = " + mapPOIItems.length);
                mapView.removePOIItems(mapPOIItems);
            }
        }
    }

    // 주제 선택 다이얼로그 띄우기
    private void showDialog() {
        Log.d(TAG, "setDialog");
        String[] items = new String[]{"선택", "맛집", "관광지"};

        final int[] item = {0}; // 선택된 포지션 저장할 변수
        new AlertDialog.Builder(this).setTitle("알고 싶은 위치를 선택해 주세요.").setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                item[0] = i; // 다이얼로그에서 아이템 선택 시 위치를 저장
            }
        }).setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String selected = items[item[0]];  // '확인' 버튼을 누르면 선택한 주제를 저장
                Log.d(TAG, "select = " + selected);
                select_btn.setText(selected);
                setItemMarker(selected);  // 선택 된 아이템에 맞는 마커 표시해주기
            }
        }).setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        }).show();
    }

    // 맛집 마커 위치 저장
    private void saveMarkerRestaurant(double latitude, double longitude) {
        Log.d(TAG, "setMarkerRestaurant");
        MapPoint mapPoint = MapPoint.mapPointWithGeoCoord(latitude, longitude);
        MapPOIItem marker = new MapPOIItem();
        marker.setItemName("Restaurant Marker");
        marker.setTag(0);
        marker.setMapPoint(mapPoint);
        marker.setMarkerType(MapPOIItem.MarkerType.BluePin);  // 마커 모양.
        marker.setSelectedMarkerType(null);  // 마커를 클릭했을때 마커 모양.

        poiItems_restaurant.add(marker);
        Log.d(TAG, "poiItems_restaurant count = " + String.valueOf(poiItems_restaurant.size()));
    }

    // 맛집 데이터 불러오기
    private void loadRestaurantData() {
        RequestQueue requestQueue = Volley.newRequestQueue(this);

        final String url = "http://192.168.0.9/load_restaurant.php?";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, new com.android.volley.Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
//                Log.d(TAG, response.toString());
                try {
                    if (response.getString("result_str").equals("success")) {
                        JSONArray jsonArray = response.getJSONArray("restaurant");  // VO 변환할 데이터 추출
                        Log.d(TAG, "restaurantData count = " + jsonArray.length());
                        Gson gson = new Gson();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            restaurantVO = gson.fromJson(jsonArray.getString(i), RestaurantVO.class);  // JSON -> Object 변환
//                            Log.d(TAG, restaurantVO.toString());
                            saveMarkerRestaurant(restaurantVO.getLatitude(), restaurantVO.getLongitude());  // 마커 저장
                        }
                    } else {
                        Log.e(TAG, "failed loadRestaurantData");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, error.toString());
            }
        });
        requestQueue.add(request);
    }
//    private void loadRestaurantData() {
//        Call<RestaurantVO> getData = RetrofitClient.getApiService().getRestaurantData("s");
//        getData.enqueue(new Callback<RestaurantVO>() {
//            @Override
//            public void onResponse(Call<RestaurantVO> call, Response<RestaurantVO> response) {
//                Log.d(TAG,"response = "+response.body());
//            }
//
//            @Override
//            public void onFailure(Call<RestaurantVO> call, Throwable t) {
//                Log.d(TAG,t.getMessage());
//            }
//        });
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
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        setPage();
        loadRestaurantData();
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
}
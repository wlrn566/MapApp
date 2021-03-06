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
    // 1. gps on ??? ?????? ???????????????????????? ???????????? ????????? ???????????? ???
    // 2. ??? ?????? ????????? ????????? ???????????? ???????????? ????????? ?????????
    // 3. ???????????? ?????? ?????? ??? ?????? ????????? ?????? (?????? ????????? ??????)
    // 4. ?????? ?????? ??? ????????? ?????? ????????? ????????? ?????? -> ???????????? ?????? ??? api ?????? ??? ?????? ??????
    // 5. ?????? ?????? ??? ???????????? ?????? ?????? ?????? ??? ?????? ?????? (fcm)

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
    // startService : ??????????????? ????????? ?????? ????????? ?????? ?????? (ex. ????????????->????????? ??????->????????? ?????? ???????????? ???)
    // bindService : ??????????????? ????????? ?????? ?????? ?????? (ex. ????????????->????????? ??????->???????????? ???????????? ??????????????? ??????????????? ??????)

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
        // ?????? ??????
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

//        // ??????????????? ??? ????????????
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

        if (isGpsServiceRunning(FusedLocationService.class)) {  // ??? ???????????? ???
            gps_btn.setText("ON");
            realTimeLocation();  // ??????????????? ????????? ?????? ?????????
        } else {  // ?????? ?????? ?????? ????????? ?????? ?????? ???????????? ???
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
                    Toast.makeText(this, "????????? ????????? ????????????.", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.gps_btn:
                Log.d(TAG, "click");
                Log.d(TAG, "isGpsServiceRunning ? " + isGpsServiceRunning(FusedLocationService.class));
                if (isGpsServiceRunning(FusedLocationService.class)) {  // ???????????? ?????? ?????????
                    gps_btn.setText("OFF");
                    stopLocation();
                } else {  // ???????????? ???????????? ?????? ???
                    // updateLocation ?????? ????????????????????? ???????????? ????????? UI??? ??????
                    mBroadcastReceiver = new BroadcastReceiver();  // ??????
                    IntentFilter filter = new IntentFilter("update");  // ??????
                    registerReceiver(mBroadcastReceiver, filter);  // ??????

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
        // ???????????? ?????? ?????? ??? 2?????? ???????????? Toast ??????
        if (System.currentTimeMillis() > backPressedTime + 2000) {
            backPressedTime = System.currentTimeMillis();
            Toast toast = Toast.makeText(this, "?????? ??? ???????????? ???????????????.", Toast.LENGTH_SHORT);
            toast.show();
            return;
        }
        // ???????????? ?????? ?????? ??? 2?????? ??????????????? ??????
        if (System.currentTimeMillis() <= backPressedTime + 2000) {
            finish();
        }
    }

    //    ------------------------------------------------------------------??????-------------------------------------------------------------------
    void checkRunTimePermission() {
        // ?????? ?????? ????????? ????????????
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION);
        int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION);
        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED && hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED
        ) {
            // ????????? ????????? ????????? ????????? ??? ??????
            Log.d(TAG, "PERMISSION_GRANTED");
            Log.d(TAG, "hasFineLocationPermission : " + hasFineLocationPermission + " hasCoarseLocationPermission : " + hasCoarseLocationPermission);
            if (isGpsServiceRunning(FusedLocationService.class)) {
                stopLocation();
                realTimeLocation();
            }
        } else {  // ????????? ??????????????? ????????? ??????????????? ??????
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, REQUIRED_PERMISSIONS[0])) {  // ????????? ??? ?????? ???
                Toast.makeText(MainActivity.this, "??? ?????? ??????????????? ?????? ?????? ????????? ???????????????.", Toast.LENGTH_LONG).show();  // ?????? ??????
                ActivityCompat.requestPermissions(MainActivity.this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE);  // ??????
            } else {  // ?????? ?????? ???
                ActivityCompat.requestPermissions(MainActivity.this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE);  // ??????
            }
        }
    }

    // checkRunTimePermission??? request ?????????
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        // ??????????????? ?????? ?????????????????? request ????????? ???
        if (requestCode == PERMISSIONS_REQUEST_CODE && grantResults.length == REQUIRED_PERMISSIONS.length) {
            boolean check_result = true;
//             ?????? ????????? ??????????????? ??????
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    check_result = false;
                    break;
                }
            }
            if (check_result) {
                // ?????? ??? ???????????? ??????
                Log.d(TAG, "permission success");
                if (isGpsServiceRunning(FusedLocationService.class)) {
                    stopLocation();
                    realTimeLocation();
                }
            } else {
                ActivityCompat.requestPermissions(MainActivity.this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE);  // ??????

                // ????????? ????????? ?????? ???
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0]) || ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[1]) || ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[2])) {
                    Toast.makeText(MainActivity.this, "????????? ?????? ???????????????. ?????? ??????????????????", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(MainActivity.this, "????????? ?????? ???????????????. ?????? ??????????????????", Toast.LENGTH_LONG).show();
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void showDialogForLocationServiceSetting() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("?????? ????????? ????????????");
        builder.setMessage("?????? ???????????? ?????? ?????? ???????????? ???????????????.");
        builder.setCancelable(true);
        builder.setPositiveButton("??????", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent callGPSSettingIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("??????", new DialogInterface.OnClickListener() {
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
                // GPS ????????? ?????? ??????
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

        // ????????? ????????????
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
        // updateLocation ?????? ????????????????????? ???????????? ????????? UI??? ??????
        mBroadcastReceiver = new BroadcastReceiver();  // ??????
        IntentFilter filter = new IntentFilter("update");  // ??????
        registerReceiver(mBroadcastReceiver, filter);  // ??????

        Intent intent = new Intent(getApplicationContext(), FusedLocationService.class);
        intent.setAction(Constants.ACTION_REALTIME_LOCATION_SERVICE);
        startService(intent);
    }

    public String getCurrentAddress(double latitude, double longitude) {
        //Geocoder - GPS??? ????????? ??????
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        List<Address> addresses;
        try {
            addresses = geocoder.getFromLocation(
                    latitude,
                    longitude,
                    7);
        } catch (IOException ioException) {
            //???????????? ??????
            Toast.makeText(this, "Geocoder failed", Toast.LENGTH_LONG).show();
            return "???????????? ????????? ????????????";
        } catch (IllegalArgumentException illegalArgumentException) {
            Toast.makeText(this, "wrong GPS coordinate", Toast.LENGTH_LONG).show();
            return "????????? GPS ??????";
        }
        if (addresses == null || addresses.size() == 0) {
            Toast.makeText(this, "address undetected", Toast.LENGTH_LONG).show();
            return "?????? ?????????";
        }
        Address address = addresses.get(0);
        add = address.getAddressLine(0).toString();
        return add;
    }

    // ?????????????????????????????? ??????????????? ???????????? ?????? ???????????????
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

    // ?????????????????? ????????? ???????????? ??????
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

    //    --------------------------------------------------------------------------??????-------------------------------------------------------------------
    @SuppressLint("SetTextI18n")
    private void setPage() {
        Log.d(TAG, "setPage");
        // ?????? ?????????
        mapView = new MapView(this);
        mapViewContainer = (ViewGroup) findViewById(R.id.map_view);
        mapViewContainer.addView(mapView);
        setCenter();
    }

    // ?????? ?????? ?????? ?????????
    private void setCenter() {
        Log.d(TAG, "setCenter");
        if (longitude != 0 && latitude != 0) {
            mapView.setMapCenterPointAndZoomLevel(MapPoint.mapPointWithGeoCoord(latitude, longitude), 3, true);
        } else {
            mapView.setMapCenterPointAndZoomLevel(MapPoint.mapPointWithGeoCoord(35.1595454, 126.8526012), 5, true);
        }
    }

    // ?????? ????????????
    private void setMarker(double latitude, double longitude) {
        Log.d(TAG, "setMarker");
        MapPoint mapPoint = MapPoint.mapPointWithGeoCoord(latitude, longitude);
        MapPOIItem marker = new MapPOIItem();
        marker.setItemName("Gps Marker");
        marker.setTag(0);
        marker.setMapPoint(mapPoint);
        marker.setMarkerType(MapPOIItem.MarkerType.RedPin);  // ?????? ??????.
        marker.setSelectedMarkerType(null);  // ????????? ??????????????? ?????? ??????.
        marker.setShowCalloutBalloonOnTouch(false);  // ?????? ?????? ??????

        MapPOIItem[] marker_before = mapView.findPOIItemByName("Gps Marker");
        if (marker_before != null) {
            mapView.removePOIItems(marker_before);
        }
        mapView.addPOIItem(marker);
//        poiItems.add(marker);
    }

    // ????????? ?????? ??? ????????? ?????? ????????????
    private void setItemMarker(String selected) {
        Log.d(TAG, "setItemMarker");
        if (selected.equals("??????")) {
            for (int i = 0; i < poiItems_restaurant.size(); i++) {
                mapView.addPOIItem(poiItems_restaurant.get(i));
            }
        } else if (selected.equals("?????????")) {
            Toast.makeText(this, "??????????????????...", Toast.LENGTH_SHORT).show();
        } else if (selected.equals("??????")) {
            MapPOIItem[] mapPOIItems = mapView.findPOIItemByName("Restaurant Marker");
            if (mapPOIItems != null) {
                Log.d(TAG, "Restaurant Marker count = " + mapPOIItems.length);
                mapView.removePOIItems(mapPOIItems);
            }
        }
    }

    // ?????? ?????? ??????????????? ?????????
    private void showDialog() {
        Log.d(TAG, "setDialog");
        String[] items = new String[]{"??????", "??????", "?????????"};

        final int[] item = {0}; // ????????? ????????? ????????? ??????
        new AlertDialog.Builder(this).setTitle("?????? ?????? ????????? ????????? ?????????.").setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                item[0] = i; // ????????????????????? ????????? ?????? ??? ????????? ??????
            }
        }).setPositiveButton("??????", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String selected = items[item[0]];  // '??????' ????????? ????????? ????????? ????????? ??????
                Log.d(TAG, "select = " + selected);
                select_btn.setText(selected);
                setItemMarker(selected);  // ?????? ??? ???????????? ?????? ?????? ???????????????
            }
        }).setNegativeButton("??????", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        }).show();
    }

    // ?????? ?????? ?????? ??????
    private void saveMarkerRestaurant(double latitude, double longitude) {
        Log.d(TAG, "setMarkerRestaurant");
        MapPoint mapPoint = MapPoint.mapPointWithGeoCoord(latitude, longitude);
        MapPOIItem marker = new MapPOIItem();
        marker.setItemName("Restaurant Marker");
        marker.setTag(0);
        marker.setMapPoint(mapPoint);
        marker.setMarkerType(MapPOIItem.MarkerType.BluePin);  // ?????? ??????.
        marker.setSelectedMarkerType(null);  // ????????? ??????????????? ?????? ??????.

        poiItems_restaurant.add(marker);
        Log.d(TAG, "poiItems_restaurant count = " + String.valueOf(poiItems_restaurant.size()));
    }

    // ?????? ????????? ????????????
    private void loadRestaurantData() {
        RequestQueue requestQueue = Volley.newRequestQueue(this);

        final String url = "http://192.168.0.9/load_restaurant.php?";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, new com.android.volley.Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
//                Log.d(TAG, response.toString());
                try {
                    if (response.getString("result_str").equals("success")) {
                        JSONArray jsonArray = response.getJSONArray("restaurant");  // VO ????????? ????????? ??????
                        Log.d(TAG, "restaurantData count = " + jsonArray.length());
                        Gson gson = new Gson();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            restaurantVO = gson.fromJson(jsonArray.getString(i), RestaurantVO.class);  // JSON -> Object ??????
//                            Log.d(TAG, restaurantVO.toString());
                            saveMarkerRestaurant(restaurantVO.getLatitude(), restaurantVO.getLongitude());  // ?????? ??????
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

    // ????????? ???
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
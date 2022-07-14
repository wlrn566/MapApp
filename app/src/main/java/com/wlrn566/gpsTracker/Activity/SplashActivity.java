package com.wlrn566.gpsTracker.Activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.wlrn566.gpsTracker.Activity.MainActivity;
import com.wlrn566.gpsTracker.BuildConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

public class SplashActivity extends AppCompatActivity {

    private String TAG = getClass().getName();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        loading();
    }
    private void loading() {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "App Loading");
                // splash 후 연결 할 액티비티
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                finish();
            }
        }, 2000);
    }


//    --------------------------------------------------맛집 api 연동 -> DB 저장 위해 INSERT문으로 로그 출력---------------------------------------
    private void loadRestaurant() {
        RequestQueue requestQueue = Volley.newRequestQueue(this);

        final String key = BuildConfig.FOOD_API_KEY;
        final String url = "https://api.odcloud.kr/api/3082925/v1/uddi:eeb6164d-1dd7-4382-8a96-a6888185864a?page=1&perPage=100&serviceKey=" + key;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d(TAG, response.toString());
//                get();
                try {
                    JSONArray jsonArray = response.getJSONArray("data");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        String addr = jsonArray.getJSONObject(i).getString("소재지");
                        String name = jsonArray.getJSONObject(i).getString("상 호");

                        // 쓰레드로 진행
                        Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                getCurrentCoordinates(addr, name);
                            }
                        });
                        thread.start();
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

    public void getCurrentCoordinates(String str, String name) {
        //Geocoder - 주소를 GPS로 변환
        Geocoder geocoder = new Geocoder(this);
        try {
            List<Address> list = geocoder.getFromLocationName(str, 10);
            for (int i = 0; i < list.size(); i++) {
                Address address = list.get(i);
                Double lat = address.getLatitude();
                Double lng = address.getLongitude();
//                Log.d(TAG, "name = " + name + " lat = " + lat + " lng = " + lng);
                Log.d(TAG, "INSERT INTO restaurant(name,latitude,longitude) VALUES('" + name + "'," + lat + "," + lng + ");");
//                saveMarkerRestaurant(lat, lng);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
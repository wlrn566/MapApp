package com.wlrn566.pushapp;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapView;

import java.security.MessageDigest;

public class MainActivity extends AppCompatActivity {

    private String TAG = getClass().getName();

    private Permissions permission;
    boolean ck;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_main);
        MapView mapView = new MapView(this);

        ViewGroup mapViewContainer = (ViewGroup) findViewById(R.id.map_view);

        // 권한 체크
        permissionCheck();
        Log.d(TAG, "permission ck = " + ck);

        // 지도 나타내기
        setMap(mapView, mapViewContainer);

//        if (ck) {
//        setMap(mapView, mapViewContainer);  // 권한이 있다면 지도 나타내기
//        } else {
//            exitProgram();  // 권한 없으면 꺼버리기
//        }

//        MainFragment mainFragment = new MainFragment();
//        getSupportFragmentManager().beginTransaction().replace(R.id.main, mainFragment).commit();
//        permissionCheck();

        getAppKeyHash();
    }

    private void setMap(MapView mapView, ViewGroup mapViewContainer) {

        mapViewContainer.addView(mapView);

        // 중심점 변경
        mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(37.53737528, 127.00557633), true);

        // 줌 레벨 변경
        mapView.setZoomLevel(7, true);

        // 중심점 변경 + 줌 레벨 변경
        mapView.setMapCenterPointAndZoomLevel(MapPoint.mapPointWithGeoCoord(33.41, 126.52), 9, true);

        // 줌 인
        mapView.zoomIn(true);

        // 줌 아웃
        mapView.zoomOut(true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");

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

    private void permissionCheck() {
        permission = new Permissions(this, this);
        // 권한 체크
        // 권한이 없다면 (false) 요청하기
        if (permission.check() == false) {
            permission.request();
        } else {
            ck = true;
        }
    }

    // permissions의 request 결과값
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // 요청이 거부
        if (permission.result(requestCode, permissions, grantResults) == false) {
            ck = false;
            // 다시 요청
            permission.request();
        } else {
            ck = true;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void exitProgram() {
        // 태스크를 백그라운드로 이동
        moveTaskToBack(true);

        if (Build.VERSION.SDK_INT >= 21) {
            // 액티비티 종료 + 태스크 리스트에서 지우기
            finishAndRemoveTask();
        } else {
            // 액티비티 종료
            finish();
        }
        System.exit(0);
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }
}
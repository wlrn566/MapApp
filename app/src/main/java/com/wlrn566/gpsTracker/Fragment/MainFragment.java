package com.wlrn566.gpsTracker.Fragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import com.wlrn566.gpsTracker.R;

import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapView;

public class MainFragment extends Fragment implements MapView.CurrentLocationEventListener{
    private String TAG = getClass().getName();
    View rootView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        rootView = inflater.inflate(R.layout.fragment_main, container, false);

        MapView mapView = new MapView(getActivity());

        ViewGroup mapViewContainer = rootView.findViewById(R.id.map_view);

        mapViewContainer.addView(mapView);

        mapView.setCurrentLocationEventListener(this);

        mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeading);

        // 중심점 변경
//        mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(37.53737528, 127.00557633), true);
//
//        // 줌 레벨 변경
//        mapView.setZoomLevel(7, true);
//
//
//        // 줌 인
//        mapView.zoomIn(true);
//
//        // 줌 아웃
//        mapView.zoomOut(true);

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    @Override
    public void onCurrentLocationUpdate(MapView mapView, MapPoint mapPoint, float v) {
        Log.d(TAG,"update = "+mapPoint);
    }

    @Override
    public void onCurrentLocationDeviceHeadingUpdate(MapView mapView, float v) {

    }

    @Override
    public void onCurrentLocationUpdateFailed(MapView mapView) {

    }

    @Override
    public void onCurrentLocationUpdateCancelled(MapView mapView) {

    }
}
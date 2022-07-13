package com.wlrn566.gpsTracker.Fragment;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.wlrn566.gpsTracker.R;

public class ShowCoordinatesDialogFragment extends DialogFragment {
    // 생명주기
    // (처음 호출 시) onAttach -> onCreate -> onCreateDialog -> onCreateView -> onStart -> onResume
    // (다른 화면 호출 시) onDismiss -> onStop -> onDestroyView -> onDestroy -> onDetach
    // (앱 백그라운드로 보냈을 시) onPause -> onStop / (다시 호출 시) onStart -> onResume

    private String TAG = getClass().getName();

    private View rootView;
    private TextView provider_tv, add_tv, lat_tv, lng_tv;

    private double latitude, longitude;
    private String provider, add;

    @Override
    public void onAttach(@NonNull Context context) {
        Log.d(TAG, "onAttach");
        super.onAttach(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            provider = getArguments().getString("provider");
            add = getArguments().getString("add");
            latitude = getArguments().getDouble("lat");
            longitude = getArguments().getDouble("lng");
        }

    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateDialog");
        return super.onCreateDialog(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        rootView = inflater.inflate(R.layout.fragment_show_coordinates_dialog, container, false);

        provider_tv = rootView.findViewById(R.id.provider_tv);
        add_tv = rootView.findViewById(R.id.add_tv);
        lat_tv = rootView.findViewById(R.id.lat_tv);
        lng_tv = rootView.findViewById(R.id.lng_tv);

        setPage();

        return rootView;
    }

    private void setPage() {
        provider_tv.setText("위치 제공자 : " + provider);
        add_tv.setText("주소 : " + add);
        lat_tv.setText("위도 : " + latitude);
        lng_tv.setText("경도 : " + longitude);
    }

    @Override
    public void onStart() {
        Log.d(TAG, "onStart");
        super.onStart();
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        Log.d(TAG, "onDismiss");
        super.onDismiss(dialog);
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView");
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        Log.d(TAG, "onDetach");
        super.onDetach();
    }

}
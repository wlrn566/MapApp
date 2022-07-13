//package com.wlrn566.gpsTracker;
//
//import android.app.Notification;
//import android.app.NotificationChannel;
//import android.app.NotificationManager;
//import android.app.PendingIntent;
//import android.content.Context;
//import android.content.Intent;
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
//import android.os.Build;
//import android.util.Log;
//
//import androidx.annotation.NonNull;
//import androidx.core.app.NotificationCompat;
//
//import com.google.firebase.messaging.FirebaseMessagingService;
//import com.google.firebase.messaging.RemoteMessage;
//import com.wlrn566.gpsTracker.R;
//
//public class MyFirebaseMessagingService extends FirebaseMessagingService {
//    private String TAG = getClass().getName();
//
//    // 디바이스 토큰 (최초 설치 시 생성됨)
//    @Override
//    public void onNewToken(@NonNull String token) {
//        super.onNewToken(token);
//        Log.d(TAG, "token = " + token);
//    }
//
//    // 메세지 수신
//    @Override
//    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
//        super.onMessageReceived(remoteMessage);
//
//        // 수신한 메세지에서의 제목과 내용
//        String title = remoteMessage.getData().get("title");
//        String message = remoteMessage.getData().get("message");
////        String uri = remoteMessage.getData().get("abc");
//
//        Log.d(TAG, "title = " + title + "  message = " + message);
//
//        //알림(Notification)을 관리하는 관리자 객체를 운영체제(Context)로부터 소환하기
//        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//
//        //Notification 객체를 생성해주는 건축가객체 생성(AlertDialog 와 비슷)
//        NotificationCompat.Builder builder = null;
//
//        //Oreo 버전(API26 버전)이상에서는 알림시에 NotificationChannel 이라는 개념이 필수 구성요소가 됨.
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//
//            String channelID = "channelID";  //알림채널 식별자
//            String channelName = "channelName";  //알림채널의 이름(별명)
//
//            //알림채널 객체 만들기
//            NotificationChannel channel = new NotificationChannel(channelID, channelName, NotificationManager.IMPORTANCE_DEFAULT);
//
//            //알림매니저에게 채널 객체의 생성을 요청
//            notificationManager.createNotificationChannel(channel);
//
//            //알림건축가 객체 생성
//            builder = new NotificationCompat.Builder(this, channelID);
//
//        } else {
//            //알림 건축가 객체 생성
//            builder = new NotificationCompat.Builder(this, (Notification) null);
//        }
//
//        // 알림 설정작업 -----------------------------------------------------------------
//        builder.setSmallIcon(R.drawable.ic_seliner);  // 알림 아이콘 (상단)
//        builder.setContentTitle(title);  // 알림창 제목
//        builder.setContentText(message);  // 알림창 내용
//        builder.setAutoCancel(true);  // 알림 클릭 시 사라지게 하기
//
//        //알림창의 큰 이미지 (우측)
//        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.ic_seliner);
//        builder.setLargeIcon(bm);  // 매개변수로 Bitmap을 줘야한다.
//
//        // 알림창 클릭 시 실행할 액티비티 -----------------------------------------------------------------------------------------
//        Intent intent = new Intent(this, SplashActivity.class)
//                .setAction(Intent.ACTION_MAIN)
//                .addCategory(Intent.CATEGORY_LAUNCHER)
//                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        // 잠시 보류시키는 Intent 객체
//        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
//        builder.setContentIntent(pendingIntent);  // 클릭 시 보류한 Intent 호출
//
//        //건축가에게 알림 객체 생성하도록
//        Notification notification = builder.build();
//
//        //알림매니저에게 알림(Notify) 요청
//        notificationManager.notify(1, notification);
//
//        //알림 요청시에 사용한 번호를 알림제거 할 수 있음.
//        //notificationManager.cancel(1);
//    }
//
//}
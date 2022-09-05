package com.hklab.hkfcm;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class MyFireBaseMessagingService extends FirebaseMessagingService {

    private static final String TAG_HH      = "[HHCHOI2]";

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        //sendRegistrationToServer(token);
        Log.d(TAG_HH, "New generated token: " + token);  // 여기에서 읽히는 token 값을 서버의 FCM 호출 프로그램(Message Console)에서 사용해야 함!!
    }


    //앱이 켜져 있는 상태에서 FCM 메시지를 받는경우에 실행 - 여기에서는 data도 받을 수 있음!
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        //앱이 foreground 상태에서 Notification을 받는경우에 실행 - foreground에서는 data도 받을 수 있음!
        if (remoteMessage.getNotification() != null) {

            // 받은 data 값들 (보낼때 dictionary 형태로 되어 있음)
            String data = remoteMessage.getData().toString();
            Log.d(TAG_HH, "FCM data: " + data);
            String title = remoteMessage.getData().get("title");
            String message = remoteMessage.getData().get("message");
            String image = remoteMessage.getData().get("image");
            Log.d(TAG_HH, "FCM data title: " + title + ", " + "FCM data message: " + message + "FCM data image: " + image);

            // 받은 notification 값들
            String notiTitle = remoteMessage.getNotification().getTitle();
            String notiBody = remoteMessage.getNotification().getBody();
            String notiImageUrl = remoteMessage.getNotification().getImageUrl().toString();
            Bitmap myBitmap = getImageFromURL(notiImageUrl);
            Log.d(TAG_HH, "FCM notification Title: " + notiTitle);
            Log.d(TAG_HH, "FCM notification Body: " + notiBody);
            Log.d(TAG_HH, "FCM notification ImageURL: " + notiImageUrl);

            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);

            String channelId = "Channel ID";
            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            NotificationCompat.Builder notificationBuilder =
                    new NotificationCompat.Builder(this, channelId)
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle(notiTitle)
                            .setContentText(notiBody)
                            .setLargeIcon(myBitmap)     // 그림을 아이콘으로 표시 위해
                            .setStyle(new NotificationCompat.BigPictureStyle()  // 눌렀을 때 큰 그림으로 나타나게 - 현재는 안됨!
                                    .bigPicture(myBitmap)
                                    .bigLargeIcon(null))
                            .setSound(defaultSoundUri)
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true);

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String channelName = "Channel Name";
                NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
                notificationManager.createNotificationChannel(channel);
            }
            notificationManager.notify(0, notificationBuilder.build());     // 위에서 내려오는 notification 메시지 실행

            //updateContent(remoteMessage.data)     // 받은 데이터 처리하는 부분 구현해야!
        }

        //앱이 포어그라운드 상태에서 Notification을 받는경우에 실행 (원래 코드 - 그림 안받는 것)
        /*if (remoteMessage.getNotification() != null) {

            String data = remoteMessage.getData().toString();
            Log.d(TAG_HH, "FCM data: " + data);

            String notiTitle = remoteMessage.getNotification().getTitle();
            String notiBody = remoteMessage.getNotification().getBody();
            Log.d(TAG_HH, "FCM notification Title: " + notiTitle);
            Log.d(TAG_HH, "FCM notification Body: " + notiBody);

            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);

            String channelId = "Channel ID";
            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            NotificationCompat.Builder notificationBuilder =
                    new NotificationCompat.Builder(this, channelId)
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle(notiTitle)
                            .setContentText(notiBody)
                            .setAutoCancel(true)
                            .setSound(defaultSoundUri)
                            .setContentIntent(pendingIntent);

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String channelName = "Channel Name";
                NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
                notificationManager.createNotificationChannel(channel);
            }
            notificationManager.notify(0, notificationBuilder.build());     // 위에서 내려오는 notification 메시지 실행

            //updateContent(remoteMessage.data)     // 받은 데이터 처리하는 부분 구현해야!
        }*/
    }

    /* Convert URL to bitmap */
    public static Bitmap getImageFromURL(String imageURL){
        Bitmap imgBitmap = null;
        HttpURLConnection conn = null;
        BufferedInputStream bis = null;

        try
        {
            URL url = new URL(imageURL);
            conn = (HttpURLConnection)url.openConnection();
            conn.connect();

            int nSize = conn.getContentLength();
            bis = new BufferedInputStream(conn.getInputStream(), nSize);
            imgBitmap = BitmapFactory.decodeStream(bis);
        }
        catch (Exception e){
            e.printStackTrace();
        } finally{
            if(bis != null) {
                try {bis.close();} catch (IOException e) {}
            }
            if(conn != null ) {
                conn.disconnect();
            }
        }

        return imgBitmap;
    }

}

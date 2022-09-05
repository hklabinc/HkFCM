package com.hklab.hkfcm;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.google.firebase.messaging.FirebaseMessaging;

import org.eclipse.paho.client.mqttv3.*;    // by hhchoi for MQTT lib
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private static final String TAG_HH      = "[HHCHOI]";
    private WebView webView;
    private String url = "http://ictrobot.hknu.ac.kr:8080";

    /* For MQTT */
    private static final String PUB_TOPIC   = "HkPlatform/FCM/Token";
    //private static final String SUB_TOPIC   = "HkPlatform/FCM/Control";
    private static final String BROKER_ADDR = "tcp://ictrobot.hknu.ac.kr:8085";
    private static final int    QOS         = 2;
    MqttClient mqttClient = null;
    MemoryPersistence persistence = new MemoryPersistence();
    private String token = null;
    private static String clientId    = null;
    private static String androidId = null;
    private static String appName = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* WebView Setting */
        webView = (WebView)findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);       // Use java script
        webView.loadUrl(url);                                   // URL address to see
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClientClass());

        Intent intent = getIntent();
        if(intent != null) {//푸시알림을 선택해서 실행한것이 아닌경우 예외처리
            String notificationData = intent.getStringExtra("test");
            if(notificationData != null)
                Log.d(TAG_HH, notificationData);
        }

        /** Get Andriod ID */
        //Log.d(LOGTAG, "==========================================================================================");
        androidId = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        Log.d(TAG_HH, "androidId: " + androidId);

        /** Get App Name */
        ApplicationInfo applicationInfo = getApplicationContext().getApplicationInfo();
        int stringId = applicationInfo.labelRes;
        appName = (stringId == 0) ? applicationInfo.nonLocalizedLabel.toString() : getApplicationContext().getString(stringId);
        Log.d(TAG_HH, "appName: " + appName);

        /** Set Client ID */
        clientId = appName + "-" + androidId;    // ClientID는 중복이 되지 않도록 appName과 androidID의 조합으로 사용


        // 이 단말에서 사용하는 token 값을 가져와서 출력하고 MQTT로 token 값을 서버로 전송
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.d(TAG_HH, "Fetching FCM registration token failed " + task.getException());
                        return;
                    }
                    token = task.getResult();
                    Log.d(TAG_HH, "Registered token : " + token);

                   // JSON 만들기
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put("id", clientId);
                        jsonObject.put("token", token);

                        Log.d(TAG_HH, "JSON msg: "+ jsonObject.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    // JSON parsing (for test)
                    /*try {
                        JSONObject jsonObject2 = new JSONObject(jsonObject.toString());
                        String android_id2 = jsonObject2.getString("id");
                        String token2 = jsonObject2.getString("token");
                        Log.d(TAG_HH, "JSON - id & token: "+ android_id2 + " " + token2);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }*/

                    // mqtt로 token 값 서버에 전달
                    try {
                        mqttClient = new MqttClient(BROKER_ADDR, clientId, persistence);
                        MqttConnectOptions connOpts = new MqttConnectOptions();
                        connOpts.setCleanSession(true);
                        Log.d(TAG_HH, "Connecting to broker: "+ BROKER_ADDR);
                        mqttClient.connect(connOpts);
                        Log.d(TAG_HH, "Connected");
                        Toast.makeText(getApplicationContext(), "MQTT 연결 성공", Toast.LENGTH_SHORT).show();
                        Log.d(TAG_HH, "Publishing message: "+ token);

                        MqttMessage message = new MqttMessage(jsonObject.toString().getBytes());    // 전송할 메시지 (JSON 메시지 전송)
                        message.setQos(QOS);
                        mqttClient.publish(PUB_TOPIC, message);     // 메시지 publish
                        Log.d(TAG_HH, "Message published");
                        Toast.makeText(getApplicationContext(), "Token 전송", Toast.LENGTH_SHORT).show();

                        mqttClient.disconnect();
                        Log.d(TAG_HH, "Disconnected");

                        // 메시지 subscribe
                        /*mqttClient.subscribe(SUB_TOPIC, new IMqttMessageListener() {
                            @Override
                            public void messageArrived(String topic, MqttMessage message) throws Exception {
                                Log.d("Received MQTT msg- " + "topic: " + topic + ", msg: " + message.toString());
                            }
                        });*/
                    } catch (MqttException me) {
                        Log.d(TAG_HH, "reason " + me.getReasonCode());
                        Log.d(TAG_HH, "msg " + me.getMessage());
                        Log.d(TAG_HH, "loc " + me.getLocalizedMessage());
                        Log.d(TAG_HH, "cause " + me.getCause());
                        Log.d(TAG_HH, "excep " + me);
                        me.printStackTrace();
                        Toast.makeText(getApplicationContext(), "MQTT 실패", Toast.LENGTH_SHORT).show();
                    }
                });

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()) {
            webView.goBack();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private class WebViewClientClass extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {     // 현재 페이지에 URL을 읽어오는 메서드
            view.loadUrl(url);
            return true;
        }
    }
}
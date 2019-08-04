package com.zhang.jitvoice;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import com.zhang.voice.VoiceDoc;

import static java.lang.System.out;

public class MainActivity extends AppCompatActivity {
//    static {
//        VoiceDoc.loadVoiceLibrary();
//    }

    //声波
    VoiceDoc voiceDoc;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        voiceDoc = VoiceDoc.getInstance(this, new VoiceDoc.VoiceCallBack() {
            @Override
            public void receiveWave(String content) {
                super.receiveWave(content);
                //收到名片
                out.println("-----------------<<<>>>--------------------收到名片:" + content);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        voiceDoc.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        voiceDoc.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        voiceDoc.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        voiceDoc.onDestroy();
    }

    public void sendwave(View view) {
        voiceDoc.sendWave("userid:169");
    }
}

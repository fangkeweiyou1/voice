package com.zhang.jitvoice;

import android.app.Application;

import com.zhang.voice.VoiceDoc;

/**
 * Created by zhangyuncai on 2019/8/4.
 */
public class DemoApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
//        System.loadLibrary("voiceRecog");
        VoiceDoc.loadVoiceLibrary();
    }
}

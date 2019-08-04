package com.zhang.voice;

import android.Manifest;
import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;

import voice.JoinChannelVO;
import voice.JoinFriendVO;
import voice.SSIDWiFiInfo;
import voice.decoder.DataDecoder;
import voice.decoder.VoiceRecognizer;
import voice.decoder.VoiceRecognizerListener;
import voice.encoder.DataEncoder;
import voice.encoder.VoicePlayer;
import voice.encoder.VoicePlayerListener;

/**
 * Created by zhangyuncai on 2019/8/4.
 * 声波通讯工具类
 * todo 先加载so库,然后构造VoiceDoc对象,然后监听各个生命周期即可
 */
public class VoiceDoc {
    private static VoiceDoc voiceDoc;
    private static final String TAG = "VoiceDoc";
    private Context context;

    //播放器(播放声音不需要权限)
    private VoicePlayer player;
    //录音识别器(录音需要权限)
    private VoiceRecognizer voiceRecognizer;
    //采集频率
    private static final int sampleRate = 44100;
    //频率
    private static final int[] freqs = new int[19];

    //当前音量大小
    int currentVolume = 0;

    private VoiceCallBack voiceCallBack;

    public VoiceDoc(Context context, VoiceCallBack voiceCallBack) {
        this.context = context;
        this.voiceCallBack = voiceCallBack;
        player = getPlayer();
    }

    /**
     * 导入so库
     */
    public static void loadVoiceLibrary() {
        //导入so库
        System.loadLibrary("voiceRecog");
    }

    public static VoiceDoc getInstance(Context context, VoiceCallBack voiceCallBack) {
        if (voiceDoc == null) {
            voiceDoc = new VoiceDoc(context, voiceCallBack);
            return voiceDoc;
        }
        return voiceDoc;
    }

    /**
     * 发送声波
     *
     * @param content 声波内容
     */
    public void sendWave(String content) {
        //设置音量
        autoSetAudioVolumn();
        //播放任意字符串
        String encodeData = DataEncoder.encodeString(content);
        getPlayer().play(encodeData, 1, 500);
    }

    /**
     * 播放哔的一声(设置静音就无效了)
     */
    private static void playBi() {
        new ToneGenerator(
                AudioManager.STREAM_SYSTEM, ToneGenerator.MAX_VOLUME).startTone(ToneGenerator.TONE_PROP_BEEP);
    }

    private VoicePlayer getPlayer() {
        if (player == null) {
            int baseFreq = 16000;
            for (int i = 0; i < freqs.length; i++) {
                freqs[i] = baseFreq + i * 150;
            }
            player = new VoicePlayer(sampleRate);
            player.setFreqs(freqs);
        }
        VoicePlayerListener voicePlayerListener = new VoicePlayerListener() {
            /**
             * 播放开始
             * @param voicePlayer
             */
            @Override
            public void onPlayStart(VoicePlayer voicePlayer) {
                Log.i(TAG, "播放开始");
                if (voiceCallBack != null) {
                    voiceCallBack.onPlayStart(voicePlayer);
                }
                if (getVoiceRecognizer() != null) {
                    getVoiceRecognizer().stop();
                }
            }

            /**
             * 播放结束
             * @param voicePlayer
             */
            @Override
            public void onPlayEnd(VoicePlayer voicePlayer) {
                Log.i(TAG, "播放结束");
                resetAudioVolumn();
                if (voiceCallBack != null) {
                    voiceCallBack.onPlayEnd(voicePlayer);
                }
                if (getVoiceRecognizer() != null) {
                    getVoiceRecognizer().start();
                }
            }
        };
        player.setListener(voicePlayerListener);
        return player;
    }

    /**
     * 创建识别器(如果没有权限就返回null)
     */
    private VoiceRecognizer getVoiceRecognizer() {
        if (voiceRecognizer != null) {
            return voiceRecognizer;
        } else {
            //如果有录音权限
            if ((ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)) >= 0) {
                voiceRecognizer = new VoiceRecognizer(sampleRate);
                voiceRecognizer.setFreqs(freqs);

                VoiceRecognizerListener voiceRecognizerListener = new VoiceRecognizerListener() {
                    /**
                     * 识别开始
                     */
                    @Override
                    public void onRecognizeStart(float _soundTime) {
                        Log.i(TAG, "识别开始");
                        if (voiceCallBack != null) {
                            voiceCallBack.onRecognizeStart(_soundTime);
                        }
                    }

                    /**
                     * 识别结束
                     */
                    @Override
                    public void onRecognizeEnd(float _soundTime, int _result, String _hexData) {

                        if (voiceCallBack != null) {
                            voiceCallBack.onRecognizeEnd(_soundTime, _result, _hexData);
                        }
                        //数据
                        String data = "";
                        //如果采集成功
                        if (_result == 0) {
                            byte[] hexData = _hexData.getBytes();
                            int infoType = DataDecoder.decodeInfoType(hexData);
                            if (infoType == DataDecoder.IT_STRING) {
                                data = DataDecoder.decodeString(_result, hexData);
                            } else if (infoType == DataDecoder.IT_SSID_WIFI) {
                                SSIDWiFiInfo wifi = DataDecoder.decodeSSIDWiFi(_result, hexData);
                                data = "ssid:" + wifi.ssid + ",pwd:" + wifi.pwd;
                            } else if (infoType == 2) {
                                JoinChannelVO vo = DataDecoder.decodeJoinChannel(_result, hexData);
                                data = "uid:tid:" + vo.tid + ",uid:" + vo.uid + ",checkCode:" + vo.checkCode;
                            } else if (infoType == 5) {
                                JoinFriendVO vo = DataDecoder.decodeJoinFriend(_result, hexData);
                                data = "sn:" + vo.sn + ",uid:" + vo.uid + ",ouid:" + vo.ouid + ",nickName:" + vo.nickName;
                            } else {
                                data = "未知数据";
                            }
                        }
                        Log.i(TAG, "识别结束,声波内容:" + data);
                        if (voiceCallBack != null && !TextUtils.isEmpty(data)) {
                            voiceCallBack.receiveWave(data);
                        }
                    }

                };
                voiceRecognizer.setListener(voiceRecognizerListener);
            }
        }
        return null;
    }


    /**
     * 设置音量
     */
    private void autoSetAudioVolumn() {
        AudioManager mAudioManager = (AudioManager) (context.getSystemService(Context.AUDIO_SERVICE));
        //获取手机最大音量
        int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        //设置最大音量
        int setVolume = (int) (maxVolume * 0.8f);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0);
    }

    /**
     * 重置音量
     */
    private void resetAudioVolumn() {
        if(currentVolume>0) {
            AudioManager mAudioManager = (AudioManager) (context.getSystemService(Context.AUDIO_SERVICE));
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0);
        }
    }

    /**
     * 界面生命周期
     */
    public void onStart() {
        if (getVoiceRecognizer() != null) {
            getVoiceRecognizer().start();
        }
    }

    /**
     * 界面生命周期
     */
    public void onResume() {
        if (getVoiceRecognizer() != null) {
            getVoiceRecognizer().start();
        }
    }

    /**
     * 界面生命周期
     */
    public void onPause() {
        if (getVoiceRecognizer() != null) {
            getVoiceRecognizer().stop();
        }
    }

    /**
     * 界面生命周期
     */
    public void onDestroy() {
        Log.i(TAG, "停止播放声音");
        getPlayer().stop();
    }

    /**
     * 声波通讯监听
     */
    public static class VoiceCallBack implements VoicePlayerListener, VoiceRecognizerListener {
        /**
         * 识别开始
         */
        @Override
        public void onRecognizeStart(float voicePlayer) {

        }

        /**
         * 识别结束
         */
        @Override
        public void onRecognizeEnd(float v, int i, String s) {

        }

        /**
         * 播放开始
         *
         * @param voicePlayer
         */
        @Override
        public void onPlayStart(VoicePlayer voicePlayer) {

        }

        /**
         * 播放结束
         *
         * @param voicePlayer
         */
        @Override
        public void onPlayEnd(VoicePlayer voicePlayer) {

        }

        /**
         * 收到声波
         *
         * @param content 声波内容
         */
        public void receiveWave(String content) {

        }
    }

}

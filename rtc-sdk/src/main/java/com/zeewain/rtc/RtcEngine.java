package com.zeewain.rtc;

import com.zeewain.rtc.internal.RtcEngineImpl;
import com.zeewain.rtc.model.CameraConfig;

import org.webrtc.SurfaceViewRenderer;

public abstract class RtcEngine {

    protected static RtcEngineImpl mInstance = null;

    public RtcEngine() {

    }

    public static synchronized RtcEngine create(RtcEngineConfig config) {
        if (config != null) {
            if (mInstance == null) {
                mInstance = new RtcEngineImpl(config);
            }
            return mInstance;
        }
        return null;
    }

    public static synchronized void destroy() {
        if (mInstance != null) {
            mInstance.doDestroy();
            mInstance = null;
            System.gc();
        }
    }

    public abstract String getRoomLink();

    public abstract int joinChannel();

    public abstract int leaveChannel();

    public abstract int closeChannel();

    public abstract void setupCameraConfig(CameraConfig config);

    public abstract int enableVideo();

    public abstract int hasVideoAvailable();

    public abstract int hasAudioAvailable();

    public abstract int disableVideo();

    public abstract int enableAudio();

    public abstract int disableAudio();

    public abstract int switchCamera();

    public abstract int setupLocalVideo(SurfaceViewRenderer viewRenderer);

    public abstract int setupRemoteVideo(SurfaceViewRenderer viewRenderer, String uid);

    public abstract int sendChatMessage(String text);

    public abstract int sendBotMessage(String text);

    public abstract int restartICE(String transportId);

    public abstract int changeDisplayUserName(String targetName);

    public abstract int startFusion();

    public abstract int stopFusion();

    public abstract int updateFusionSetting(int userCount, float scale, float fromBottomRatio, float scaleFromLeft, float scaleFromWidth, int rotationAngle);

    public abstract int updateFusionBackground(String imageUrl);

}

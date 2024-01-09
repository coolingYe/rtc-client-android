package com.zeewain.rtc;

import com.zeewain.rtc.internal.RtcEngineImpl;
import com.zeewain.rtc.model.CameraCapturerConfiguration;

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

    public abstract void joinChannel();

    public abstract void leaveChannel();

    public abstract void closeChannel();

    public abstract void setupCameraCapturerConfiguration(CameraCapturerConfiguration config);

    public abstract void enableVideo();

    public abstract Boolean hasVideoAvailable();

    public abstract Boolean hasAudioAvailable();

    public abstract void disableVideo();

    public abstract void enableAudio();

    public abstract void disableAudio();

    public abstract void switchCamera();

    public abstract void setupLocalVideo(SurfaceViewRenderer viewRenderer);

    public abstract void setupRemoteVideo(SurfaceViewRenderer viewRenderer, String uid);

    public abstract void sendChatMessage(String text);

    public abstract void restartICE(String transportId);

    public abstract void changeDisplayUserName(String targetName);

    public abstract void startFusion();

    public abstract void stopFusion();

    public abstract void updateFusionSetting(int userCount, float scale, float fromBottomRatio, float scaleFromLeft, float scaleFromWidth, int rotationAngle);

    public abstract void updateFusionBackground(String imageUrl);


}

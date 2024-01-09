package com.zeewain.rtc.internal;

import androidx.annotation.NonNull;

import com.zeewain.rtc.RtcEngineConfig;
import com.zeewain.rtc.model.CameraCapturerConfiguration;

import org.webrtc.SurfaceViewRenderer;

public class RtcEngineImpl extends RtcEngineEx  {

    public RtcEngineImpl(@NonNull RtcEngineConfig config) {
        super(config);
    }

    public synchronized void doDestroy() {
        this.nativeDestroy();
    }

    @Override
    public synchronized String getRoomLink() {
        return this.nativeGetRoomLink();
    }

    @Override
    public synchronized void joinChannel() {
        this.nativeJoinChannel();
    }

    @Override
    public synchronized void leaveChannel() {
        this.nativeLeaveChannel();
    }

    @Override
    public synchronized void closeChannel() {
        this.nativeCloseChannel();
    }

    @Override
    public synchronized void setupCameraCapturerConfiguration(CameraCapturerConfiguration config) {
        this.nativeSetupCameraCapturerConfiguration(config);
    }

    @Override
    public synchronized void enableVideo() {
        this.nativeEnableCamera();
    }

    @Override
    public synchronized Boolean hasVideoAvailable() {
        return this.nativeHasVideoAvailable();
    }

    @Override
    public synchronized Boolean hasAudioAvailable() {
        return this.nativeHasAudioAvailable();
    }

    @Override
    public synchronized void disableVideo() {
        this.nativeDisableCamera();
    }

    @Override
    public synchronized void enableAudio() {
        this.nativeEnableAudio();
    }

    @Override
    public synchronized void disableAudio() {
        this.nativeDisableAudio();
    }

    @Override
    public synchronized void switchCamera() {
        this.nativeSwitchCamera();
    }

    @Override
    public void setupLocalVideo(SurfaceViewRenderer viewRenderer) {
        this.nativeSetupLocalVideo(viewRenderer);
    }

    @Override
    public void setupRemoteVideo(SurfaceViewRenderer viewRenderer, String uid) {
        this.nativeSetupRemoteVideo(viewRenderer, uid);
    }


    @Override
    public void sendChatMessage(String text) {
        this.nativeSendChatMessage(text);
    }

    @Override
    public void restartICE(String transportId) {
        this.nativeStartICE(transportId);
    }

    @Override
    public void changeDisplayUserName(String targetName) {
        this.nativeChangeDisplayUserName(targetName);
    }

    @Override
    public void startFusion() {
        this.nativeStartFusion();
    }

    @Override
    public void stopFusion() {
        this.nativeStopFusion();
    }

    @Override
    public void updateFusionSetting(int userCount, float scale, float fromBottomRatio, float scaleFromLeft, float scaleFromWidth, int rotationAngle) {
        this.nativeUpdateFusionSetting(userCount, scale, fromBottomRatio, scaleFromLeft, scaleFromWidth, rotationAngle);
    }

    @Override
    public void updateFusionBackground(String imageUrl) {
        this.nativeUpdateFusionBackground(imageUrl);
    }

}

package com.zeewain.rtc.internal;

import androidx.annotation.NonNull;

import com.zeewain.rtc.RtcEngineConfig;
import com.zeewain.rtc.model.CameraConfig;

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
    public synchronized int joinChannel() {
        return this.nativeJoinChannel();
    }

    @Override
    public synchronized int leaveChannel() {
        return this.nativeLeaveChannel();
    }

    @Override
    public synchronized int closeChannel() {
        return this.nativeCloseChannel();
    }

    @Override
    public synchronized void setupCameraConfig(CameraConfig config) {
        this.nativeSetupCameraConfig(config);
    }

    @Override
    public synchronized int enableVideo() {
        return this.nativeEnableCamera();
    }

    @Override
    public synchronized int hasVideoAvailable() {
        return this.nativeHasVideoAvailable();
    }

    @Override
    public synchronized int hasAudioAvailable() {
        return this.nativeHasAudioAvailable();
    }

    @Override
    public synchronized int disableVideo() {
        return this.nativeDisableCamera();
    }

    @Override
    public synchronized int enableAudio() {
        return this.nativeEnableAudio();
    }

    @Override
    public synchronized int disableAudio() {
        return this.nativeDisableAudio();
    }

    @Override
    public synchronized int switchCamera() {
        return this.nativeSwitchCamera();
    }

    @Override
    public synchronized int setupLocalVideo(SurfaceViewRenderer viewRenderer) {
        return this.nativeSetupLocalVideo(viewRenderer);
    }

    @Override
    public synchronized int setupRemoteVideo(SurfaceViewRenderer viewRenderer, String uid) {
        return this.nativeSetupRemoteVideo(viewRenderer, uid);
    }

    @Override
    public synchronized int sendChatMessage(String text) {
       return this.nativeSendChatMessage(text);
    }

    @Override
    public synchronized int sendBotMessage(String text) {
        return this.nativeSendBotMessage(text);
    }

    @Override
    public synchronized int restartICE(String transportId) {
        return this.nativeStartICE(transportId);
    }

    @Override
    public synchronized int changeDisplayUserName(String targetName) {
        return this.nativeChangeDisplayUserName(targetName);
    }

    @Override
    public synchronized int startFusion() {
        return this.nativeStartFusion();
    }

    @Override
    public synchronized int stopFusion() {
        return this.nativeStopFusion();
    }

    @Override
    public synchronized int updateFusionSetting(int userCount, float scale, float fromBottomRatio, float scaleFromLeft, float scaleFromWidth, int rotationAngle) {
        return this.nativeUpdateFusionSetting(userCount, scale, fromBottomRatio, scaleFromLeft, scaleFromWidth, rotationAngle);
    }

    @Override
    public synchronized int updateFusionBackground(String imageUrl) {
        return this.nativeUpdateFusionBackground(imageUrl);
    }

}

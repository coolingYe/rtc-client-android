package com.zeewain.rtc.model;

import org.webrtc.CalledByNative;

public class CameraConfig {
    public enum CAMERA_DIRECTION {
        CAMERA_REAR(0),
        CAMERA_FRONT(1);
        private int value;
        private CAMERA_DIRECTION(int v) {
            value = v;
        }
        public int getValue() {
            return this.value;
        }
    }
    public CAMERA_DIRECTION cameraDirection;
    static public class CaptureFormat {
        public int width;
        public int height;
        public int fps;
        public CaptureFormat(int width, int height, int fps) {
            this.width = width;
            this.height = height;
            this.fps = fps;
        }
        public CaptureFormat() {
            this.width = 640;
            this.height = 480;
            this.fps = 15;
        }
        @CalledByNative("CaptureFormat")
        public int getHeight() {
            return height;
        }
        @CalledByNative("CaptureFormat")
        public int getWidth() {
            return width;
        }
        @CalledByNative("CaptureFormat")
        public int getFps() {
            return fps;
        }
        @Override
        public String toString() {
            return "CaptureFormat{"
                    + "width=" + width + ", height=" + height + ", fps=" + fps + '}';
        }
    }
    public CaptureFormat captureFormat;
    public boolean followEncodeDimensionRatio;
    public CameraConfig(CAMERA_DIRECTION cameraDirection) {
        this.cameraDirection = cameraDirection;
        this.captureFormat = new CaptureFormat();
        this.followEncodeDimensionRatio = true;
    }
    public CameraConfig(CaptureFormat captureFormat) {
        this.captureFormat = captureFormat;
        this.cameraDirection = CAMERA_DIRECTION.CAMERA_FRONT;
        this.followEncodeDimensionRatio = true;
    }
    public CameraConfig(
            CAMERA_DIRECTION cameraDirection, CaptureFormat captureFormat) {
        this.cameraDirection = cameraDirection;
        this.captureFormat = captureFormat;
        this.followEncodeDimensionRatio = true;
    }
    @CalledByNative
    public int getCameraDirection() {
        return cameraDirection.value;
    }
    @CalledByNative
    public CaptureFormat getCaptureFormat() {
        return captureFormat;
    }
    @CalledByNative
    public boolean isFollowEncodeDimensionRatio() {
        return followEncodeDimensionRatio;
    }
    @Override
    public String toString() {
        return "CameraCapturerConfiguration{"
                + "cameraDirection=" + cameraDirection + ", captureDimensions=" + captureFormat
                + ", followEncodeDimensionRatio=" + followEncodeDimensionRatio + '}';
    }
}


package com.zeewain.rtc;

import com.zeewain.rtc.model.Notification;

import org.mediasoup.droid.MediasoupException;

import java.util.concurrent.ExecutionException;

public interface IRtcEventHandler {

    void onOpen() throws MediasoupException, ExecutionException, InterruptedException;
    void onFail();
    void onNotification(Notification notification);
    void onDisconnected();
    void onClose();
}

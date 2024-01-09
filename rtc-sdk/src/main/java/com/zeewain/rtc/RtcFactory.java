package com.zeewain.rtc;

import android.content.Context;

import org.mediasoup.droid.Logger;
import org.mediasoup.droid.MediasoupClient;

public class RtcFactory {

    public static void initialize(Context appContext) {
        Logger.setLogLevel(Logger.LogLevel.LOG_DEBUG);
        Logger.setDefaultHandler();
        MediasoupClient.initialize(appContext);
    }
}

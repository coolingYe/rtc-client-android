package com.zeewain.rtc;

import android.content.Context;

public class RtcEngineConfig {

    public Context context = null;
    public int channelProfile = 0;
    public IRtcEngineEventHandler eventHandler = null;
    public String roomId;
    public String userId;
    public String token;
    public String appId;
    public String displayName;
    public String backgroundUrl;
}

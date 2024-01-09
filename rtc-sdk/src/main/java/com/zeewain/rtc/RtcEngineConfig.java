package com.zeewain.rtc;

import android.content.Context;

public class RtcEngineConfig {
    public Context mContext = null;
    public int mChannelProfile = 0;
    public IRtcEngineEventHandler mEventHandler = null;
    public String mServerUrl;      //< 服务器url(require)
    public String mRoomId;         //< 房间id
    public String mUserId;         //< 用户id
    public String mUserToken;      //< 用户授权token
    public String mAppId;          //< 用户授权appid
    public String mDisplayName;    // optional
    public String mBackgroundUrl; //融合背景
}

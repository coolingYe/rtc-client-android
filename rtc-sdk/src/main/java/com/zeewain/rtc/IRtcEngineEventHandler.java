package com.zeewain.rtc;

import com.alibaba.fastjson.JSONArray;

public interface IRtcEngineEventHandler {

    /**
     * Executed when the underlying event is error.
     * @param err
     */
    void onError(int err);

    /**
     * Executed when joining the room successfully.
     * @param uid user ID
     */
    void onJoinChannelSuccess(String uid);

    /**
     * Executed when leaving the room.
     */
    void onLeaveChannel();

    /**
     * Executed when the room closed.
     */
    void onCloseChannel();

    /**
     * Executed when the remote user is joined.
     * @param uid The identifier of the remote user.
     */
    void onUserJoined(String uid);

    /**
     * Executed when the remote user is offline.
     * @param uid The identifier of the remote user.
     */
    void onUserOffline(String uid);

    /**
     * Executed when the remote user is online.
     * @param userInfo Return all online user information.
     */
    void onUserOnline(JSONArray userInfo);

    /**
     * Executed when the remote user open camera when remote video track state changed.
     * @param uid The identifier of the remote user.
     * @param trackId The identifier of the remote user video track.
     * @param state The state of the remote user video track. true is available false is unavailable.
     */
    void onRemoteVideoStateChanged(String uid, String trackId, boolean state);

    /**
     * Executed when the remote user message is received.
     * @param uid The identifier of the remote user.
     * @param message The remote user message.
     */
    void onUserMessage(String uid, String message);

}

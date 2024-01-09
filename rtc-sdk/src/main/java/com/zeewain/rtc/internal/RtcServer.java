package com.zeewain.rtc.internal;

import com.alibaba.fastjson.JSONObject;
import com.zeewain.cbb.common.core.resp.Response;
import com.zeewain.cbb.netty.core.NettyMapping;
import com.zeewain.cbb.netty.protocol.NettyResponse;

import io.netty.channel.Channel;
import io.netty.util.concurrent.Promise;

public interface RtcServer {

    @NettyMapping("INIT_CLIENT")
    Promise<NettyResponse<JSONObject>> initClient(Channel channel, JSONObject json);

    @NettyMapping("CREATE_ROOM")
    Promise<Response<JSONObject>> createRoom(Channel channel, JSONObject json);

    @NettyMapping("CREATE_ROOM_MEDIASOUP")
    Promise<NettyResponse<JSONObject>> createRoomMediasoup(Channel channel, JSONObject json);

    @NettyMapping("JOIN_ROOM")
    Promise<NettyResponse<JSONObject>> joinRoom(Channel channel, JSONObject json);

    @NettyMapping("JOIN_ROOM_MEDIASOUP")
    Promise<NettyResponse<JSONObject>> joinRoomMediaSoup(Channel channel, JSONObject json);

    @NettyMapping("EXIT_ROOM")
    Promise<NettyResponse<JSONObject>> exitRoom(Channel channel, JSONObject json);

    @NettyMapping("CLOSE_ROOM")
    Promise<NettyResponse<JSONObject>> closeRoom(Channel channel, JSONObject json);

    @NettyMapping("GET_ROUTER_RTPCAPABILITIES")
    Promise<Response<JSONObject>> getRouterRtpCapabilities(Channel channel, JSONObject json);

    @NettyMapping("CREATE_WEBRTC_TRANSPORT")
    Promise<Response<JSONObject>> createTransport(Channel channel, JSONObject json);

    @NettyMapping("CONNECT_WEBRTC_TRANSPORT")
    Promise<Response<JSONObject>> connectTransport(Channel channel, JSONObject json);

    @NettyMapping("CREATE_USER_PRODUCE")
    Promise<Response<JSONObject>> createUserProduce(Channel channel, JSONObject json);

    @NettyMapping("CREATE_USER_TEXT_PRODUCE")
    Promise<Response<JSONObject>> createUserTextProduce(Channel channel, JSONObject json);

    @NettyMapping("CLOSE_USER_PRODUCER")
    Promise<Response<JSONObject>> closeUserProducer(Channel channel, JSONObject json);

    @NettyMapping("RESTART_ICE")
    Promise<Response<JSONObject>> restartICE(Channel channel, JSONObject json);

    @NettyMapping("CHANGE_DISPLAY_USERNAME")
    Promise<Response<JSONObject>> changeDisplayUserName(Channel channel, JSONObject json);

    @NettyMapping("START_FUSION")
    Promise<Response<JSONObject>> startFusion(Channel channel, JSONObject json);

    @NettyMapping("STOP_FUSION")
    Promise<Response<JSONObject>> stopFusion(Channel channel, JSONObject json);

    @NettyMapping("UPDATE_FUSION_SETTING")
    Promise<Response<JSONObject>> updateFusionSetting(Channel channel, JSONObject json);

    @NettyMapping("UPDATE_FUSION_BG_URL")
    Promise<Response<JSONObject>> updateFusionBackground(Channel channel, JSONObject json);
}

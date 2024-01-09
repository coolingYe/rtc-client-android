package com.zeewain.rtc.internal;

import android.content.Context;
import android.content.Intent;

import com.alibaba.fastjson.JSONObject;
import com.zeewain.cbb.netty.core.Processor;
import com.zeewain.cbb.netty.mvc.NettyProcessorManager;
import com.zeewain.cbb.netty.protocol.NettyResponse;

public class RtcServerProcessor {

    public static void registerReceiver(Context context) {

        Processor<JSONObject> roomJoined = new Processor<JSONObject>() {
            @Override
            public String getMsgCode() {
                return "ROOM_JOINED";
            }

            @Override
            public Object process(JSONObject param) {
                sendMessage(context, getMsgCode(), param);
                return null;
            }
        };

        NettyProcessorManager.register(roomJoined.getMsgCode(), roomJoined);

        Processor<JSONObject> userJoined = new Processor<JSONObject>() {
            @Override
            public String getMsgCode() {
                return "USER_JOINED";
            }

            @Override
            public Object process(JSONObject param) {
                sendMessage(context, getMsgCode(), param);
                return null;
            }
        };

        NettyProcessorManager.register(userJoined.getMsgCode(), userJoined);

        Processor<JSONObject> userLeft = new Processor<JSONObject>() {
            @Override
            public String getMsgCode() {
                return "USER_LEFT";
            }

            @Override
            public Object process(JSONObject param) {
                sendMessage(context, getMsgCode(), param);
                return null;
            }
        };

        NettyProcessorManager.register(userLeft.getMsgCode(), userLeft);

        Processor<JSONObject> usersOnline = new Processor<JSONObject>() {
            @Override
            public String getMsgCode() {
                return "USERS_ONLINE";
            }

            @Override
            public Object process(JSONObject param) {
                sendMessage(context, getMsgCode(), param);
                return null;
            }
        };

        NettyProcessorManager.register(usersOnline.getMsgCode(), usersOnline);

        Processor<JSONObject> roomClose = new Processor<JSONObject>() {
            @Override
            public String getMsgCode() {
                return "ROOM_CLOSED";
            }

            @Override
            public Object process(JSONObject param) {
                sendMessage(context, getMsgCode(), param);
                return null;
            }
        };

        NettyProcessorManager.register(roomClose.getMsgCode(), roomClose);

        Processor<JSONObject> createUserConsumer = new Processor<JSONObject>() {
            @Override
            public String getMsgCode() {
                return "CREATE_USER_CONSUMER";
            }

            @Override
            public Object process(JSONObject param) {

                sendMessage(context, getMsgCode(), param);
                JSONObject data = new JSONObject();
                data.put("roomId", param.getString("roomId"));
                data.put("userId", param.getString("userId"));
                data.put("messageType", "response");
                return NettyResponse.success(data);
            }
        };

        NettyProcessorManager.register(createUserConsumer.getMsgCode(), createUserConsumer);

        Processor<JSONObject> textConsumer = new Processor<JSONObject>() {
            @Override
            public String getMsgCode() {
                return "CREATE_USER_TEXT_CONSUMER";
            }

            @Override
            public Object process(JSONObject param) {
                sendMessage(context, getMsgCode(), param);
                JSONObject data = new JSONObject();
                data.put("roomId", param.getString("roomId"));
                data.put("userId", param.getString("userId"));
                data.put("messageType", "response");
                return NettyResponse.success(data);
            }
        };

        NettyProcessorManager.register(textConsumer.getMsgCode(), textConsumer);

        Processor<JSONObject> closeUserConsumer = new Processor<JSONObject>() {
            @Override
            public String getMsgCode() {
                return "USER_CONSUMER_CLOSED";
            }

            @Override
            public Object process(JSONObject param) {
                sendMessage(context, getMsgCode(), param);
                return null;
            }
        };

        NettyProcessorManager.register(closeUserConsumer.getMsgCode(), closeUserConsumer);

        Processor<JSONObject> scoreConsumer = new Processor<JSONObject>() {
            @Override
            public String getMsgCode() {
                return "CONSUMER_SCORE";
            }

            @Override
            public Object process(JSONObject param) {
                sendMessage(context, getMsgCode(), param);
                return null;
            }
        };

        NettyProcessorManager.register(scoreConsumer.getMsgCode(), scoreConsumer);

        Processor<JSONObject> scoreProducer = new Processor<JSONObject>() {
            @Override
            public String getMsgCode() {
                return "PRODUCER_SCORE";
            }

            @Override
            public Object process(JSONObject param) {
                sendMessage(context, getMsgCode(), param);
                return null;
            }
        };

        NettyProcessorManager.register(scoreProducer.getMsgCode(), scoreProducer);
    }

    private static void sendMessage(Context context, String method, JSONObject data) {
        Intent intent = new Intent("test");
        intent.putExtra("message", method);
        intent.putExtra("data", data);
        context.sendBroadcast(intent);
    }

}

package com.zeewain.rtc.model;

import com.alibaba.fastjson.JSONObject;

public class Notification {
    public String method;
    public JSONObject data;

    public Notification(String method, JSONObject data) {
        this.method = method;
        this.data = data;
    }
}

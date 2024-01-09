package com.zeewain.rtc.model;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSONObject;

import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("WeakerAccess")
public class Peer extends Info {

  private String mId;
  private String mDisplayName;
  private DeviceInfo mDevice;

  private Set<String> mConsumers;
  private Set<String> mDataConsumers;

  public Peer(@NonNull JSONObject info) {
    mId = info.getString("id");
    mDisplayName = info.getString("displayName");
    JSONObject deviceInfo = info.getJSONObject("device");
    if (deviceInfo != null) {
      mDevice =
          new DeviceInfo()
              .setFlag(deviceInfo.getString("flag"))
              .setName(deviceInfo.getString("name"))
              .setVersion(deviceInfo.getString("version"));
    } else {
      mDevice = DeviceInfo.unknownDevice();
    }
    mConsumers = new HashSet<>();
    mDataConsumers = new HashSet<>();
  }

  @Override
  public String getId() {
    return mId;
  }

  @Override
  public String getDisplayName() {
    return mDisplayName;
  }

  @Override
  public DeviceInfo getDevice() {
    return mDevice;
  }

  public void setDisplayName(String displayName) {
    this.mDisplayName = displayName;
  }

  public void setDevice(DeviceInfo device) {
    this.mDevice = device;
  }

  public Set<String> getConsumers() {
    return mConsumers;
  }
  public Set<String> getDataConsumers() {
    return mDataConsumers;
  }
}

package com.zeewain.rtc.internal;

import static com.zeewain.rtc.IRtcEngineEventHandler.ErrorCode.ERR_CONNECTION_LOST;

import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.zeewain.ClientFactory;
import com.zeewain.cbb.common.core.resp.Response;
import com.zeewain.cbb.netty.client.StartupClient;
import com.zeewain.cbb.netty.core.Processor;
import com.zeewain.cbb.netty.mvc.NettyProcessorManager;
import com.zeewain.cbb.netty.protocol.NettyResponse;
import com.zeewain.rtc.IRtcEngineEventHandler;
import com.zeewain.rtc.IRtcEventHandler;
import com.zeewain.rtc.RtcEngine;
import com.zeewain.rtc.RtcEngineConfig;
import com.zeewain.rtc.lv.RoomStore;
import com.zeewain.rtc.model.CameraCapturerConfiguration;
import com.zeewain.rtc.model.Notification;
import com.zeewain.utils.NetworkUtils;
import com.zeewain.utils.PeerConnectionUtils;

import org.mediasoup.droid.Consumer;
import org.mediasoup.droid.DataConsumer;
import org.mediasoup.droid.DataProducer;
import org.mediasoup.droid.Device;
import org.mediasoup.droid.Logger;
import org.mediasoup.droid.MediasoupException;
import org.mediasoup.droid.Producer;
import org.mediasoup.droid.RecvTransport;
import org.mediasoup.droid.SendTransport;
import org.mediasoup.droid.Transport;
import org.webrtc.AudioTrack;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.DataChannel;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import cn.hutool.core.lang.Pair;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

public abstract class RtcEngineEx extends RtcEngine {

    private final String TAG = "RtcEngine";

    private final RtcEngineConfig rtcEngineConfig;

    private final Handler mMainHandler;
    private final Handler mWorkHandler;
    private PeerConnectionUtils mPeerConnectionUtils;
    private StartupClient mStartupClient;
    private final RtcServer mService;

    private SendTransport mSendTransport;
    private RecvTransport mRecvTransport;

    private DataProducer mChatDataProducer;

    private DataProducer mBotDataProducer;
    private Producer mVideoProducer;
    private Producer mAudioProducer;

    private VideoTrack mLocalVideoTrack;
    private AudioTrack mLocalAudioTrack;

    private CameraCapturerConfiguration mCameraConfig;

    private final Device mDevice;

    private Boolean mClosed = false;

    private String serverRoomId;

    @NonNull
    final Map<String, ConsumerHolder> mConsumers;

    @NonNull
    final Map<String, DataConsumerHolder> mDataConsumers;

    @NonNull
    final RoomStore mStore;

    static class ConsumerHolder {
        @NonNull
        final String peerId;
        @NonNull
        final Consumer mConsumer;

        ConsumerHolder(@NonNull String peerId, @NonNull Consumer consumer) {
            this.peerId = peerId;
            this.mConsumer = consumer;
        }
    }

    static class DataConsumerHolder {

        @NonNull
        final String peerId;
        @NonNull
        final DataConsumer mDataConsumer;

        DataConsumerHolder(@NonNull String peerId, @NonNull DataConsumer dataConsumer) {
            this.peerId = peerId;
            mDataConsumer = dataConsumer;
        }
    }

    public RtcEngineEx(@NonNull RtcEngineConfig config) {
        this.rtcEngineConfig = config;
        this.mConsumers = new ConcurrentHashMap<>();
        this.mDataConsumers = new ConcurrentHashMap<>();
        this.mStore = new RoomStore();
    }

    {
        HandlerThread handlerThread = new HandlerThread("worker");
        handlerThread.start();
        mMainHandler = new Handler(Looper.getMainLooper());
        mWorkHandler = new Handler(handlerThread.getLooper());
        mWorkHandler.post(new Runnable() {
            @Override
            public void run() {
                mPeerConnectionUtils = new PeerConnectionUtils();
            }
        });
        mCameraConfig = new CameraCapturerConfiguration(new CameraCapturerConfiguration.CaptureFormat());
        mDevice = new Device();
        mStartupClient = ClientFactory.get();
        mService = ClientFactory.getSender(RtcServer.class);
    }

    public RtcEngineConfig getRtcEngineConfig() {
        return rtcEngineConfig;
    }

    protected void nativeJoinChannel() {
        if (!NetworkUtils.isNetworkAvailable(getRtcEngineConfig().mContext)) {
            getRtcEngineConfig().mEventHandler.onError(ERR_CONNECTION_LOST);
            return;
        }

        JSONObject reqInitClient = new JSONObject();
        reqInitClient.put("roomId", rtcEngineConfig.mRoomId);
        reqInitClient.put("userId", rtcEngineConfig.mUserId);
        reqInitClient.put("userToken", rtcEngineConfig.mUserToken);
        reqInitClient.put("zeewainAppId", rtcEngineConfig.mAppId);
        reqInitClient.put("type", "normal");

        mService.initClient(mStartupClient.getChannel(), reqInitClient).addListener((GenericFutureListener<Future<NettyResponse<JSONObject>>>) future -> {
            if (future.isSuccess()) {
                switch (future.get().getCode()) {
                    case 0:
                        registerReceiver();
                        serverRoomId = future.get().getData().getString("serverRoomId");
                        mIRtcEventHandler.onOpen();
                        break;
                    case 1:
                        getRtcEngineConfig().mEventHandler.onError(IRtcEngineEventHandler.ErrorCode.ERR_INVALID_TOKEN);
                        mWorkHandler.post(() -> {
                            throw new RuntimeException ("Token is invalid");
                        });
                    case 1000:
                        getRtcEngineConfig().mEventHandler.onError(IRtcEngineEventHandler.ErrorCode.ERR_NOT_READY);
                        mWorkHandler.post(() -> {
                            throw new RuntimeException ("Initialization failed. Room number and APP ID and token does not match.");
                        });
                }
            } else {
                mIRtcEventHandler.onDisconnected();
                getRtcEngineConfig().mEventHandler.onError(IRtcEngineEventHandler.ErrorCode.ERR_NOT_READY);
            }
        });
    }

    protected void nativeLeaveChannel() {
        if (!NetworkUtils.isNetworkAvailable(getRtcEngineConfig().mContext)) {
            getRtcEngineConfig().mEventHandler.onError(ERR_CONNECTION_LOST);
            return;
        }

        JSONObject leaveReq = new JSONObject();
        leaveReq.put("userId", rtcEngineConfig.mUserId);
        leaveReq.put("roomId", serverRoomId);
        leaveReq.put("messageType", "request");

        mService.exitRoom(mStartupClient.getChannel(), leaveReq).addListener((GenericFutureListener<Future<NettyResponse<JSONObject>>>) future -> {
            if (future.isSuccess()) {
                rtcEngineConfig.mEventHandler.onLeaveChannel();
                mIRtcEventHandler.onClose();
            }
        });
    }

    protected void nativeCloseChannel() {
        if (!NetworkUtils.isNetworkAvailable(getRtcEngineConfig().mContext)) {
            getRtcEngineConfig().mEventHandler.onError(ERR_CONNECTION_LOST);
            return;
        }

        JSONObject closeReq = new JSONObject();
        closeReq.put("userId", rtcEngineConfig.mUserId);
        closeReq.put("roomId", serverRoomId);
        closeReq.put("messageType", "request");

        mService.closeRoom(mStartupClient.getChannel(), closeReq).addListener((GenericFutureListener<Future<NettyResponse<JSONObject>>>) future -> {
            if (future.isSuccess()) {
                rtcEngineConfig.mEventHandler.onLeaveChannel();
                mIRtcEventHandler.onClose();
            }
        });
    }

    protected void nativeSwitchCamera() {
        mStore.setCamInProgress(true);
        mWorkHandler.post(() -> mPeerConnectionUtils.switchCam(new CameraVideoCapturer.CameraSwitchHandler() {
            @Override
            public void onCameraSwitchDone(boolean b) {
                mStore.setCamInProgress(false);
            }

            @Override
            public void onCameraSwitchError(String s) {
                mStore.setCamInProgress(false);
            }
        }));
    }

    private String getVideoType() {
        if (rtcEngineConfig.mChannelProfile == 0) {
            return "rtc_video_call";
        }
        return "rtc_video_fusion_call";
    }

    private void joinImpl() throws MediasoupException, ExecutionException, InterruptedException {
        if (!NetworkUtils.isNetworkAvailable(getRtcEngineConfig().mContext)) {
            getRtcEngineConfig().mEventHandler.onError(ERR_CONNECTION_LOST);
            return;
        }

        JSONObject rtpReq = new JSONObject();
        rtpReq.put("roomId", serverRoomId);
        rtpReq.put("userId", rtcEngineConfig.mUserId);
        rtpReq.put("type", "request");
        JSONObject routerRtp = mService.getRouterRtpCapabilities(mStartupClient.getChannel(), rtpReq).get().getData();
        Logger.d(TAG, "getRouterRtpCapabilities(): " + routerRtp.toString());
        Object codecs = routerRtp.get("codecs");
        Object headerExtensions = routerRtp.get("headerExtensions");
        JSONObject routerRtpCapabilities = new JSONObject();
        routerRtpCapabilities.put("codecs", codecs);
        routerRtpCapabilities.put("headerExtensions", headerExtensions);
        mDevice.load(routerRtpCapabilities.toJSONString(), null);

        createSendTransport();
        createRecvTransport();

        JSONObject createRoom = new JSONObject();
        createRoom.put("roomId", serverRoomId);
        createRoom.put("userId", rtcEngineConfig.mUserId);
        createRoom.put("videoType", getVideoType());
        if (getRtcEngineConfig().mChannelProfile == 1) {
            createRoom.put("frameWidth", 1920);
            createRoom.put("frameHeight", 1080);
            createRoom.put("fusionType", "StandardFusion");
            createRoom.put("frameRate", 20);
            createRoom.put("rtcType", "ZWNRTC2");
            createRoom.put("backGroundUrl", rtcEngineConfig.mBackgroundUrl);
        }
        mService.createRoom(mStartupClient.getChannel(), createRoom).addListener((GenericFutureListener<Future<Response<JSONObject>>>) future -> {
            if (future.isSuccess()) {
                Logger.d(TAG, "createRoom(): " + future.get().getCode());
                if (future.get().getCode() == 0) {
                    mWorkHandler.post(() -> {
                        JSONObject createRoomMediaSoup = new JSONObject();
                        createRoomMediaSoup.put("roomId", serverRoomId);
                        createRoomMediaSoup.put("userId", rtcEngineConfig.mUserId);
                        createRoomMediaSoup.put("videoType", getVideoType());
                        if (getRtcEngineConfig().mChannelProfile == 1) {
                            createRoom.put("frameWidth", 1920);
                            createRoom.put("frameHeight", 1080);
                            createRoom.put("backGroundUrl", rtcEngineConfig.mBackgroundUrl);
                        }
                        String createRoomMediaSoupInfo;
                        try {
                            createRoomMediaSoupInfo = mService.createRoomMediasoup(mStartupClient.getChannel(), createRoomMediaSoup).get().getData().toJSONString();
                        } catch (ExecutionException | InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        Logger.d(TAG, "createRoomMediaSoup(): " + createRoomMediaSoupInfo);
                    });
                }
            }
        });

        JSONObject joinRoom = new JSONObject();
        joinRoom.put("roomId", serverRoomId);
        joinRoom.put("userId", rtcEngineConfig.mUserId);
        if (getRtcEngineConfig().mChannelProfile == 1) {
            joinRoom.put("userNumber", 2);
            joinRoom.put("scale", 1.2);
            joinRoom.put("fromBottomRatio", 0.2);
            joinRoom.put("scaleFromLeft", 0.4);
            joinRoom.put("scaleFromWidth", 1);
            joinRoom.put("rotationAngle", 0);
            joinRoom.put("openMedia", "video");
        }
        String joinRoomInfo = mService.joinRoom(mStartupClient.getChannel(), joinRoom).get().toString();
        Logger.d(TAG, "joinRoom(): " + joinRoomInfo);

        JSONObject device = new JSONObject();
        device.put("flag", "android");
        device.put("name", "Android " + Build.DEVICE);
        device.put("version", Build.VERSION.CODENAME);

        String rtpCapabilities = mDevice.getRtpCapabilities();
        String sctpCapabilities = mDevice.getSctpCapabilities();

        JSONObject reqJoinRoomMediaSoup = new JSONObject();
        reqJoinRoomMediaSoup.put("displayName", rtcEngineConfig.mDisplayName);
        reqJoinRoomMediaSoup.put("device", device);
        reqJoinRoomMediaSoup.put("roomId", serverRoomId);
        reqJoinRoomMediaSoup.put("userId", rtcEngineConfig.mUserId);
        reqJoinRoomMediaSoup.put("rtpCapabilities", JSONObject.parseObject(rtpCapabilities));
        reqJoinRoomMediaSoup.put("sctpCapabilities", JSONObject.parseObject(sctpCapabilities));

        mService.joinRoomMediaSoup(mStartupClient.getChannel(), reqJoinRoomMediaSoup).addListener((GenericFutureListener<Future<NettyResponse<JSONObject>>>) future -> {
            if (future.isSuccess()) {
                Logger.d(TAG, "joinRoomMediaSoup(): " + JSONObject.toJSONString(future.get()));

                JSONArray peers = future.get().getData().getJSONArray("users");
                for (int i = 0; i < peers.size(); i++) {
                    JSONObject peer = peers.getJSONObject(i);
                    mStore.addPeer(peer.getString("id"), peer);
                }
            }
        });
    }

    public void nativeEnableCamera() {
        mMainHandler.post(() -> mWorkHandler.post(this::enableCameraImpl));
    }

    public void nativeDisableCamera() {
        mWorkHandler.post(this::disableCameraImpl);
    }

    @WorkerThread
    private void disableCameraImpl() {
        if (mVideoProducer == null) {
            return;
        }
        mVideoProducer.close();
        mStore.removeProducer(mVideoProducer.getId());

        try {
            JSONObject req = new JSONObject();
            req.put("producerId", mVideoProducer.getId());
            mService.closeUserProducer(mStartupClient.getChannel(), req);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mVideoProducer = null;
    }

    @WorkerThread
    private void enableCameraImpl() {
        mStore.setCamInProgress(true);
        try {
            if (mVideoProducer != null) {
                return;
            }
            if (!mDevice.isLoaded()) {
                Logger.w(TAG, "enableCam() | not loaded");
                return;
            }
            if (!mDevice.canProduce("video")) {
                Logger.w(TAG, "enableCam() | cannot produce video");
                return;
            }
            if (mSendTransport == null) {
                Logger.w(TAG, "enableCam() | mSendTransport doesn't ready");
                return;
            }

            if (mLocalVideoTrack == null) {

                mLocalVideoTrack = createVideoTrack();
                mLocalVideoTrack.setEnabled(true);
            }
            mVideoProducer =
                    mSendTransport.produce(
                            producer -> {
                                Logger.e(TAG, "onTransportClose(), camProducer");
                                if (mVideoProducer != null) {
                                    mVideoProducer = null;
                                }
                            },
                            mLocalVideoTrack,
                            null,
                            null,
                            null);
            mStore.addProducer(mVideoProducer);
        } catch (MediasoupException e) {
            e.printStackTrace();
            if (mLocalVideoTrack != null) {
                mLocalVideoTrack.setEnabled(false);
            }
        }
        mStore.setCamInProgress(false);

        if (getRtcEngineConfig().mChannelProfile == 1) {
            nativeStartFusion();
        }
    }

    public void nativeSetupCameraCapturerConfiguration(CameraCapturerConfiguration config) {
        this.mCameraConfig = config;
    }

    public void nativeEnableAudio() {
        mWorkHandler.post(this::enableAudioImpl);
    }

    public void nativeDisableAudio() {
        mWorkHandler.post(this::disableAudioImpl);
    }

    @WorkerThread
    private void enableAudioImpl() {
        try {
            if (mAudioProducer != null) {
                return;
            }

            if (!mDevice.isLoaded()) {
                return;
            }

            if (!mDevice.canProduce("audio")) {
                return;
            }

            if (mSendTransport == null) {
                return;
            }

            if (mLocalAudioTrack == null) {
                mLocalAudioTrack = mPeerConnectionUtils.createAudioTrack(rtcEngineConfig.mContext, "Mic");
                mLocalAudioTrack.setEnabled(true);
            }

            mAudioProducer = mSendTransport.produce(producer -> {
                        if (mAudioProducer != null) {
                            mStore.removeProducer(mAudioProducer.getId());
                            mAudioProducer = null;
                        }
                    },
                    mLocalAudioTrack, null, null, null);
            mStore.addProducer(mAudioProducer);

        } catch (Exception e) {
            e.printStackTrace();
            mLocalAudioTrack.setEnabled(false);
        }
    }

    @WorkerThread
    private void disableAudioImpl() {
        if (mAudioProducer == null) {
            return;
        }
        mAudioProducer.close();
        mStore.removeProducer(mAudioProducer.getId());
        try {
            JSONObject req = new JSONObject();
            req.put("producerId", mAudioProducer.getId());
            mService.closeUserProducer(mStartupClient.getChannel(), req);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mAudioProducer = null;
    }

    public void nativeSendChatMessage(String text) {
        mWorkHandler.post(() -> {
            if (mChatDataProducer == null) {
                return;
            }

            try {
                mChatDataProducer.send(new DataChannel.Buffer(ByteBuffer.wrap(text.getBytes("UTF-8")), false));
            } catch (Exception e) {
                Logger.e(TAG, "chat DataProducer.send() failed:", e);
            }
        });
    }

    public void nativeSendBotMessage(String txt) {
        mWorkHandler.post(
                () -> {
                    if (mBotDataProducer == null) {
                        return;
                    }

                    try {
                        mBotDataProducer.send(new DataChannel.Buffer(ByteBuffer.wrap(txt.getBytes("UTF-8")), false));
                    } catch (Exception e) {
                        Logger.e(TAG, "bot DataProducer.send() failed:", e);
                    }
                });
    }

    public boolean nativeHasVideoAvailable() {
        return mVideoProducer != null;
    }

    public boolean nativeHasAudioAvailable() {
        return mAudioProducer != null;
    }

    @WorkerThread
    private void createSendTransport() throws MediasoupException, ExecutionException, InterruptedException {
        String sctpCapabilities = mDevice.getSctpCapabilities();
        JSONObject req = new JSONObject();
        req.put("forceTcp", false);
        req.put("producing", true);
        req.put("consuming", false);
        req.put("sctpCapabilities", sctpCapabilities);
        JSONObject resp = mService.createTransport(mStartupClient.getChannel(), req).get().getData();
        String id = resp.getString("id");
        String iceParameters = resp.getString("iceParameters");
        String iceCandidates = resp.getString("iceCandidates");
        String dtlsParameters = resp.getString("dtlsParameters");
        String sctpParameters = resp.getString("sctpParameters");

        mSendTransport = mDevice.createSendTransport(sendTransportListener, id, iceParameters, iceCandidates, dtlsParameters, sctpParameters);
        if (mSendTransport != null) {
            enableChatDataProducer();
            enableBotDataProducer();
        }
    }

    @WorkerThread
    private void createRecvTransport() throws ExecutionException, InterruptedException, MediasoupException {
        JSONObject req = new JSONObject();
        req.put("forceTcp", false);
        req.put("producing", false);
        req.put("consuming", true);
        req.put("type", "request");
        req.put("userId", rtcEngineConfig.mUserId);
        req.put("roomId", serverRoomId);
        JSONObject resp = mService.createTransport(mStartupClient.getChannel(), req).get().getData();
        String id = resp.getString("id");
        String iceParameters = resp.getString("iceParameters");
        String iceCandidates = resp.getString("iceCandidates");
        String dtlsParameters = resp.getString("dtlsParameters");
        String sctpParameters = resp.getString("sctpParameters");

        mRecvTransport = mDevice.createRecvTransport(recvTransportListener, id, iceParameters, iceCandidates, dtlsParameters, sctpParameters);
    }

    private void enableChatDataProducer() {
        mWorkHandler.post(
                () -> {
                    if (mChatDataProducer != null) {
                        return;
                    }
                    try {
                        DataProducer.Listener listener =
                                new DataProducer.Listener() {
                                    @Override
                                    public void onOpen(DataProducer dataProducer) {
                                        Logger.d(TAG, "chat DataProducer \"open\" event");
                                    }

                                    @Override
                                    public void onClose(DataProducer dataProducer) {
                                        Logger.e(TAG, "chat DataProducer \"close\" event");
                                        mChatDataProducer = null;
                                    }

                                    @Override
                                    public void onBufferedAmountChange(
                                            DataProducer dataProducer, long sentDataSize) {
                                    }

                                    @Override
                                    public void onTransportClose(DataProducer dataProducer) {
                                        mChatDataProducer = null;
                                    }
                                };
                        mChatDataProducer =
                                mSendTransport.produceData(
                                        listener, "chat", "low", false, 1, 0, "{\"info\":\"my-chat-DataProducer\"}");
                        mStore.addDataProducer(mChatDataProducer);
                    } catch (Exception e) {
                        Logger.e(TAG, "enableChatDataProducer() | failed:", e);
                    }
                });
    }

    public void enableBotDataProducer() {
        Logger.d(TAG, "enableBotDataProducer()");
        mWorkHandler.post(
                () -> {
                    if (mBotDataProducer != null) {
                        return;
                    }
                    try {
                        DataProducer.Listener listener =
                                new DataProducer.Listener() {
                                    @Override
                                    public void onOpen(DataProducer dataProducer) {
                                        Logger.d(TAG, "bot DataProducer \"open\" event");
                                    }

                                    @Override
                                    public void onClose(DataProducer dataProducer) {
                                        Logger.e(TAG, "bot DataProducer \"close\" event");
                                        mBotDataProducer = null;
                                        mStore.addNotify("error", "Bot DataProducer closed");
                                    }

                                    @Override
                                    public void onBufferedAmountChange(
                                            DataProducer dataProducer, long sentDataSize) {}

                                    @Override
                                    public void onTransportClose(DataProducer dataProducer) {
                                        mBotDataProducer = null;
                                    }
                                };
                        mBotDataProducer =
                                mSendTransport.produceData(
                                        listener,
                                        "bot",
                                        "medium",
                                        false,
                                        -1,
                                        2000,
                                        "{\"info\":\"my-bot-DataProducer\"}");
                        mStore.addDataProducer(mBotDataProducer);
                    } catch (Exception e) {
                        Logger.e(TAG, "enableBotDataProducer() | failed:", e);
                    }
                });
    }

    private void registerReceiver() {
        Processor<JSONObject> roomJoined = new Processor<JSONObject>() {
            @Override
            public String getMsgCode() {
                return "ROOM_JOINED";
            }

            @Override
            public Object process(JSONObject param) {
                mIRtcEventHandler.onNotification(new Notification(getMsgCode(), param));
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
                mIRtcEventHandler.onNotification(new Notification(getMsgCode(), param));
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
                mIRtcEventHandler.onNotification(new Notification(getMsgCode(), param));
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
                mIRtcEventHandler.onNotification(new Notification(getMsgCode(), param));
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
                mIRtcEventHandler.onNotification(new Notification(getMsgCode(), param));
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
                mIRtcEventHandler.onNotification(new Notification(getMsgCode(), param));

                JSONObject data = new JSONObject();
                data.put("roomId", serverRoomId);
                data.put("userId", rtcEngineConfig.mUserId);
                data.put("messageType", "response");
                return NettyResponse.success(data);
            }
        };

        NettyProcessorManager.register(createUserConsumer.getMsgCode(), createUserConsumer);

        Processor<JSONObject> textDataConsumer = new Processor<JSONObject>() {
            @Override
            public String getMsgCode() {
                return "CREATE_USER_TEXT_CONSUMER";
            }

            @Override
            public Object process(JSONObject param) {
                mIRtcEventHandler.onNotification(new Notification(getMsgCode(), param));

                JSONObject data = new JSONObject();
                data.put("roomId", serverRoomId);
                data.put("userId", rtcEngineConfig.mUserId);
                data.put("messageType", "response");
                return NettyResponse.success(data);
            }
        };

        NettyProcessorManager.register(textDataConsumer.getMsgCode(), textDataConsumer);

        Processor<JSONObject> closeUserConsumer = new Processor<JSONObject>() {
            @Override
            public String getMsgCode() {
                return "USER_CONSUMER_CLOSED";
            }

            @Override
            public Object process(JSONObject param) {
                mIRtcEventHandler.onNotification(new Notification(getMsgCode(), param));
                return null;
            }
        };

        NettyProcessorManager.register(closeUserConsumer.getMsgCode(), closeUserConsumer);

        Processor<JSONObject> consumerLayersChange = new Processor<JSONObject>() {
            @Override
            public String getMsgCode() {
                return "CONSUMER_LAYERS_CHANGED";
            }

            @Override
            public Object process(JSONObject param) {
                mIRtcEventHandler.onNotification(new Notification(getMsgCode(), param));
                return null;
            }
        };

        NettyProcessorManager.register(consumerLayersChange.getMsgCode(), consumerLayersChange);

        Processor<JSONObject> scoreConsumer = new Processor<JSONObject>() {
            @Override
            public String getMsgCode() {
                return "CONSUMER_SCORE";
            }

            @Override
            public Object process(JSONObject param) {
                mIRtcEventHandler.onNotification(new Notification(getMsgCode(), param));
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
                mIRtcEventHandler.onNotification(new Notification(getMsgCode(), param));
                return null;
            }
        };

        NettyProcessorManager.register(scoreProducer.getMsgCode(), scoreProducer);
    }

    private final SendTransport.Listener sendTransportListener = new SendTransport.Listener() {

        private final String listenerTAG = TAG + "_SendTrans";

        @Override
        public String onProduce(Transport transport, String kind, String rtpParameters, String appData) {
            if (mClosed) return "";
            Logger.d(listenerTAG, "onProduce()");
            JSONObject req = new JSONObject();
            req.put("transportId", transport.getId());
            req.put("kind", kind);
            req.put("rtpParameters", JSONObject.parseObject(rtpParameters));
            req.put("appData", appData);
            req.put("roomId", serverRoomId);
            req.put("userId", rtcEngineConfig.mUserId);
            req.put("messageType", "request");

            String producerId = fetchProduceId(req);
            Logger.d(listenerTAG, "producerId: " + producerId);
            return producerId;
        }

        @Override
        public String onProduceData(Transport transport, String sctpStreamParameters, String label, String protocol, String appData) {
            if (mClosed) return "";
            Logger.d(listenerTAG, "onProduceData()");
            JSONObject req = new JSONObject();
            req.put("transportId", transport.getId());
            req.put("sctpStreamParameters", JSONObject.parseObject(sctpStreamParameters));
            req.put("label", label);
            req.put("protocol", protocol);
            req.put("appData", JSONObject.parseObject(appData));
            req.put("roomId", serverRoomId);
            req.put("userId", rtcEngineConfig.mUserId);
            req.put("type", "request");
            String produceDataId = fetchProduceDataId(req);
            Logger.d(listenerTAG, "producerDataId: " + produceDataId);
            return produceDataId;
        }

        @Override
        public void onConnect(Transport transport, String dtlsParameters) {
            if (mClosed) return;
            JSONObject req = new JSONObject();
            req.put("transportId", transport.getId());
            req.put("dtlsParameters", JSONObject.parseObject(dtlsParameters));
            mService.connectTransport(mStartupClient.getChannel(), req);
        }

        @Override
        public void onConnectionStateChange(Transport transport, String connectionState) {
            Logger.d(listenerTAG, "onConnectionStateChange(): " + connectionState);
        }
    };

    private final RecvTransport.Listener recvTransportListener = new RecvTransport.Listener() {
        private final String listenerTAG = TAG + "_RecvTrans";

        @Override
        public void onConnect(Transport transport, String dtlsParameters) {
            if (mClosed) return;
            Logger.d(listenerTAG, "onConnect()");
            JSONObject req = new JSONObject();
            req.put("transportId", transport.getId());
            req.put("dtlsParameters", JSON.parseObject(dtlsParameters));

            mService.connectTransport(mStartupClient.getChannel(), req);
        }

        @Override
        public void onConnectionStateChange(Transport transport, String connectionState) {
            Logger.d(listenerTAG, "onConnectionStateChange: " + connectionState);
        }
    };

    private final IRtcEventHandler mIRtcEventHandler = new IRtcEventHandler() {
        @Override
        public void onOpen() {
            Logger.d(TAG, "onNotification(): " + "onOpen()");
            mWorkHandler.post(() -> {
                try {
                    joinImpl();
                } catch (MediasoupException | ExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        @Override
        public void onFail() {
            Logger.d(TAG, "onNotification(): " + "onFail()");
        }

        @Override
        public void onNotification(Notification notification) {
            Logger.d(TAG, "onNotification(): " + JSONObject.toJSONString(notification));
            handleNotification(notification);
        }

        @Override
        public void onDisconnected() {
            Logger.d(TAG, "onNotification(): " + "onDisconnected()");
        }

        @Override
        public void onClose() {
            Logger.d(TAG, "onNotification(): " + "onClose()");
        }
    };

    private void handleNotification(Notification notification) {
        JSONObject jsonData = notification.data;
        switch (notification.method) {
            case "ROOM_JOINED":
                getRtcEngineConfig().mEventHandler.onJoinChannelSuccess(rtcEngineConfig.mUserId);
                break;
            case "USER_JOINED":
                String id = jsonData.getString("id");
                getRtcEngineConfig().mEventHandler.onUserJoined(id);
                mStore.addPeer(id, jsonData);
                break;
            case "USER_LEFT":
                getRtcEngineConfig().mEventHandler.onUserOffline(jsonData.getString("userId"));
                String uid = jsonData.getString("id");
                mStore.removePeer(uid);
                break;
            case "USERS_ONLINE":
                getRtcEngineConfig().mEventHandler.onUserOnline(jsonData.getJSONArray("users"));
                break;
            case "ROOM_CLOSED":
                getRtcEngineConfig().mEventHandler.onCloseChannel();
                break;
            case "USER_CONSUMER_CLOSED":
                String consumerId = jsonData.getString("consumerId");
                String kind = jsonData.getString("kind");
                ConsumerHolder holder = mConsumers.remove(consumerId);
                if (holder == null) return;
                holder.mConsumer.close();
                mConsumers.remove(consumerId);
                mStore.removeConsumer(holder.peerId, holder.mConsumer.getId());
                if (kind.equals("video")) {
                    getRtcEngineConfig().mEventHandler.onRemoteVideoStateChanged(holder.peerId, holder.mConsumer.getId(), false);
                }
                break;
            case "CREATE_USER_CONSUMER":
                onNewConsumer(jsonData);
                break;
            case "CREATE_USER_TEXT_CONSUMER":
                onNewDataConsumer(jsonData);
                break;
        }

    }

    private void onNewConsumer(JSONObject jsonData) {
        try {
            String type = jsonData.getString("type");
            String userId = jsonData.getString("userId");
            boolean producerPaused = jsonData.getBoolean("producerPaused");
            String id = jsonData.getString("id");
            String producerId = jsonData.getString("producerId");
            String kind = jsonData.getString("kind");
            String rtpParameters = jsonData.getString("rtpParameters");
            String appData = jsonData.getString("appData");

            Consumer consumer =
                    mRecvTransport.consume(
                            consume -> {
                                Logger.w(TAG, "onTransportClose for consume");
                                mConsumers.remove(consume.getId());
                            },
                            id,
                            producerId,
                            kind,
                            rtpParameters,
                            appData);

            mConsumers.put(consumer.getId(), new ConsumerHolder(userId, consumer));
            mStore.addConsumer(userId, type, consumer, producerPaused);

            if (consumer.getTrack() instanceof VideoTrack) {
                rtcEngineConfig.mEventHandler.onRemoteVideoStateChanged(userId, id, true);
            }

        } catch (MediasoupException e) {
            e.printStackTrace();
        }
    }

    private void onNewDataConsumer(JSONObject jsonData) {
        try {
            String peerId = jsonData.getString("peerId");
            String dataProducerId = jsonData.getString("dataProducerId");
            String id = jsonData.getString("id");
            JSONObject sctpStreamParameters = jsonData.getJSONObject("sctpStreamParameters");
            long streamId = sctpStreamParameters.getLong("streamId");
            String label = jsonData.getString("label");
            String protocol = jsonData.getString("protocol");
            String appData = jsonData.getString("appData");

            DataConsumer.Listener listener = new DataConsumer.Listener() {
                @Override
                public void OnConnecting(DataConsumer dataConsumer) {

                }

                @Override
                public void OnOpen(DataConsumer dataConsumer) {
                    Logger.d(TAG, "DataConsumer \"open\" event");
                }

                @Override
                public void OnClosing(DataConsumer dataConsumer) {

                }

                @Override
                public void OnClose(DataConsumer dataConsumer) {
                    mDataConsumers.remove(dataConsumer.getId());
                }

                @Override
                public void OnMessage(DataConsumer dataConsumer, DataChannel.Buffer buffer) {
                    try {
                        byte[] data = new byte[buffer.data.remaining()];
                        buffer.data.get(data);
                        String message = new String(data, StandardCharsets.UTF_8);
                        if ("chat".equals(dataConsumer.getLabel())) {

                            rtcEngineConfig.mEventHandler.onUserMessage(peerId, message);
                            Logger.d(TAG, "DataConsumer \"message\"" + message);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void OnTransportClose(DataConsumer dataConsumer) {

                }
            };

            DataConsumer dataConsumer =
                    mRecvTransport.consumeData(
                            listener, id, dataProducerId, streamId, label, protocol, appData);

            mDataConsumers.put(dataConsumer.getId(), new DataConsumerHolder(peerId, dataConsumer));
            mStore.addDataConsumer(peerId, dataConsumer);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void nativeSetupLocalVideo(SurfaceViewRenderer viewRenderer) {
        mMainHandler.post(() -> {
            viewRenderer.init(PeerConnectionUtils.getEglContext(), null);
            mWorkHandler.post(() -> {
                if (mLocalVideoTrack == null) {
                    mLocalVideoTrack = createVideoTrack();
                    mLocalVideoTrack.setEnabled(true);
                    mLocalVideoTrack.addSink(viewRenderer);
                } else mLocalVideoTrack.addSink(viewRenderer);
            });
        });
    }

    private VideoTrack createVideoTrack() {
        if (mCameraConfig.cameraDirection.getValue() == 1) {
            PeerConnectionUtils.setPreferCameraFace("front");
        }
       return mPeerConnectionUtils.createVideoTrack(rtcEngineConfig.mContext, "Cam",
                mCameraConfig.captureFormat.width, mCameraConfig.captureFormat.height, mCameraConfig.captureFormat.fps);
    }

    public void nativeSetupRemoteVideo(SurfaceViewRenderer viewRenderer, String uid) {
        mStore.getPeers().getValue().getPeer(uid).getConsumers().forEach(s -> {

            ConsumerHolder consumerHolder = mConsumers.get(s);
            if (consumerHolder == null) return;
            if (consumerHolder.peerId.equals(uid)) {
                Consumer targetConsumer = consumerHolder.mConsumer;
                if (targetConsumer.getKind().contains("video")) {
                    viewRenderer.init(PeerConnectionUtils.getEglContext(), null);
                    ((VideoTrack) targetConsumer.getTrack()).addSink(viewRenderer);
                }
            }

        });
    }

    private String fetchProduceId(JSONObject json) {
        Logger.d(TAG, "fetchProduceId()");
        try {
            JSONObject resp = mService.createUserProduce(mStartupClient.getChannel(), json).get().getData();
            return resp.getString("id");
        } catch (JSONException e) {
            e.printStackTrace();
            return "";
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private String fetchProduceDataId(JSONObject json) {
        Logger.d(TAG, "fetchProduceDataId()");

        try {
            JSONObject resp = mService.createUserTextProduce(mStartupClient.getChannel(), json).get().getData();
            return resp.getString("id");
        } catch (JSONException e) {
            e.printStackTrace();
            return "";
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void nativeStartICE(String transportId) {
        if (!NetworkUtils.isNetworkAvailable(getRtcEngineConfig().mContext)) {
            getRtcEngineConfig().mEventHandler.onError(ERR_CONNECTION_LOST);
            return;
        }

        JSONObject reqStartICE = new JSONObject();
        reqStartICE.put("transportId", transportId);
        reqStartICE.put("roomId", serverRoomId);
        reqStartICE.put("userId", rtcEngineConfig.mUserId);
        mService.restartICE(mStartupClient.getChannel(), reqStartICE);
    }

    public void nativeChangeDisplayUserName(String targetName) {
        if (!NetworkUtils.isNetworkAvailable(getRtcEngineConfig().mContext)) {
            getRtcEngineConfig().mEventHandler.onError(ERR_CONNECTION_LOST);
            return;
        }

        JSONObject reqChangeDisplay = new JSONObject();
        reqChangeDisplay.put("displayName", targetName);
        reqChangeDisplay.put("roomId", serverRoomId);
        reqChangeDisplay.put("userId", rtcEngineConfig.mUserId);
        mService.changeDisplayUserName(mStartupClient.getChannel(), reqChangeDisplay);
    }

    public void nativeStartFusion() {
        if (!NetworkUtils.isNetworkAvailable(getRtcEngineConfig().mContext)) {
            getRtcEngineConfig().mEventHandler.onError(ERR_CONNECTION_LOST);
            return;
        }

        JSONObject reqStartFusion = new JSONObject();
        reqStartFusion.put("roomId", serverRoomId);
        reqStartFusion.put("userId", rtcEngineConfig.mUserId);
        mService.startFusion(mStartupClient.getChannel(), reqStartFusion);
    }

    public void nativeStopFusion() {
        if (!NetworkUtils.isNetworkAvailable(getRtcEngineConfig().mContext)) {
            getRtcEngineConfig().mEventHandler.onError(ERR_CONNECTION_LOST);
            return;
        }

        JSONObject reqStopFusion = new JSONObject();
        reqStopFusion.put("roomId", serverRoomId);
        reqStopFusion.put("userId", rtcEngineConfig.mUserId);
        mService.stopFusion(mStartupClient.getChannel(), reqStopFusion);
    }

    public void nativeUpdateFusionSetting(int userCount, float scale, float fromBottomRatio, float scaleFromLeft, float scaleFromWidth, int rotationAngle) {
        if (!NetworkUtils.isNetworkAvailable(getRtcEngineConfig().mContext)) {
            getRtcEngineConfig().mEventHandler.onError(ERR_CONNECTION_LOST);
            return;
        }

        JSONObject reqChangeFusionConfig = new JSONObject();
        reqChangeFusionConfig.put("userNumber", userCount);
        reqChangeFusionConfig.put("scale", scale);
        reqChangeFusionConfig.put("fromBottomRatio", fromBottomRatio);
        reqChangeFusionConfig.put("scaleFromLeft", scaleFromLeft);
        reqChangeFusionConfig.put("scaleFromWidth", scaleFromWidth);
        reqChangeFusionConfig.put("rotationAngle", rotationAngle);
        reqChangeFusionConfig.put("roomId",serverRoomId);
        reqChangeFusionConfig.put("userId", rtcEngineConfig.mUserId);
        mService.updateFusionSetting(mStartupClient.getChannel(), reqChangeFusionConfig);
    }

    public void nativeUpdateFusionBackground(String imageUrl) {
        if (!NetworkUtils.isNetworkAvailable(getRtcEngineConfig().mContext)) {
            getRtcEngineConfig().mEventHandler.onError(ERR_CONNECTION_LOST);
            return;
        }

        JSONObject reqBackground = new JSONObject();
        reqBackground.put("backgroundUrl", imageUrl);
        reqBackground.put("roomId", serverRoomId);
        reqBackground.put("userId", getRtcEngineConfig().mUserId);
        mService.updateFusionBackground(mStartupClient.getChannel(), reqBackground).addListener((GenericFutureListener<Future<Response<JSONObject>>>) future -> {
            if (future.isSuccess()) {
                rtcEngineConfig.mBackgroundUrl = imageUrl;
            }
        });
    }

    public String nativeGetRoomLink() {
        return "https://" + ClientFactory.getHost() + "/media-rtc-demos/?roomId=" + getRtcEngineConfig().mRoomId + "&videoType=" + getVideoType();
    }

    public void nativeDestroy() {
        if (mClosed) return;
        mClosed = true;

        mWorkHandler.post(() -> {

            if (mStartupClient != null) {
                mStartupClient.close();
                mStartupClient = null;
                processorsClear();
            }

            disposeTransportDevice();

            if (mLocalAudioTrack != null) {
                mLocalAudioTrack.setEnabled(false);
                mLocalAudioTrack.dispose();
            }

            if (mLocalVideoTrack != null) {
                mLocalVideoTrack.setEnabled(false);
                mLocalVideoTrack.dispose();
            }

            mConsumers.forEach((s, consumerHolder) -> {
                Consumer consumer = consumerHolder.mConsumer;
                if (consumer.getTrack() instanceof VideoTrack) {
                    VideoTrack videoTrack = (VideoTrack) consumer.getTrack();
                    videoTrack.setEnabled(false);
                    videoTrack.dispose();
                }
                mConsumers.remove(s);
            });

            if (mPeerConnectionUtils != null) {
                mPeerConnectionUtils.dispose();
            }

            mWorkHandler.getLooper().quit();

        });
    }

    @WorkerThread
    private void disposeTransportDevice() {
        Logger.d(TAG, "disposeTransportDevice()");
        if (mSendTransport != null) {
            mSendTransport.close();
            mSendTransport.dispose();
            mSendTransport = null;
        }

        if (mRecvTransport != null) {
            mRecvTransport.close();
            mRecvTransport.dispose();
            mRecvTransport = null;
        }

        // dispose device.
        mDevice.dispose();
    }

    private void processorsClear() {
        try {
            Field privateMethod = NettyProcessorManager.class.getDeclaredField("processorTable");
            privateMethod.setAccessible(true);
            HashMap<String, Pair<Processor<?>, ExecutorService>> value = (HashMap<String, Pair<Processor<?>, ExecutorService>>) privateMethod.get(new NettyProcessorManager());
            if (value != null)
                value.clear();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}

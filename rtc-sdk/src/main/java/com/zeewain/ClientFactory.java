package com.zeewain;

import com.zeewain.cbb.netty.NettyClientAutoConfig;
import com.zeewain.cbb.netty.NettyCoreConfig;
import com.zeewain.cbb.netty.client.ClientHandlerProvider;
import com.zeewain.cbb.netty.client.NettyClient;
import com.zeewain.cbb.netty.client.StartupClient;
import com.zeewain.cbb.netty.client.builder.NettyClientBuilder;
import com.zeewain.cbb.netty.client.protocol.ClientSocketProtocolHandler;
import com.zeewain.cbb.netty.client.protocol.ClientWebsocketProtocolHandler;
import com.zeewain.cbb.netty.core.NettyProperties;
import com.zeewain.cbb.netty.core.heartbeat.HeartBeatClient;
import com.zeewain.cbb.netty.core.heartbeat.HeartBeatHandler;
import com.zeewain.cbb.netty.mvc.NettyProcessorPostProcessor;
import com.zeewain.cbb.netty.protocol.IdGenerator;
import com.zeewain.cbb.netty.protocol.codec.json.JsonCodec;
import com.zeewain.cbb.netty.protocol.compress.GzipCompressor;
import com.zeewain.cbb.netty.protocol.compress.UnCompressor;
import com.zeewain.cbb.netty.rpc.NettyClientFactory;
import com.zeewain.cbb.netty.rpc.NettyRpcResponseHandler;

import org.mediasoup.droid.BuildConfig;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientFactory {

    /**
     * 服务端的 host
     */
    private final static String host_debug = "dev.local.zeewain.com";

    private final static String host_release = "test.local.zeewain.com";

    /**
     * 服务端的 port
     */
    private final static int port = 443;

    /**
     * 连接websocket的默认路径
     */
    private final static String path = "/api/wsproxy/websocket?upstream=video-fusion-service-netty-nacos&roomId=";

    /**
     * 是否开启 debug 日志
     */
    private final static boolean debug = true;

    private static StartupClient startupClient = null;
    private static NettyClientFactory nettyClientFactory = null;
    private static NettyProcessorPostProcessor nettyProcessorPostProcessor = null;
    private final static Map<Class<?>, Object> clientMap = new ConcurrentHashMap<>();

    public static StartupClient get() {
        if (startupClient == null) {
            synchronized (ClientFactory.class) {
                if (startupClient == null) {
                    startupClient = create();
                }
            }
        }
        return startupClient;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getSender(Class<T> clazz){
        T client = (T) clientMap.get(clazz);
        if(client == null) {
            client = nettyClientFactory.getClient(clazz);
            clientMap.put(clazz, client);
        }
        return client;
    }

    public static void registerReceiver(Class<?> clazz){
        nettyProcessorPostProcessor._innerRegister(clazz);
    }

    public static void registerReceiver(Object processor){
        nettyProcessorPostProcessor._innerRegister(processor);
    }

    public static String getHost() {
        return BuildConfig.DEBUG ? host_debug : host_release;
    }

    private static StartupClient create() {
        NettyProperties nettyProperties = new NettyProperties();
        nettyProperties.getClient().setHost(getHost());
        nettyProperties.getClient().setPort(port);
        nettyProperties.getClient().setPath(path);
        nettyProperties.getClient().setDebug(debug);
        nettyProperties.getClient().setEnable(true);
        nettyProperties.getClient().setSchema("wss");
        nettyProperties.getClient().setWebSocketEnable(true);


        NettyCoreConfig nettyCoreConfig = new NettyCoreConfig();
        NettyClientAutoConfig nettyClientAutoConfig = new NettyClientAutoConfig();

        // -> -> -> -> core 的配置
        IdGenerator idGenerator = nettyCoreConfig.idGenerator();
        JsonCodec jsonCodec = nettyCoreConfig.jsonCodec();
        GzipCompressor gzipCompressor = nettyCoreConfig.gzipCompressor();
        UnCompressor unCompressor = nettyCoreConfig.unCompressor();

        nettyClientFactory = new NettyClientFactory();
        nettyClientFactory.setNettyProperties(nettyProperties);
        nettyClientFactory.setIdGenerator(idGenerator);

        nettyProcessorPostProcessor = nettyCoreConfig.nettyProcessorPostProcessor();

        HeartBeatClient heartBeatClient = getSender(HeartBeatClient.class);
        HeartBeatHandler heartBeatHandler = nettyCoreConfig
                .heartBeatHandler(heartBeatClient);
        NettyRpcResponseHandler nettyRpcResponseHandler = nettyCoreConfig
                .nettyRpcResponseHandler();

        // -> -> -> -> client 的配置
        ClientHandlerProvider clientHandlerProvider = nettyClientAutoConfig
                .defaultClientHandler(idGenerator);
        ClientSocketProtocolHandler clientSocketProtocolHandler = nettyClientAutoConfig
                .clientSocketProtocolHandler(nettyProperties, clientHandlerProvider, heartBeatHandler, nettyRpcResponseHandler);
        ClientWebsocketProtocolHandler clientWebsocketProtocolHandler = nettyClientAutoConfig
                .clientWebsocketProtocolHandler(nettyProperties, clientHandlerProvider, heartBeatHandler, nettyRpcResponseHandler);

        NettyClient nettyClient = nettyClientAutoConfig
                .nettyClient(nettyProperties, clientSocketProtocolHandler, clientWebsocketProtocolHandler);
        NettyClientBuilder nettyClientBuilder = nettyClientAutoConfig
                .nettyClientBuilder(nettyProperties, nettyClient, Collections.emptyList());

        return nettyClientAutoConfig.startupClient(nettyProperties, nettyClientBuilder);
    }

    public static void dispose() {
        nettyProcessorPostProcessor = null;
    }

}



package com.example;

import com.corundumstudio.socketio.AckCallback;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import com.corundumstudio.socketio.annotation.OnEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 客户端和服务端都是通过事件来交互的
 * 用于监听客户端websocket的事件
 * 同时也可以往客户端发送事件(客户端自己可以监听)
 */
@Component
@Slf4j
public class MessageEventHandler {

    @Autowired
    private SocketIOServer socketIoServer;


    /**
     * 线程安全的map,用于保存和客户端的回话
     * <p>
     * 如果是使用集群部署的情况下则不能这么使用,
     * 因为客户端每次命中的服务不一定是上次命中那个
     * 集群解决方案:使用redis的发布订阅或者消息中间件的发布订阅
     * 这样,每个服务都有listener监听着,然后可以拿到对应的客户端socketclient
     */
    public static ConcurrentMap<String, SocketIOClient> socketIOClientMap = new ConcurrentHashMap<>();

    private boolean isInit = false;

    /**
     * 客户端连接的时候触发
     *
     * @param client
     */
    @OnConnect
    public void onConnect(SocketIOClient client) {
        //
        String UID = client.getHandshakeData().getSingleUrlParam("UID");
        //存储SocketIOClient，用于发送消息
        socketIOClientMap.put(UID, client);
        //通过client.sendEvent可以往客户端回发消息
//        client.sendEvent("message", "{\"doctorId\":\"xingwang\"}");

        log.info("客户端:" + client.getSessionId() + "已连接,UID=" + UID);

        if (!isInit) {
            isInit = true;
            new Thread() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            sleep(5000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
//                        sendBroadcast();
                    }
                }
            }.start();
        }
    }

    /**
     * 客户端关闭连接时触发
     *
     * @param client
     */
    @OnDisconnect
    public void onDisconnect(SocketIOClient client) {
        String uid = client.getHandshakeData().getSingleUrlParam("UID");
        socketIOClientMap.remove(uid);
        log.info("客户端:" + client.getSessionId() + "断开连接");
    }

    /**
     * 监听客户端事件messageevent
     *
     * @param client 　客户端信息
     */
    @OnEvent(value = "ServerReceive")
    public void onEvent(SocketIOClient client, AckRequest request, MessageBean messageBean) {
        log.info("发来消息：" + messageBean.toString());
        request.sendAckData(new MessageBean("我是服务端,我收到了你的消息","服务器"));
        client.sendEvent("sendMessaged", new AckCallback<MessageBean>(MessageBean.class) {
            @Override
            public void onSuccess(MessageBean o) {
                log.info("客户端已应答:"+o);
            }
        }, "服务端向客户端发的消息:" + messageBean);
        //回发消息
//        sendBroadcast();
    }


    /**
     * 监听客户端事件messageevent
     *
     * @param client  　客户端信息
     * @param request 请求信息
     * @param data    　客户端发送数据
     */
//    @OnEvent(value = "messageevent")
//    public void onEvent(SocketIOClient client, AckRequest request, Message data) {
//        log.info("发来消息：" + data);
//        //回发消息
//        client.sendEvent("messageevent", "我是服务器都安发送的信息==" + data.getFormattedMessage());
//        //广播消息
//        sendBroadcast();
//    }


    /**
     * 监听客户端事件messageevent
     *
     * @param client  　客户端信息
     * @param data    　客户端发送数据
     */
//    @OnEvent(value = "messageevent2")
//    public void messageevent2(SocketIOClient client,  JSONObject data) {
//        log.info("发来消息：" + data);
//        //回发消息
//        client.sendEvent("messageevent2", "我是服务器都安发送的信息==" + data.getString("FirstName"));
//    }

    /**
     * 广播消息
     */
    public void sendBroadcast() {
        for (SocketIOClient client : socketIOClientMap.values()) {
            if (client.isChannelOpen()) {
                client.sendEvent("Broadcast", "当前时间", System.currentTimeMillis());
            }
        }
    }
}

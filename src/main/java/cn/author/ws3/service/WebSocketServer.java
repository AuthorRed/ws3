package cn.author.ws3.service;

import cn.author.ws3.entity.MessageBody;
import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@ServerEndpoint("/webSocket/{uid}")
@Component
public class WebSocketServer {
    //静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。
    private static AtomicInteger onlineNum = new AtomicInteger();

    protected static final Logger log = LoggerFactory.getLogger(WebSocketServer.class);
    //concurrent包的线程安全Set，用来存放每个客户端对应的WebSocketServer对象。
    private static ConcurrentHashMap<String, Session> sessionPools = new ConcurrentHashMap<>();

    //发送消息
    public void sendMessage(Session session, MessageBody messageBody) throws IOException {
        try {
            if(session != null){
                synchronized (session) {
                    session.getBasicRemote().sendText(JSON.toJSONString(messageBody));
                }
            }else {
                log.info("用户未登录");
            }
        }catch ( Exception e){
            log.error("发送消息出错:",e);
        }
    }

    //建立连接成功调用
    @OnOpen
    public void onOpen(Session session, @PathParam(value = "uid") String userName){
        try {
            sessionPools.put(userName, session);
            addOnlineCount();
            System.out.println(userName + "加入webSocket！当前人数为" + onlineNum);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //关闭连接时调用
    @OnClose
    public void onClose(@PathParam(value = "uid") String userName){
        sessionPools.remove(userName);
        subOnlineCount();
        System.out.println(userName + "断开webSocket连接！当前人数为" + onlineNum);
    }

    //收到客户端信息
    @OnMessage
    public void onMessage(String message) throws IOException{
        try{
            if(StringUtils.isBlank(message.trim())){return;}
            System.out.println(message);
            MessageBody msg = JSON.parseObject(message, MessageBody.class);
            msg.setDate(new Date());
            if(null==msg||null==msg.getFrom()||null==msg.getTo()||null==msg.getMessage()){
                log.info("消息信息不完整:"+message);
                return;
            }
            sendMessage(sessionPools.get(msg.getTo()),msg);
        }catch (Exception e){
            log.error("发送消息出错:",e);

        }

    }

    //错误时调用
    @OnError
    public void onError(Session session, Throwable throwable){
        System.out.println("发生错误");
        throwable.printStackTrace();
    }

    public static void addOnlineCount(){
        onlineNum.incrementAndGet();
    }

    public static void subOnlineCount() {
        onlineNum.decrementAndGet();
    }

}

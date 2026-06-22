package com.agri.claim.assess.websocket;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class AssessWebSocketHandler extends TextWebSocketHandler {

    private static final Map<String, WebSocketSession> SESSIONS = new ConcurrentHashMap<>();
    private static final Map<String, String> USER_SESSION = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = extractUserId(session);
        String sessionId = session.getId();
        SESSIONS.put(sessionId, session);
        if (userId != null) {
            USER_SESSION.put(userId, sessionId);
        }
        log.info("WebSocket连接建立 | sessionId: {} | userId: {}", sessionId, userId);
        sendMessage(session, Map.of("type", "CONNECTED", "message", "连接成功",
                "sessionId", sessionId, "timestamp", System.currentTimeMillis()));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();
        log.debug("收到WebSocket消息 | sessionId: {} | payload: {}", session.getId(), payload);
        try {
            Map<String, Object> data = JSON.parseObject(payload);
            String type = (String) data.get("type");
            if ("PING".equals(type)) {
                sendMessage(session, Map.of("type", "PONG", "timestamp", System.currentTimeMillis()));
            } else if ("SUBSCRIBE".equals(type)) {
                String userId = (String) data.get("userId");
                if (userId != null) {
                    USER_SESSION.put(userId, session.getId());
                    log.info("用户订阅消息推送 | userId: {} | sessionId: {}", userId, session.getId());
                    sendMessage(session, Map.of("type", "SUBSCRIBED", "userId", userId));
                }
            }
        } catch (Exception e) {
            log.warn("WebSocket消息处理失败", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = session.getId();
        SESSIONS.remove(sessionId);
        USER_SESSION.values().removeIf(sid -> sid.equals(sessionId));
        log.info("WebSocket连接关闭 | sessionId: {} | status: {}", sessionId, status);
    }

    public boolean sendToUser(String userId, Map<String, Object> message) {
        String sessionId = USER_SESSION.get(userId);
        if (sessionId == null) return false;
        WebSocketSession session = SESSIONS.get(sessionId);
        if (session == null || !session.isOpen()) return false;
        return sendMessage(session, message);
    }

    public void broadcast(Map<String, Object> message) {
        SESSIONS.values().forEach(session -> sendMessage(session, message));
    }

    public int getOnlineCount() {
        return SESSIONS.size();
    }

    private boolean sendMessage(WebSocketSession session, Map<String, Object> message) {
        try {
            if (!session.isOpen()) return false;
            session.sendMessage(new TextMessage(JSON.toJSONString(message)));
            return true;
        } catch (IOException e) {
            log.warn("WebSocket发送消息失败 | sessionId: {}", session.getId(), e);
            return false;
        }
    }

    private String extractUserId(WebSocketSession session) {
        try {
            String query = session.getUri() != null ? session.getUri().getQuery() : null;
            if (query == null) return null;
            for (String param : query.split("&")) {
                String[] kv = param.split("=", 2);
                if (kv.length == 2 && "userId".equals(kv[0])) {
                    return kv[1];
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}

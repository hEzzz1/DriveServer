package com.example.demo.realtime.listener;

import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.auth.service.BusinessAccessService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.security.Principal;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RealtimeAlertWebSocketHandler extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final ObjectMapper objectMapper;
    private final BusinessAccessService businessAccessService;

    public RealtimeAlertWebSocketHandler(ObjectMapper objectMapper,
                                         BusinessAccessService businessAccessService) {
        this.objectMapper = objectMapper;
        this.businessAccessService = businessAccessService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }

    @EventListener
    public void onAlertEvent(RealtimeAlertBroadcastEvent event) {
        if (sessions.isEmpty()) {
            return;
        }
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event.payload());
        } catch (IOException error) {
            return;
        }

        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                sessions.remove(session);
                continue;
            }
            AuthenticatedUser user = resolveUser(session.getPrincipal());
            if (user == null || !canReceive(user, event.enterpriseId(), event.fleetId())) {
                continue;
            }
            try {
                session.sendMessage(new TextMessage(payload));
            } catch (IOException error) {
                sessions.remove(session);
            }
        }
    }

    private boolean canReceive(AuthenticatedUser user, Long enterpriseId, Long fleetId) {
        try {
            return businessAccessService.hasPermission(user, "alert.read")
                    && businessAccessService.resolveDataScope(user, enterpriseId, fleetId).canAccessData(enterpriseId, fleetId);
        } catch (RuntimeException error) {
            return false;
        }
    }

    private AuthenticatedUser resolveUser(Principal principal) {
        if (principal instanceof Authentication authentication && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user;
        }
        if (principal instanceof AuthenticatedUser user) {
            return user;
        }
        return null;
    }
}

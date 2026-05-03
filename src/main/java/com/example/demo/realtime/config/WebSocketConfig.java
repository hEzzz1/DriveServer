package com.example.demo.realtime.config;

import com.example.demo.config.CorsProperties;
import com.example.demo.realtime.listener.RealtimeAlertWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final RealtimeAlertWebSocketHandler realtimeAlertWebSocketHandler;
    private final CorsProperties corsProperties;

    public WebSocketConfig(RealtimeAlertWebSocketHandler realtimeAlertWebSocketHandler,
                           CorsProperties corsProperties) {
        this.realtimeAlertWebSocketHandler = realtimeAlertWebSocketHandler;
        this.corsProperties = corsProperties;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(realtimeAlertWebSocketHandler, "/ws/alerts")
                .setAllowedOriginPatterns(corsProperties.getAllowedOriginPatterns().toArray(String[]::new));
    }
}

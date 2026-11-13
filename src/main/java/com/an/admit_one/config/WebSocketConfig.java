package com.an.admit_one.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;


@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    // Tái dùng property có sẵn trong application.properties (OrderService cũng dùng cái này
    // để tạo link thanh toán), thay vì hardcode "http://localhost:5173" trong code.
    @Value("${app.fe-url}")
    private String feUrl;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(feUrl);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Thiếu TaskScheduler thì simple broker KHÔNG gửi nhịp tim (bài 11): kết nối im lặng
        // lúc sự kiện chưa mở bán sẽ bị nginx/trình duyệt âm thầm cắt sau một lúc.
        ThreadPoolTaskScheduler heartbeatScheduler = new ThreadPoolTaskScheduler();
        heartbeatScheduler.setPoolSize(1);
        heartbeatScheduler.setThreadNamePrefix("ws-heartbeat-");
        heartbeatScheduler.initialize();

        config.enableSimpleBroker("/topic")
                .setHeartbeatValue(new long[]{10_000, 10_000})
                .setTaskScheduler(heartbeatScheduler);
    }
}
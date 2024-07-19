package com.ruoyi.webchat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.DefaultManagedTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket 消息代理
 * 2024/07/19
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    //设置心跳的时间间隔
    private static long HEART_BEAT=10000;

    /**
     * 注册stomp站点
     * @param registry
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 设置连接地址，注意使用"/ws"进行连接websocket。
        // setAllowedOrigins("*")是设置所有请求都可以访问，即允许跨域的问题，或者自己设置允许访问的域名。
        // withSockJS()来确保跨浏览器兼容性
        registry.addEndpoint("/ws").setAllowedOrigins("*").withSockJS();
    }

    /**
     * 这是用于配置服务器向浏览器发送的消息。
     * clientOut就表示出出口。还有一个inBoundChannel用于处理浏览器向服务器发送的消息
     * @param registration
     */
    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        // 默认的发送器是采用多线程发送的。
        // 修改默认的发送器的线程个数为1，解决消息乱序的问题。
        registration.taskExecutor().corePoolSize(1).maxPoolSize(1);
    }

    /**
     * 注册拦截"/topic","/queue"的消息
     * @param config
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // enableSimpleBroker
        // 设置了两个简单消息代理的目的地前缀："/topic"用于发布/订阅模式的消息，"/queue"用于点对点的消息传递。
        // setHeartbeatValue
        // 设置心跳间隔。这样，服务器就可以定期发送心跳包，以确保客户端连接处于活动状态。要配合后面setTaskScheduler才可以生效。
        config.enableSimpleBroker("/topic", "/queue").setHeartbeatValue(new long[]{HEART_BEAT, HEART_BEAT}).setTaskScheduler(new DefaultManagedTaskScheduler());
        // websocket请求的前缀地址。即client.send("/app")作为前缀，然后再加上对应的@MessageMapping后面的地址
        config.setApplicationDestinationPrefixes("/app");
    }

}
package com.ruoyi.webchat.controller;

import com.github.binarywang.java.emoji.EmojiConverter;
import com.ruoyi.webchat.service.GroupMsgContentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import com.ruoyi.webchat.domain.GroupMsgContent;
import com.ruoyi.webchat.domain.Message;
import com.ruoyi.common.core.domain.entity.SysUser;
import top.javahai.subtlechat.api.utils.TuLingUtil;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * WebSocket 控制器
 * 2024/07/19
 */
@Controller
public class WebSocketController {

    @Autowired
    SimpMessagingTemplate simpMessagingTemplate;

    /**
     * 单聊的消息的接受与转发
     * @param authentication
     * @param message
     */
    @MessageMapping("/ws/chat")
    //@SendTo("/queue/chat") RabbitMQ，将消息发送到/queue/chat队列中
    public void handleMessage(Authentication authentication, Message message){
        SysUser user = ((SysUser) authentication.getPrincipal());
        message.setFromNickname(user.getNickName());
        message.setFrom(user.getUserName());
        message.setCreateTime(new Date());
        simpMessagingTemplate.convertAndSendToUser(message.getTo(),"/queue/chat", message);
    }

    @Autowired
    GroupMsgContentService groupMsgContentService;
    EmojiConverter emojiConverter = EmojiConverter.getInstance();

    SimpleDateFormat dateFormat=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 群聊的消息接受与转发
     * @param authentication
     * @param groupMsgContent
     */
    @MessageMapping("/ws/groupchat")
    public void handleGroupMessage(Authentication authentication, GroupMsgContent groupMsgContent){
        SysUser currentUser = ((SysUser) authentication.getPrincipal());
        //处理emoji内容,转换成unicode编码
        groupMsgContent.setContent(emojiConverter.toHtml(groupMsgContent.getContent()));
        //保证来源正确性，从Security中获取用户信息
        groupMsgContent.setFromId(currentUser.getUserId().intValue());
        groupMsgContent.setFromName(currentUser.getNickName());
        groupMsgContent.setFromProfile(currentUser.getAvatar());
        groupMsgContent.setCreateTime(new Date());
        //保存该条群聊消息记录到数据库中
        groupMsgContentService.insert(groupMsgContent);
        //转发该条数据
        simpMessagingTemplate.convertAndSend("/topic/greetings", groupMsgContent);
    }

    /**
     * 接受前端发来的消息，获得图灵机器人回复并转发回给发送者
     * @param authentication
     * @param message
     * @throws IOException
     */
    @MessageMapping("/ws/robotchat")
    public void handleRobotChatMessage(Authentication authentication, Message message) throws IOException {
        SysUser user = ((SysUser) authentication.getPrincipal());
        //接收到的消息
        message.setFrom(user.getUserName());
        message.setCreateTime(new Date());
        message.setFromNickname(user.getNickName());
        message.setFromUserProfile(user.getAvatar());
        //发送消息内容给机器人，获得回复
        String result = TuLingUtil.replyMessage(message.getContent());
        //构建返回消息JSON字符串
        Message resultMessage = new Message();
        resultMessage.setFrom("瓦力");
        resultMessage.setCreateTime(new Date());
        resultMessage.setFromNickname("瓦力");
        resultMessage.setContent(result);
        //回送机器人回复的消息给发送者
        simpMessagingTemplate.convertAndSendToUser(message.getFrom(),"/queue/robot", resultMessage);

    }

}

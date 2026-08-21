package com.opsflow.module.automation.rabbit;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 自动化执行 RabbitMQ 配置
 * 架构：工单/手动触发 → 写执行记录(等待) → 投递消息到 exec.queue
 *      → 消费者异步 SSH 执行 → 实时写日志 + WebSocket 推送
 */
@Configuration
public class AutomationRabbitConfig {

    /** 脚本执行任务队列 */
    public static final String EXEC_QUEUE = "opsflow.exec.task";

    @Bean
    public Queue execQueue() {
        return new Queue(EXEC_QUEUE, true);
    }

    /** JSON 消息转换器，保证消息体为可读 JSON */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }
}
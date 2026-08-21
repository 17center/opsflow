package com.opsflow.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Web MVC 配置：JSON 响应统一使用 UTF-8 编码
 * 部分客户端（如 Windows PowerShell）对无 charset 的 application/json 会按本地编码解码导致中文乱码，
 * 此处显式附加 ;charset=UTF-8 以保证跨客户端中文显示正确。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        for (HttpMessageConverter<?> converter : converters) {
            if (converter instanceof MappingJackson2HttpMessageConverter jsonConverter) {
                jsonConverter.setDefaultCharset(StandardCharsets.UTF_8);
            }
        }
    }
}

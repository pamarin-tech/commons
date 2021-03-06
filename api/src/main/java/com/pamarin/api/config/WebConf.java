/*
 * Copyright 2017-2019 Pamarin.com
 */
package com.pamarin.api.config;

import javax.servlet.SessionCookieConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 *
 * @author jitta
 */
@Configuration
@EnableWebMvc
public class WebConf extends WebMvcConfigurerAdapter {

    @Value("${server.hostUrl}")
    private String hostUrl;

    @Bean
    public RestTemplate newRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    public ServletContextInitializer servletContextInitializer() {
        return servletContext -> {
            SessionCookieConfig config = servletContext.getSessionCookieConfig();
            config.setSecure(hostUrl.startsWith("https://"));
            config.setName("session");
            config.setHttpOnly(true);
        };
    }

    @Bean
    public RequestContextListener newRequestContextListener() {
        return new RequestContextListener();
    }

}

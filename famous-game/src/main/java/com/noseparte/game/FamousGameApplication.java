package com.noseparte.game;

import com.noseparte.game.base.listener.ApplicationEventListener;
import org.redisson.spring.starter.RedissonAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author Noseparte
 * @date 2019/7/31 10:42
 * @Description
 */
@ImportAutoConfiguration(RedissonAutoConfiguration.class)
@SpringBootApplication(exclude = {MongoAutoConfiguration.class, DataSourceAutoConfiguration.class})
@ComponentScan({"com.noseparte.game.*", "com.noseparte.common.*"})
public class FamousGameApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(FamousGameApplication.class);
        application.addListeners(new ApplicationEventListener());
        application.run(args);
    }

}

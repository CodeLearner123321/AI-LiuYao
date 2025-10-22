package com.divination.liuyao;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;

@Slf4j
@SpringBootApplication(exclude = {
    HibernateJpaAutoConfiguration.class
})
@EnableAsync
public class AiLiuyaoApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiLiuyaoApplication.class, args);
        log.info("项目启动成功！");
    }
}
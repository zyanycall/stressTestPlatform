package io.renren.modules.test.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 自定义线程池及配置
 * Created by zyanycall@gmail.com on 2018/12/3 17:57.
 */
@Configuration
@EnableAsync
public class ExecutorConfig {

    Logger logger = LoggerFactory.getLogger(getClass());

    @Bean
    public Executor asyncServiceExecutor() {
        logger.info("start asyncServiceExecutor");
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        //配置核心线程数：并发预计不多
        executor.setCorePoolSize(8);
        //配置最大线程数
        executor.setMaxPoolSize(16);
        //配置队列大小
        executor.setQueueCapacity(999);
        //配置线程池中的线程的名称前缀
        executor.setThreadNamePrefix("jmeter-async-service-");

        // rejection-policy：当pool已经达到max size的时候，如何处理新任务
        // CallerRunsPolicy：是如果队列也满了，则使用主线程来执行。不在新线程中执行任务，而是由调用者所在的线程来执行
        // 这里直接使用丢弃的，同时抛出异常。
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        //执行初始化
        executor.initialize();
        return executor;
    }
}

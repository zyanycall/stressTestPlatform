package io.renren.modules.test.config;

import io.renren.modules.test.utils.StressTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.io.File;

/**
 * 增加url的匹配，如果是testReport的url，则会实际使用casePath的实际路径。
 * casePath的实际路径在这里相当于文件服务器的存储部分，
 * 所以在Shiro的配置中，需要屏蔽掉token的校验。
 * Created by zyanycall@gmail.com on 18:25.
 */
@Configuration
public class TestWebMvcConfig extends WebMvcConfigurerAdapter {

    @Autowired
    StressTestUtils stressTestUtils;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/testReport/**")
                .addResourceLocations("file:" + stressTestUtils.getCasePath() + File.separator);
    }
}

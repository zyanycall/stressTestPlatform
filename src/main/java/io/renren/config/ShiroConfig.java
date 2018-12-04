package io.renren.config;

import io.renren.modules.sys.oauth2.OAuth2Filter;
import io.renren.modules.sys.oauth2.OAuth2Realm;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.session.mgt.SessionManager;
import org.apache.shiro.spring.LifecycleBeanPostProcessor;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.Filter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shiro配置
 *
 * @author chenshun
 * @email sunlightcs@gmail.com
 * @date 2017-04-20 18:33
 */
@Configuration
public class ShiroConfig {

    @Bean("sessionManager")
    public SessionManager sessionManager() {
        DefaultWebSessionManager sessionManager = new DefaultWebSessionManager();
        sessionManager.setSessionValidationSchedulerEnabled(true);
        sessionManager.setSessionIdCookieEnabled(true);
        return sessionManager;
    }

    /**
     * 不指定名字的话，自动创建一个方法名第一个字母小写的bean
     * @Bean(name = "securityManager")
     * @return
     */
    @Bean("securityManager")
    public SecurityManager securityManager(OAuth2Realm oAuth2Realm, SessionManager sessionManager) {
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
        securityManager.setRealm(oAuth2Realm);
        securityManager.setSessionManager(sessionManager);

        return securityManager;
    }

    /**
     * Shiro的Web过滤器Factory 命名:shiroFilter
     *
     * @param securityManager
     * @return
     */
    @Bean("shiroFilter")
    public ShiroFilterFactoryBean shirFilter(SecurityManager securityManager) {
        ShiroFilterFactoryBean shiroFilter = new ShiroFilterFactoryBean();
        //Shiro的核心安全接口,这个属性是必须的
        shiroFilter.setSecurityManager(securityManager);

        //要求登录时的链接(可根据项目的URL进行替换),非必须的属性,默认会自动寻找Web工程根目录下的"/login.jsp"页面
//        shiroFilter.setLoginUrl("/login");
        //登录成功后要跳转的连接,逻辑也可以自定义，例如返回上次请求的页面
//        shiroFilter.setSuccessUrl("/index");
        //用户访问未对其授权的资源时,所显示的连接
//        shiroFilter.setUnauthorizedUrl("/403");

        //oauth过滤
        Map<String, Filter> filters = new HashMap<>();
        filters.put("oauth2", new OAuth2Filter());
        shiroFilter.setFilters(filters);

        /* 定义shiro过滤链 Map结构
         * Map中key(xml中是指value值)的
         * 第一个'/'代表的路径是相对于HttpServletRequest.getContextPath()的值来的
         * anon：它对应的过滤器里面是空的,什么都没做,
         * 这里.do和.jsp后面的*表示参数,比方说login.jsp?main这种
         * authc：该过滤器下的页面必须验证后才能访问,
         * 它是Shiro内置的一个拦截器
         * org.apache.shiro.web.filter.authc.FormAuthenticationFilter
        */
        Map<String, String> filterMap = new LinkedHashMap<>();
        // 配置退出过滤器,其中的具体的退出代码Shiro已经替我们实现了
//        filterMap.put("/logout", "logout");

        filterMap.put("/test/stressReports/downloadReport/*?*", "anon");
        filterMap.put("/test/debugReports/downloadReport/*?*", "anon");
        filterMap.put("/test/stressFile/downloadFile/*?*", "anon");
        filterMap.put("/test/stressFile/statInfo/*?*", "anon");
        filterMap.put("/testReport/**", "anon");
        filterMap.put("/webjars/**", "anon");
        filterMap.put("/druid/**", "anon");
        filterMap.put("/app/**", "anon");
        filterMap.put("/sys/login", "anon"); 
        filterMap.put("/**/*.css", "anon");
        filterMap.put("/**/*.js", "anon");
        filterMap.put("/**/*.html", "anon");
        filterMap.put("/fonts/**", "anon");
        filterMap.put("/plugins/**", "anon");
        filterMap.put("/swagger/**", "anon");
        filterMap.put("/v2/api-docs", "anon");
        filterMap.put("/swagger-ui.html", "anon");
        filterMap.put("/swagger-resources/**", "anon");
        filterMap.put("/favicon.ico", "anon");
        filterMap.put("/captcha.jpg", "anon");
        filterMap.put("/", "anon");
        filterMap.put("/**", "oauth2");
        shiroFilter.setFilterChainDefinitionMap(filterMap);

        return shiroFilter;
    }

    /**
     * Shiro生命周期处理器
     * @return
     */
    @Bean("lifecycleBeanPostProcessor")
    public LifecycleBeanPostProcessor lifecycleBeanPostProcessor() {
        return new LifecycleBeanPostProcessor();
    }

    @Bean
    public DefaultAdvisorAutoProxyCreator defaultAdvisorAutoProxyCreator() {
        DefaultAdvisorAutoProxyCreator proxyCreator = new DefaultAdvisorAutoProxyCreator();
        proxyCreator.setProxyTargetClass(true);
        return proxyCreator;
    }

    @Bean
    public AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor(SecurityManager securityManager) {
        AuthorizationAttributeSourceAdvisor advisor = new AuthorizationAttributeSourceAdvisor();
        advisor.setSecurityManager(securityManager);
        return advisor;
    }

}

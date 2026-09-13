package com.yanyu.todo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/*
 * CORS 跨域说明：
 *
 * 前端运行在 http://localhost:5173，
 * 后端运行在 http://localhost:8080。
 * 由于端口不同，前端请求后端接口属于跨域请求。
 *
 * 浏览器要求后端响应中包含：
 *
 * Access-Control-Allow-Origin: http://localhost:5173
 *
 * WebConfig 配置 Spring MVC 层的 CORS，
 * SecurityConfig 配置 Spring Security 过滤器链的 CORS。
 *
 * 只要其中任意一处配置仍然生效，后端就可能添加
 * Access-Control-Allow-Origin 响应头，浏览器就不会报 CORS 错误。
 *
 * 当两处配置都关闭时，接口仍可能正常返回 200 OK，
 * 但由于响应缺少 Access-Control-Allow-Origin，
 * 浏览器会阻止前端 JavaScript 读取响应，并在控制台报告 CORS 错误。
 *
 * 注意：
 * 200 OK 表示后端处理成功；
 * net::ERR_FAILED 表示浏览器因 CORS 安全策略丢弃了响应，
 * 两者可以同时出现。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry){
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET","POST","PUT","PATCH","DELETE","OPTIONS")
                .allowedHeaders("*");
    }
}

package com.analistas.lavadero.web.config;

import jakarta.servlet.http.*;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class PrivacidadConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(
                        new HandlerInterceptor() {
                            @Override
                            public boolean preHandle(
                                    HttpServletRequest req, HttpServletResponse res, Object h) {
                                res.setHeader("Cache-Control", "no-store, max-age=0");
                                res.setHeader("X-Robots-Tag", "noindex, nofollow, noarchive");
                                return true;
                            }
                        })
                .addPathPatterns(
                        "/api/**", "/admin/**", "/operacion/**", "/pantalla", "/vehiculo/**");
    }
}

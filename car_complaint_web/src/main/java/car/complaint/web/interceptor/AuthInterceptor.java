package car.complaint.web.interceptor;

import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * 登录拦截器：检查 Session 中是否有登录用户
 * 白名单路径：/api/login, /api/logout, /login.html, /static/**
 */
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        String path = request.getRequestURI();

        // 白名单
        if (path.startsWith("/login.html") || path.startsWith("/api/login")
                || path.startsWith("/api/logout") || path.equals("/")
                || path.startsWith("/css") || path.startsWith("/js")
                || path.startsWith("/api/ai")   // AI接口独立鉴权
                || path.startsWith("/api/brand")|| path.startsWith("/api/series")
                || path.startsWith("/api/problem")|| path.startsWith("/api/trend")) {
            return true;
        }

        // 管理接口需要登录
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("user") != null) {
            return true;
        }

        // 未登录重定向到登录页
        response.sendRedirect("/login.html");
        return false;
    }
}

package car.complaint.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.*;

/**
 * 登录/登出控制器
 */
@RestController
@CrossOrigin(origins = "*")
public class LoginController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 登录
     * POST /api/login
     * Body: { "username": "admin", "password": "admin123" }
     */
    @PostMapping("/api/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body, HttpSession session) {
        String username = body.get("username");
        String password = body.get("password");

        Map<String, Object> result = new LinkedHashMap<>();

        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            result.put("success", false);
            result.put("message", "用户名和密码不能为空");
            return result;
        }

        // MD5验证
        String sql = "SELECT id, username, nickname, role FROM users WHERE username = ? AND password = MD5(?)";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, username, password);

        if (rows.isEmpty()) {
            result.put("success", false);
            result.put("message", "用户名或密码错误");
            return result;
        }

        Map<String, Object> user = rows.get(0);
        session.setAttribute("user", user);

        result.put("success", true);
        result.put("message", "登录成功");
        result.put("user", user);
        return result;
    }

    /**
     * 登出
     */
    @PostMapping("/api/logout")
    public Map<String, Object> logout(HttpSession session) {
        session.invalidate();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        return result;
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/api/user/info")
    public Map<String, Object> userInfo(HttpSession session) {
        Map<String, Object> result = new LinkedHashMap<>();
        Object user = session.getAttribute("user");
        if (user != null) {
            result.put("success", true);
            result.put("user", user);
        } else {
            result.put("success", false);
            result.put("message", "未登录");
        }
        return result;
    }
}

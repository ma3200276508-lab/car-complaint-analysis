package car.complaint.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 投诉数据管理 — 完整CRUD
 */
@RestController
@RequestMapping("/api/manage")
@CrossOrigin(origins = "*")
public class ComplaintManageController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ============ 列表（分页+搜索） ============
    @GetMapping("/list")
    public Map<String, Object> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String brand,
            @RequestParam(defaultValue = "") String status) {

        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (!keyword.isEmpty()) {
            where.append(" AND (description LIKE ? OR series LIKE ? OR model LIKE ?)");
            String kw = "%" + keyword + "%";
            params.add(kw); params.add(kw); params.add(kw);
        }
        if (!brand.isEmpty()) {
            where.append(" AND brand = ?");
            params.add(brand);
        }
        if (!status.isEmpty()) {
            where.append(" AND status = ?");
            params.add(status);
        }

        // 总数
        String countSql = "SELECT COUNT(*) FROM complaints" + where;
        int total = jdbcTemplate.queryForObject(countSql, Integer.class, params.toArray());

        // 分页查询
        int offset = (page - 1) * size;
        String sql = "SELECT * FROM complaints" + where + " ORDER BY id DESC LIMIT ?, ?";
        params.add(offset);
        params.add(size);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, params.toArray());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("data", rows);
        return result;
    }

    // ============ 新增 ============
    @PostMapping("/add")
    public Map<String, Object> add(@RequestBody Map<String, Object> body, HttpSession session) {
        Map<String, Object> user = (Map<String, Object>) session.getAttribute("user");
        String operator = user != null ? (String) user.get("username") : "system";

        String sql = "INSERT INTO complaints (brand, series, model, description, " +
                "problem_category, problem_type, complaint_date, status, created_by) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql,
                body.get("brand"), body.get("series"), body.get("model"),
                body.get("description"), body.get("problem_category"),
                body.get("problem_type"), body.get("complaint_date"),
                body.getOrDefault("status", "待处理"), operator);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("message", "添加成功");
        return result;
    }

    // ============ 编辑 ============
    @PutMapping("/update/{id}")
    public Map<String, Object> update(@PathVariable int id, @RequestBody Map<String, Object> body) {
        String sql = "UPDATE complaints SET brand=?, series=?, model=?, description=?, " +
                "problem_category=?, problem_type=?, complaint_date=?, status=?, remark=? WHERE id=?";

        jdbcTemplate.update(sql,
                body.get("brand"), body.get("series"), body.get("model"),
                body.get("description"), body.get("problem_category"),
                body.get("problem_type"), body.get("complaint_date"),
                body.getOrDefault("status", "待处理"),
                body.getOrDefault("remark", ""), id);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("message", "更新成功");
        return result;
    }

    // ============ 删除 ============
    @DeleteMapping("/delete/{id}")
    public Map<String, Object> delete(@PathVariable int id) {
        jdbcTemplate.update("DELETE FROM complaints WHERE id = ?", id);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("message", "删除成功");
        return result;
    }

    // ============ 批量删除 ============
    @PostMapping("/batch-delete")
    public Map<String, Object> batchDelete(@RequestBody Map<String, List<Integer>> body) {
        List<Integer> ids = body.get("ids");
        if (ids != null && !ids.isEmpty()) {
            for (int id : ids) {
                jdbcTemplate.update("DELETE FROM complaints WHERE id = ?", id);
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("message", "批量删除成功");
        return result;
    }

    // ============ 品牌列表（下拉选项） ============
    @GetMapping("/brands")
    public List<String> brands() {
        return jdbcTemplate.queryForList(
                "SELECT DISTINCT brand FROM complaints ORDER BY brand", String.class);
    }

    // ============ 统计概览 ============
    @GetMapping("/stats")
    public Map<String, Object> stats() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM complaints", Integer.class));
        result.put("pending", jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM complaints WHERE status = '待处理'", Integer.class));
        result.put("resolved", jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM complaints WHERE status = '已处理'", Integer.class));
        result.put("brands", jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT brand) FROM complaints", Integer.class));
        return result;
    }
}

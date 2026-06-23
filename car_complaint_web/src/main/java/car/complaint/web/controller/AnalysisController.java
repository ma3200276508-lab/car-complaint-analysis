package car.complaint.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@CrossOrigin(origins = "*")
public class AnalysisController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 品牌投诉 TOP20
    @GetMapping("/api/brand/top20")
    public Map<String, Object> getBrandTop20() {
        String sql = "SELECT name, cnt, problem_category_count, problem_type_count FROM brand_analysis ORDER BY cnt DESC LIMIT 20";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

        List<String> brand = new ArrayList<>();
        List<Integer> cnt = new ArrayList<>();
        List<Integer> catCount = new ArrayList<>();
        List<Integer> typeCount = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            brand.add((String) row.get("name"));
            cnt.add(((Number) row.get("cnt")).intValue());
            catCount.add(((Number) row.get("problem_category_count")).intValue());
            typeCount.add(((Number) row.get("problem_type_count")).intValue());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("brand", brand);
        result.put("cnt", cnt);
        result.put("problem_category_count", catCount);
        result.put("problem_type_count", typeCount);
        return result;
    }

    // 车系投诉 TOP15
    @GetMapping("/api/series/top15")
    public Map<String, Object> getSeriesTop15() {
        String sql = "SELECT name, cnt FROM series_analysis ORDER BY cnt DESC LIMIT 15";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

        List<String> series = new ArrayList<>();
        List<Integer> cnt = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            series.add((String) row.get("name"));
            cnt.add(((Number) row.get("cnt")).intValue());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("series", series);
        result.put("cnt", cnt);
        return result;
    }

    // 问题大类分布
    @GetMapping("/api/problem/category")
    public Map<String, Object> getProblemCategory() {
        String sql = "SELECT name, cnt FROM problem_category_analysis ORDER BY cnt DESC";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

        List<String> category = new ArrayList<>();
        List<Integer> cnt = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            category.add((String) row.get("name"));
            cnt.add(((Number) row.get("cnt")).intValue());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("category", category);
        result.put("cnt", cnt);
        return result;
    }

    // 问题类型 TOP20
    @GetMapping("/api/problem/type")
    public Map<String, Object> getProblemType() {
        String sql = "SELECT name, cnt FROM problem_type_analysis ORDER BY cnt DESC LIMIT 20";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

        List<String> type = new ArrayList<>();
        List<Integer> cnt = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            type.add((String) row.get("name"));
            cnt.add(((Number) row.get("cnt")).intValue());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", type);
        result.put("cnt", cnt);
        return result;
    }

    // 品牌×问题交叉数据
    @GetMapping("/api/brand/problem/cross")
    public Map<String, Object> getBrandProblemCross() {
        String sql = "SELECT brand, problem_category, cnt FROM brand_problem_cross ORDER BY cnt DESC LIMIT 200";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

        // 构建交叉矩阵
        Set<String> brandSet = new LinkedHashSet<>();
        Set<String> catSet = new LinkedHashSet<>();
        Map<String, Map<String, Integer>> matrix = new LinkedHashMap<>();

        for (Map<String, Object> row : rows) {
            String brand = (String) row.get("brand");
            String cat = (String) row.get("problem_category");
            int cnt = ((Number) row.get("cnt")).intValue();

            brandSet.add(brand);
            catSet.add(cat);
            matrix.computeIfAbsent(brand, k -> new LinkedHashMap<>()).put(cat, cnt);
        }

        List<String> brands = new ArrayList<>(brandSet);
        List<String> categories = new ArrayList<>(catSet);

        List<List<Integer>> data = new ArrayList<>();
        for (String brand : brands) {
            List<Integer> rowData = new ArrayList<>();
            Map<String, Integer> brandData = matrix.getOrDefault(brand, Collections.emptyMap());
            for (String cat : categories) {
                rowData.add(brandData.getOrDefault(cat, 0));
            }
            data.add(rowData);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("brand", brands);
        result.put("category", categories);
        result.put("data", data);
        return result;
    }

    // 日投诉趋势
    @GetMapping("/api/trend/daily")
    public Map<String, Object> getDailyTrend() {
        String sql = "SELECT day, cnt FROM daily_trend ORDER BY day";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

        List<String> date = new ArrayList<>();
        List<Integer> cnt = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            date.add(row.get("day").toString());
            cnt.add(((Number) row.get("cnt")).intValue());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("date", date);
        result.put("cnt", cnt);
        return result;
    }
}

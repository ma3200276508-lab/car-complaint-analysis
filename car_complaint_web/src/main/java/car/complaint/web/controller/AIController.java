package car.complaint.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

/**
 * AI 智能分析接口
 *
 * 集成大模型 API（支持通义千问 / 文心一言 / OpenAI 兼容接口）
 * 配置项在 application.properties:
 *   ai.api.url=https://your-llm-api-endpoint
 *   ai.api.key=your-api-key
 *   ai.api.model=qwen-turbo
 */
@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AIController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${ai.api.url:}")
    private String aiApiUrl;

    @Value("${ai.api.key:}")
    private String aiApiKey;

    @Value("${ai.api.model:qwen-turbo}")
    private String aiModel;

    /**
     * AI 智能分析
     * POST /api/ai/analyze
     * Body: { "query": "分析比亚迪的投诉问题" }
     */
    @PostMapping("/analyze")
    public Map<String, Object> analyze(@RequestBody Map<String, String> body) {
        String query = body.getOrDefault("query", "");
        Map<String, Object> result = new LinkedHashMap<>();

        // 1. 先做本地数据统计
        Map<String, Object> localStats = buildLocalStats();

        // 2. 构建给大模型的 prompt
        String prompt = buildPrompt(query, localStats);

        // 3. 调用大模型API（如果没配API则返回本地分析）
        if (aiApiUrl.isEmpty() || aiApiKey.isEmpty()) {
            result.put("success", true);
            result.put("type", "local");
            result.put("analysis", generateLocalAnalysis(query, localStats));
            result.put("stats", localStats);
            return result;
        }

        try {
            String aiResponse = callLLM(prompt);
            result.put("success", true);
            result.put("type", "ai");
            result.put("analysis", aiResponse);
            result.put("stats", localStats);
        } catch (Exception e) {
            result.put("success", true);
            result.put("type", "local_fallback");
            result.put("analysis", generateLocalAnalysis(query, localStats) + "\n\n(大模型调用失败，使用本地分析)");
            result.put("stats", localStats);
        }

        return result;
    }

    // ============ 本地数据统计 ============
    private Map<String, Object> buildLocalStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        // 品牌TOP5
        stats.put("topBrands", jdbcTemplate.queryForList(
                "SELECT name, cnt FROM brand_analysis ORDER BY cnt DESC LIMIT 5"));

        // 问题TOP5
        stats.put("topProblems", jdbcTemplate.queryForList(
                "SELECT name, cnt FROM problem_category_analysis ORDER BY cnt DESC LIMIT 5"));

        // 类型TOP5
        stats.put("topTypes", jdbcTemplate.queryForList(
                "SELECT name, cnt FROM problem_type_analysis ORDER BY cnt DESC LIMIT 5"));

        // 总数
        stats.put("totalComplaints", jdbcTemplate.queryForObject(
                "SELECT SUM(cnt) FROM brand_analysis", Integer.class));

        // 总品牌数
        stats.put("totalBrands", jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM brand_analysis", Integer.class));

        return stats;
    }

    // ============ 构建 Prompt ============
    private String buildPrompt(String query, Map<String, Object> stats) {
        return "你是一个汽车投诉数据分析专家。请根据以下数据对用户问题进行专业分析。" +
               "\n\n## 用户问题：" + query +
               "\n\n## 数据统计：" +
               "\n- 投诉总量：" + stats.get("totalComplaints") + " 条" +
               "\n- 品牌数：" + stats.get("totalBrands") + " 个" +
               "\n- 投诉TOP5品牌：" + formatList(stats.get("topBrands")) +
               "\n- 投诉TOP5问题：" + formatList(stats.get("topProblems")) +
               "\n- 投诉TOP5类型：" + formatList(stats.get("topTypes")) +
               "\n\n请给出专业的分析报告，包括：1) 问题概述 2) 数据解读 3) 可能原因 4) 改进建议。" +
               "\n用中文回答，约300字。";
    }

    private String formatList(Object obj) {
        if (!(obj instanceof List)) return "无数据";
        List<Map<String, Object>> list = (List<Map<String, Object>>) obj;
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> item : list) {
            sb.append(item.get("name")).append("(").append(item.get("cnt")).append("条) ");
        }
        return sb.toString();
    }

    // ============ 本地规则分析（无API时使用） ============
    private String generateLocalAnalysis(String query, Map<String, Object> stats) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 汽车投诉数据分析报告\n\n");
        sb.append("### 一、数据概览\n");
        sb.append("当前系统共记录 **").append(stats.get("totalComplaints")).append("** 条投诉，");
        sb.append("覆盖 **").append(stats.get("totalBrands")).append("** 个品牌。\n\n");

        sb.append("### 二、投诉品牌分析\n");
        List<Map<String, Object>> brands = (List<Map<String, Object>>) stats.get("topBrands");
        if (brands != null && !brands.isEmpty()) {
            Map<String, Object> top = brands.get(0);
            sb.append("投诉量最高的品牌是 **").append(top.get("name")).append("**（")
              .append(top.get("cnt")).append("条），远超其他品牌。\n");
        }
        sb.append("建议重点关注高投诉品牌的售后服务质量。\n\n");

        sb.append("### 三、主要问题分析\n");
        List<Map<String, Object>> problems = (List<Map<String, Object>>) stats.get("topProblems");
        if (problems != null && !problems.isEmpty()) {
            sb.append("投诉主要集中在以下领域：");
            for (int i = 0; i < Math.min(3, problems.size()); i++) {
                sb.append(problems.get(i).get("name")).append("(").append(problems.get(i).get("cnt")).append("条)");
                if (i < Math.min(3, problems.size()) - 1) sb.append("、");
            }
            sb.append("。\n");
        }
        sb.append("建议制造商针对高频问题类型进行专项质量改进。\n\n");

        sb.append("### 四、改进建议\n");
        sb.append("1. 建立品牌投诉预警机制，当单一品牌投诉量异常增长时自动告警。\n");
        sb.append("2. 针对高频问题类型（如异响、部件开裂）开展供应商质量审核。\n");
        sb.append("3. 定期生成投诉分析月报，跟踪问题趋势变化。\n");

        return sb.toString();
    }

    // ============ 大模型API调用 ============
    private String callLLM(String prompt) throws Exception {
        URL url = new URL(aiApiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + aiApiKey);
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(60000);

        // 构建 OpenAI 兼容格式的请求体
        String requestBody = String.format(
                "{\"model\":\"%s\",\"messages\":[" +
                "{\"role\":\"system\",\"content\":\"你是汽车投诉数据分析专家\"}," +
                "{\"role\":\"user\",\"content\":\"%s\"}]}",
                aiModel, prompt.replace("\"", "\\\"").replace("\n", "\\n"));

        try (OutputStream os = conn.getOutputStream()) {
            os.write(requestBody.getBytes("UTF-8"));
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        // 简单解析 OpenAI 格式返回
        String respStr = response.toString();
        // 从 JSON 中提取 content 字段
        int contentIdx = respStr.indexOf("\"content\":\"");
        if (contentIdx > 0) {
            int start = contentIdx + 11;
            int end = respStr.indexOf("\"}", start);
            if (end < 0) end = respStr.indexOf("\",\"", start);
            if (end > 0) {
                return respStr.substring(start, end)
                        .replace("\\n", "\n").replace("\\\"", "\"");
            }
        }

        return respStr;
    }
}

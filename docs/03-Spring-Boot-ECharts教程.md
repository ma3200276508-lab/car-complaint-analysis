# 03 — Spring Boot + ECharts Web 开发教程

> 构建 RESTful API 后端 + ECharts 可视化仪表盘大屏

## 项目结构

```
car_complaint_web/
├── pom.xml
└── src/main/
    ├── java/car/complaint/web/
    │   ├── ComplaintWebApplication.java      # Spring Boot 启动类
    │   └── controller/
    │       └── AnalysisController.java       # 6个REST API控制器
    └── resources/
        ├── application.properties            # 数据源 + 端口配置
        └── static/
            └── index.html                    # ECharts 可视化大屏
```

---

## 1. MariaDB 数据库建表

### 创建数据库

```sql
CREATE DATABASE IF NOT EXISTS car_complaint
  DEFAULT CHARSET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

USE car_complaint;
```

### 6张分析表DDL

```sql
-- 品牌投诉分析表（自然主键：品牌名）
CREATE TABLE brand_analysis (
  name VARCHAR(100) PRIMARY KEY COMMENT '品牌名称',
  cnt INT COMMENT '投诉量',
  problem_category_count INT COMMENT '涉及问题大类数',
  problem_type_count INT COMMENT '涉及问题类型数'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 车系投诉分析表
CREATE TABLE series_analysis (
  name VARCHAR(200) PRIMARY KEY COMMENT '车系名称',
  cnt INT COMMENT '投诉量'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 问题大类分析表
CREATE TABLE problem_category_analysis (
  name VARCHAR(100) PRIMARY KEY COMMENT '问题大类',
  cnt INT COMMENT '投诉量'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 问题类型分析表
CREATE TABLE problem_type_analysis (
  name VARCHAR(200) PRIMARY KEY COMMENT '问题类型',
  cnt INT COMMENT '投诉量'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 品牌×问题交叉表（代理主键：自增ID）
CREATE TABLE brand_problem_cross (
  id INT AUTO_INCREMENT PRIMARY KEY,
  brand VARCHAR(100) COMMENT '品牌',
  problem_category VARCHAR(100) COMMENT '问题大类',
  cnt INT COMMENT '投诉量'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 日投诉趋势表
CREATE TABLE daily_trend (
  day DATE PRIMARY KEY COMMENT '投诉日期',
  cnt INT COMMENT '投诉量'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 批量导入CSV数据

```bash
# 从HDFS拉取分析结果
hdfs dfs -get /car_complaint/output/BrandAnalysis/part-* /tmp/brand_data/
cat /tmp/brand_data/part-* > /tmp/brand_data.csv

# LOAD DATA 高速导入MariaDB
mysql -u root -p car_complaint -e "
LOAD DATA LOCAL INFILE '/tmp/brand_data.csv'
INTO TABLE brand_analysis
FIELDS TERMINATED BY ',' LINES TERMINATED BY '\n';
"

# 有自增ID的表需指定列（跳过id）
mysql -u root -p car_complaint -e "
LOAD DATA LOCAL INFILE '/tmp/brand_cross_data.csv'
INTO TABLE brand_problem_cross
FIELDS TERMINATED BY ',' LINES TERMINATED BY '\n'
(brand, problem_category, cnt);
"
```

| 表名 | 导入行数 |
|------|----------|
| brand_analysis | 118 |
| series_analysis | 670 |
| problem_category_analysis | 19 |
| problem_type_analysis | 110 |
| brand_problem_cross | 733 |
| daily_trend | 92 |

---

## 2. Spring Boot 后端开发

### 2.1 pom.xml 核心依赖

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.7.18</version>
</parent>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-jdbc</artifactId>
    </dependency>
    <dependency>
        <groupId>org.mariadb.jdbc</groupId>
        <artifactId>mariadb-java-client</artifactId>
        <version>3.1.4</version>
    </dependency>
</dependencies>
```

### 2.2 application.properties

```properties
server.port=8080

spring.datasource.url=jdbc:mariadb://192.168.88.101:3306/car_complaint?useSSL=false&characterEncoding=utf8
spring.datasource.username=root
spring.datasource.password=123456
spring.datasource.driver-class-name=org.mariadb.jdbc.Driver

# HikariCP连接池（Spring Boot默认）
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
```

### 2.3 启动类

```java
package car.complaint.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication  // = @Configuration + @EnableAutoConfiguration + @ComponentScan
public class ComplaintWebApplication {
    public static void main(String[] args) {
        SpringApplication.run(ComplaintWebApplication.class, args);
    }
}
```

### 2.4 REST API 控制器

```java
package car.complaint.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@CrossOrigin(origins = "*")  // 允许跨域
public class AnalysisController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ============ API 1: 品牌TOP20 ============
    @GetMapping("/api/brand/top20")
    public Map<String, Object> getBrandTop20() {
        String sql = "SELECT name, cnt FROM brand_analysis ORDER BY cnt DESC LIMIT 20";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

        List<String> brands = new ArrayList<>();
        List<Integer> cnts = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            brands.add((String) row.get("name"));
            cnts.add(((Number) row.get("cnt")).intValue());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("brand", brands);
        result.put("cnt", cnts);
        return result;  // Jackson自动序列化为JSON
    }

    // ============ API 2: 车系TOP15 ============
    @GetMapping("/api/series/top15")
    public Map<String, Object> getSeriesTop15() {
        String sql = "SELECT name, cnt FROM series_analysis ORDER BY cnt DESC LIMIT 15";
        // ... 同样模式
    }

    // ============ API 3: 问题大类 ============
    @GetMapping("/api/problem/category")
    public Map<String, Object> getProblemCategory() {
        String sql = "SELECT name, cnt FROM problem_category_analysis ORDER BY cnt DESC";
        // ...
    }

    // ============ API 4: 问题类型TOP20 ============
    @GetMapping("/api/problem/type")
    public Map<String, Object> getProblemType() {
        String sql = "SELECT name, cnt FROM problem_type_analysis ORDER BY cnt DESC LIMIT 20";
        // ...
    }

    // ============ API 5: 品牌×问题热力图数据 ============
    @GetMapping("/api/brand/problem/cross")
    public Map<String, Object> getBrandProblemCross() {
        String sql = "SELECT brand, problem_category, cnt " +
                     "FROM brand_problem_cross ORDER BY cnt DESC LIMIT 200";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

        // 构建二维矩阵 → 转热力图三元组
        Set<String> brandSet = new LinkedHashSet<>();
        Set<String> catSet = new LinkedHashSet<>();
        Map<String, Map<String, Integer>> matrix = new LinkedHashMap<>();

        for (Map<String, Object> row : rows) {
            String b = (String) row.get("brand");
            String c = (String) row.get("problem_category");
            int v = ((Number) row.get("cnt")).intValue();
            brandSet.add(b);
            catSet.add(c);
            matrix.computeIfAbsent(b, k -> new LinkedHashMap<>()).put(c, v);
        }

        List<String> brands = new ArrayList<>(brandSet);
        List<String> categories = new ArrayList<>(catSet);
        List<List<Integer>> data = new ArrayList<>();
        for (String b : brands) {
            List<Integer> rd = new ArrayList<>();
            Map<String, Integer> md = matrix.getOrDefault(b, Collections.emptyMap());
            for (String c : categories) {
                rd.add(md.getOrDefault(c, 0));
            }
            data.add(rd);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("brand", brands);
        result.put("category", categories);
        result.put("data", data);
        return result;
    }

    // ============ API 6: 日投诉趋势 ============
    @GetMapping("/api/trend/daily")
    public Map<String, Object> getDailyTrend() {
        String sql = "SELECT day, cnt FROM daily_trend ORDER BY day";
        // ...
    }
}
```

### 2.5 6个API接口总览

| API | 响应JSON | ECharts图表 |
|-----|----------|-------------|
| `GET /api/brand/top20` | `{brand:[], cnt:[]}` | 横向柱状图 |
| `GET /api/series/top15` | `{series:[], cnt:[]}` | 横向柱状图 |
| `GET /api/problem/category` | `{category:[], cnt:[]}` | 玫瑰饼图 |
| `GET /api/problem/type` | `{type:[], cnt:[]}` | 柱状图 |
| `GET /api/brand/problem/cross` | `{brand:[], category:[], data:[[..]]}` | 热力图 |
| `GET /api/trend/daily` | `{date:[], cnt:[]}` | 折线图 |

---

## 3. ECharts 前端大屏

### 3.1 整体设计

- **布局**：CSS Grid 两列，gap 16px
- **主题**：深蓝黑背景 (#0a0e27)，蓝色渐变文字标题
- **图表**：6张ECharts图表，异步Fetch加载

### 3.2 核心CSS

```css
body {
  background: #0a0e27;
  color: #fff;
}

.header h1 {
  background: linear-gradient(90deg, #4da8ff, #fff, #4da8ff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.panel {
  background: rgba(16, 20, 60, 0.9);
  border: 1px solid #1e3a8a;
}

.panel::before {  /* 顶部发光边线 */
  content: "";
  background: linear-gradient(90deg, transparent, #2a4eff, transparent);
}
```

### 3.3 ECharts 图表配置模式

```javascript
// 通用Fetch+ECharts模式
fetch('/api/brand/top20')        // 异步请求后端API
  .then(res => res.json())       // 解析JSON
  .then(data => {
    const chart = echarts.init(   // 初始化图表
      document.getElementById('brand-chart'));
    chart.setOption({             // 声明式配置
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'value' },
      yAxis: { type: 'category', data: data.brand.reverse() },
      series: [{
        type: 'bar',
        data: data.cnt.reverse(),
        itemStyle: { color: gradient }
      }]
    });
  });
```

### 3.4 6张图表类型

| 图表 | ECharts type | 关键配置 |
|------|-------------|----------|
| 品牌TOP20 | `bar` | yAxis.inverse + LinearGradient |
| 日趋势 | `line` | smooth:true + areaStyle渐变 |
| 问题大类 | `pie` | radius: ["45%","75%"] 玫瑰图 |
| 问题类型 | `bar` | 标准柱状图，橙色配色 |
| 热力图 | `heatmap` | visualMap + [ci,bi,val]三元组 |
| 车系TOP15 | `bar` | 紫色渐变，横向布局 |

---

## 4. 运行

```bash
# IDEA中打开 car_complaint_web 项目
# 运行 ComplaintWebApplication.main()
# 或：mvn spring-boot:run

# 浏览器访问
http://localhost:8080        # ECharts可视化大屏
http://localhost:8080/api/brand/top20   # 品牌API
```

---

## 下一步

遇到报错？查看 [04-常见问题排查.md](04-常见问题排查.md)。

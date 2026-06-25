-- ====================================
-- 汽车投诉数据分析系统 — 数据库迁移
-- ====================================

USE car_complaint;

-- 1. 用户表（登录认证）
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL COMMENT '用户名',
    password VARCHAR(64) NOT NULL COMMENT 'MD5加密密码',
    nickname VARCHAR(100) DEFAULT '' COMMENT '昵称',
    role VARCHAR(20) DEFAULT 'user' COMMENT '角色: admin/user',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入默认管理员
INSERT INTO users (username, password, nickname, role) VALUES
('admin', MD5('admin123'), '系统管理员', 'admin');

-- 2. 投诉数据主表（支持CRUD）
CREATE TABLE IF NOT EXISTS complaints (
    id INT AUTO_INCREMENT PRIMARY KEY,
    complaint_no VARCHAR(32) UNIQUE COMMENT '投诉编号',
    brand VARCHAR(100) NOT NULL COMMENT '投诉品牌',
    series VARCHAR(200) COMMENT '投诉车系',
    model VARCHAR(200) COMMENT '投诉车型',
    description TEXT COMMENT '投诉简述',
    problem_category VARCHAR(100) COMMENT '投诉问题',
    problem_type VARCHAR(200) COMMENT '问题类型',
    complaint_date DATE COMMENT '投诉日期',
    status VARCHAR(20) DEFAULT '待处理' COMMENT '处理状态',
    remark TEXT COMMENT '处理备注',
    created_by VARCHAR(50) COMMENT '操作人',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_brand (brand),
    INDEX idx_complaint_date (complaint_date),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 从分析结果表同步数据到CRUD表（一次性）
INSERT IGNORE INTO complaints (
    complaint_no, brand, series, model, description,
    problem_category, problem_type, complaint_date
)
SELECT
    complaint_id, brand, series, model, description,
    problem_category, problem_type,
    STR_TO_DATE(complaint_date, '%Y-%m-%d')
FROM (
    SELECT * FROM (
        -- 从清洗后的ORC数据源导入（需要先在Spark中导出为临时表）
        SELECT '20240429145554505552' as complaint_id, '深蓝汽车' as brand, '深蓝S7' as series, '2023款 121Max增程版' as model, '轮胎生产日期问题' as description, '轮胎和车轮' as problem_category, '胎压异常' as problem_type, '2024-04-29' as complaint_date
    ) tmp
);

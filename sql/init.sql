-- =============================================
-- 农业保险快速定损系统 - 数据库初始化脚本
-- Database: MySQL 8.0+ / 达梦DB 8
-- =============================================

-- ---------- 系统模块 ----------

CREATE TABLE IF NOT EXISTS sys_user (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    user_name       VARCHAR(64)  NOT NULL UNIQUE COMMENT '用户名',
    nick_name       VARCHAR(64)  DEFAULT NULL COMMENT '昵称',
    password        VARCHAR(128) NOT NULL COMMENT '密码',
    avatar          VARCHAR(255) DEFAULT NULL COMMENT '头像',
    email           VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    phone           VARCHAR(32)  DEFAULT NULL COMMENT '手机号',
    dept_id         BIGINT       DEFAULT NULL COMMENT '部门ID',
    role_key        VARCHAR(64)  DEFAULT 'user' COMMENT '角色标识',
    status          TINYINT      DEFAULT 0 COMMENT '状态 0启用 1禁用',
    remark          VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT      DEFAULT 0 COMMENT '逻辑删除 0否 1是',
    INDEX idx_user_name (user_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

CREATE TABLE IF NOT EXISTS sys_role (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID',
    role_name       VARCHAR(64)  NOT NULL COMMENT '角色名称',
    role_key        VARCHAR(64)  NOT NULL UNIQUE COMMENT '角色编码',
    sort            INT          DEFAULT 0 COMMENT '排序',
    status          TINYINT      DEFAULT 0 COMMENT '状态 0启用 1禁用',
    remark          VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT      DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';

CREATE TABLE IF NOT EXISTS sys_menu (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '菜单ID',
    parent_id       BIGINT       DEFAULT 0 COMMENT '父菜单ID',
    menu_name       VARCHAR(64)  NOT NULL COMMENT '菜单名称',
    path            VARCHAR(255) DEFAULT NULL COMMENT '路由路径',
    component       VARCHAR(255) DEFAULT NULL COMMENT '组件路径',
    icon            VARCHAR(64)  DEFAULT NULL COMMENT '图标',
    sort            INT          DEFAULT 0 COMMENT '排序',
    menu_type       CHAR(1)      DEFAULT 'M' COMMENT '类型 M目录 C菜单 F按钮',
    perms           VARCHAR(128) DEFAULT NULL COMMENT '权限标识',
    visible         TINYINT      DEFAULT 0 COMMENT '显示状态 0显示 1隐藏',
    status          TINYINT      DEFAULT 0 COMMENT '状态 0启用 1禁用',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT      DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统菜单表';

CREATE TABLE IF NOT EXISTS sys_dict (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '字典ID',
    dict_name       VARCHAR(128) NOT NULL COMMENT '字典名称',
    dict_type       VARCHAR(64)  NOT NULL UNIQUE COMMENT '字典类型',
    status          TINYINT      DEFAULT 0 COMMENT '状态 0启用 1禁用',
    remark          VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT      DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典类型表';

CREATE TABLE IF NOT EXISTS sys_dict_item (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '字典项ID',
    dict_id         BIGINT       NOT NULL COMMENT '字典ID',
    dict_type       VARCHAR(64)  NOT NULL COMMENT '字典类型',
    item_label      VARCHAR(128) NOT NULL COMMENT '标签',
    item_value      VARCHAR(255) NOT NULL COMMENT '值',
    sort            INT          DEFAULT 0 COMMENT '排序',
    status          TINYINT      DEFAULT 0 COMMENT '状态 0启用 1禁用',
    remark          VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT      DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_dict_type (dict_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典数据表';

-- ---------- 影像模块 ----------

CREATE TABLE IF NOT EXISTS image_info (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '影像ID',
    image_name      VARCHAR(255) NOT NULL COMMENT '存储文件名',
    original_name   VARCHAR(255) DEFAULT NULL COMMENT '原始文件名',
    image_type      VARCHAR(32)  NOT NULL COMMENT '影像类型 BEFORE/AFTER/DOM/MASK',
    disaster_type   VARCHAR(32)  DEFAULT NULL COMMENT '灾害类型 FLOOD/LODGE/WITHER',
    image_status    VARCHAR(32)  DEFAULT 'UPLOADED' COMMENT '处理状态 UPLOADED/PREPROCESSING/PREPROCESSED/FAILED',
    bucket_name     VARCHAR(128) NOT NULL COMMENT 'MinIO存储桶',
    object_key      VARCHAR(500) NOT NULL COMMENT '对象存储Key',
    thumbnail_key   VARCHAR(500) DEFAULT NULL COMMENT '缩略图Key',
    dom_key         VARCHAR(500) DEFAULT NULL COMMENT 'DOM正射影像Key',
    file_size       BIGINT       DEFAULT NULL COMMENT '文件大小(字节)',
    file_format     VARCHAR(16)  DEFAULT NULL COMMENT '文件格式 JPG/TIF/PNG等',
    width           INT          DEFAULT NULL COMMENT '宽度(像素)',
    height          INT          DEFAULT NULL COMMENT '高度(像素)',
    resolution      INT          DEFAULT NULL COMMENT '分辨率(DPI)',
    quality_score   DECIMAL(5,2) DEFAULT NULL COMMENT '质量评分 0-100',
    coordinate_system VARCHAR(64) DEFAULT NULL COMMENT '坐标系 EPSG:4326等',
    upper_left_lon  DECIMAL(12,8) DEFAULT NULL COMMENT '左上角经度',
    upper_left_lat  DECIMAL(12,8) DEFAULT NULL COMMENT '左上角纬度',
    lower_right_lon DECIMAL(12,8) DEFAULT NULL COMMENT '右下角经度',
    lower_right_lat DECIMAL(12,8) DEFAULT NULL COMMENT '右下角纬度',
    center_lon      DECIMAL(12,8) DEFAULT NULL COMMENT '中心点经度',
    center_lat      DECIMAL(12,8) DEFAULT NULL COMMENT '中心点纬度',
    coverage_area   DECIMAL(14,4) DEFAULT NULL COMMENT '覆盖面积(亩)',
    avg_gsd         DECIMAL(8,4)  DEFAULT NULL COMMENT '平均地面采样距离(cm/pixel)',
    srs_wkt         TEXT         DEFAULT NULL COMMENT '空间参考WKT',
    geo_transform   VARCHAR(255) DEFAULT NULL COMMENT '地理变换参数',
    band_count      INT          DEFAULT NULL COMMENT '波段数',
    driver_name     VARCHAR(64)  DEFAULT NULL COMMENT 'GDAL驱动名称',
    shoot_time      VARCHAR(32)  DEFAULT NULL COMMENT '拍摄时间',
    location        VARCHAR(500) DEFAULT NULL COMMENT '地理位置描述',
    surveyor_id     BIGINT       DEFAULT NULL COMMENT '上传查勘员ID',
    surveyor_name   VARCHAR(64)  DEFAULT NULL COMMENT '上传查勘员姓名',
    mission_id      BIGINT       DEFAULT NULL COMMENT '关联定损任务ID',
    mission_name    VARCHAR(255) DEFAULT NULL COMMENT '关联定损任务名称',
    remark          VARCHAR(500) DEFAULT NULL COMMENT '备注',
    preprocess_time DATETIME     DEFAULT NULL COMMENT '预处理完成时间',
    upload_time     DATETIME     DEFAULT NULL COMMENT '上传时间',
    metadata        TEXT         DEFAULT NULL COMMENT '元数据JSON',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT      DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_mission_id (mission_id),
    INDEX idx_image_type (image_type),
    INDEX idx_image_status (image_status),
    INDEX idx_center (center_lon, center_lat)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='影像信息表';

CREATE TABLE IF NOT EXISTS image_chunk (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '分片ID',
    upload_id       VARCHAR(128) NOT NULL COMMENT '上传任务标识',
    image_id        BIGINT       DEFAULT NULL COMMENT '关联影像ID',
    file_name       VARCHAR(255) NOT NULL COMMENT '原始文件名',
    bucket_name     VARCHAR(128) NOT NULL COMMENT '存储桶',
    object_key      VARCHAR(500) NOT NULL COMMENT '对象Key',
    chunk_index     INT          NOT NULL COMMENT '分片序号 从0开始',
    total_chunks    INT          NOT NULL COMMENT '总分片数',
    chunk_size      BIGINT       NOT NULL COMMENT '分片大小(字节)',
    md5             VARCHAR(64)  DEFAULT NULL COMMENT '分片MD5',
    status          TINYINT      DEFAULT 0 COMMENT '状态 0上传中 1已上传 2已合并',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT      DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_upload_chunk (upload_id, chunk_index),
    INDEX idx_upload_id (upload_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='影像分片上传表';

-- ---------- AI处理模块 ----------

CREATE TABLE IF NOT EXISTS segment_result (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '分割结果ID',
    task_id         BIGINT       NOT NULL COMMENT '任务ID(定损任务ID)',
    image_id        BIGINT       DEFAULT NULL COMMENT '源影像ID',
    model_name      VARCHAR(128) DEFAULT 'UNet++' COMMENT '模型名称',
    model_version   VARCHAR(32)  DEFAULT NULL COMMENT '模型版本',
    segment_class   VARCHAR(64)  NOT NULL COMMENT '分割类别 FARMLAND/ROAD/BUILDING/WATER',
    crop_type       VARCHAR(64)  DEFAULT NULL COMMENT '作物类型(仅农田)',
    crop_confidence DECIMAL(6,4) DEFAULT NULL COMMENT '作物分类置信度',
    confidence      DECIMAL(6,4) DEFAULT NULL COMMENT '分割置信度',
    area            DECIMAL(14,4) DEFAULT NULL COMMENT '面积(亩)',
    polygon_wkt     TEXT         DEFAULT NULL COMMENT '多边形WKT',
    mask_path       VARCHAR(500) DEFAULT NULL COMMENT '掩膜图存储路径',
    status          VARCHAR(32)  DEFAULT 'COMPLETED' COMMENT '处理状态',
    start_time      DATETIME     DEFAULT NULL COMMENT '开始时间',
    end_time        DATETIME     DEFAULT NULL COMMENT '结束时间',
    duration        BIGINT       DEFAULT NULL COMMENT '处理耗时(毫秒)',
    metadata        TEXT         DEFAULT NULL COMMENT '扩展元数据JSON',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT      DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_task_id (task_id),
    INDEX idx_segment_class (segment_class)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI语义分割结果表';

CREATE TABLE IF NOT EXISTS change_detect_result (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '检测结果ID',
    task_id         BIGINT       NOT NULL UNIQUE COMMENT '任务ID(定损任务ID)',
    before_image_id BIGINT       DEFAULT NULL COMMENT '灾前影像ID',
    after_image_id  BIGINT       DEFAULT NULL COMMENT '灾后影像ID',
    model_name      VARCHAR(128) DEFAULT 'ChangeDetect' COMMENT '模型名称',
    model_version   VARCHAR(32)  DEFAULT NULL COMMENT '模型版本',
    ndvi_before     DECIMAL(6,4) DEFAULT NULL COMMENT '灾前NDVI值',
    ndvi_after      DECIMAL(6,4) DEFAULT NULL COMMENT '灾后NDVI值',
    ndvi_diff       DECIMAL(6,4) DEFAULT NULL COMMENT 'NDVI差值',
    disaster_type   VARCHAR(32)  DEFAULT NULL COMMENT '灾害类型 FLOOD/LODGE/WITHER',
    disaster_level  VARCHAR(32)  DEFAULT NULL COMMENT '受灾等级 LIGHT/MODERATE/SEVERE',
    disaster_area   DECIMAL(14,4) DEFAULT NULL COMMENT '受灾面积(亩)',
    disaster_ratio  DECIMAL(8,4)  DEFAULT NULL COMMENT '受灾比例(%)',
    mask_path       VARCHAR(500) DEFAULT NULL COMMENT '受灾掩膜图路径',
    status          VARCHAR(32)  DEFAULT 'COMPLETED' COMMENT '处理状态',
    start_time      DATETIME     DEFAULT NULL COMMENT '开始时间',
    end_time        DATETIME     DEFAULT NULL COMMENT '结束时间',
    duration        BIGINT       DEFAULT NULL COMMENT '处理耗时(毫秒)',
    metadata        TEXT         DEFAULT NULL COMMENT '扩展元数据JSON',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT      DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_task_id (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI变化检测结果表';

-- ---------- 定损评估模块 ----------

CREATE TABLE IF NOT EXISTS assess_mission (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '定损任务ID',
    mission_no          VARCHAR(64)  NOT NULL UNIQUE COMMENT '定损任务编号',
    mission_name        VARCHAR(255) NOT NULL COMMENT '任务名称',
    policy_id           BIGINT       DEFAULT NULL COMMENT '保单ID',
    policy_no           VARCHAR(64)  DEFAULT NULL COMMENT '保单号',
    policy_holder_name  VARCHAR(64)  DEFAULT NULL COMMENT '被保险人姓名',
    id_card_no          VARCHAR(32)  DEFAULT NULL COMMENT '身份证号',
    phone               VARCHAR(32)  DEFAULT NULL COMMENT '联系电话',
    address             VARCHAR(500) DEFAULT NULL COMMENT '保险地址',
    crop_type           VARCHAR(64)  NOT NULL COMMENT '作物类型',
    insured_area        DECIMAL(14,4) DEFAULT NULL COMMENT '投保面积(亩)',
    insured_amount      DECIMAL(14,2) DEFAULT NULL COMMENT '投保金额(元)',
    disaster_type       VARCHAR(32)  NOT NULL COMMENT '灾害类型 FLOOD/LODGE/WITHER',
    disaster_level      VARCHAR(32)  DEFAULT 'MODERATE' COMMENT '受灾等级 LIGHT/MODERATE/SEVERE',
    disaster_date       VARCHAR(32)  DEFAULT NULL COMMENT '灾害发生日期',
    disaster_location   VARCHAR(500) DEFAULT NULL COMMENT '受灾地点',
    disaster_center_lon DECIMAL(12,8) DEFAULT NULL COMMENT '受灾中心点经度',
    disaster_center_lat DECIMAL(12,8) DEFAULT NULL COMMENT '受灾中心点纬度',
    before_image_id     BIGINT       DEFAULT NULL COMMENT '灾前影像ID',
    after_image_id      BIGINT       DEFAULT NULL COMMENT '灾后影像ID',
    disaster_area       DECIMAL(14,4) DEFAULT NULL COMMENT '受灾面积(亩)',
    disaster_ratio      DECIMAL(8,4)  DEFAULT NULL COMMENT '受灾比例(%)',
    estimate_amount     DECIMAL(14,2) DEFAULT NULL COMMENT 'AI估算赔付金额(元)',
    final_amount        DECIMAL(14,2) DEFAULT NULL COMMENT '最终赔付金额(元)',
    assess_status       VARCHAR(32)  DEFAULT 'PROCESSING' COMMENT '定损状态 PENDING/PROCESSING/AUDIT/APPROVED/REJECTED/PAID',
    surveyor_name       VARCHAR(64)  DEFAULT NULL COMMENT '查勘员姓名',
    surveyor_phone      VARCHAR(32)  DEFAULT NULL COMMENT '查勘员电话',
    auditor_name        VARCHAR(64)  DEFAULT NULL COMMENT '审核人姓名',
    audit_time          DATETIME     DEFAULT NULL COMMENT '审核时间',
    audit_remark        VARCHAR(500) DEFAULT NULL COMMENT '审核意见',
    report_path         VARCHAR(500) DEFAULT NULL COMMENT '定损报告存储路径',
    report_no           VARCHAR(64)  DEFAULT NULL COMMENT '报告编号',
    report_time         DATETIME     DEFAULT NULL COMMENT '报告生成时间',
    remark              VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time         DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted             TINYINT      DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_mission_no (mission_no),
    INDEX idx_policy_no (policy_no),
    INDEX idx_assess_status (assess_status),
    INDEX idx_disaster_type (disaster_type),
    INDEX idx_crop_type (crop_type),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定损任务主表';

CREATE TABLE IF NOT EXISTS assess_detail (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '明细ID',
    mission_id          BIGINT       NOT NULL COMMENT '定损任务ID',
    mission_no          VARCHAR(64)  DEFAULT NULL COMMENT '定损任务编号',
    crop_type           VARCHAR(64)  NOT NULL COMMENT '作物类型',
    disaster_type       VARCHAR(32)  NOT NULL COMMENT '灾害类型',
    disaster_level      VARCHAR(32)  NOT NULL COMMENT '受灾等级 LIGHT/MODERATE/SEVERE',
    plot_area           DECIMAL(14,4) DEFAULT NULL COMMENT '地块面积(亩)',
    disaster_area       DECIMAL(14,4) DEFAULT NULL COMMENT '受灾面积(亩)',
    disaster_ratio      DECIMAL(8,4)  DEFAULT NULL COMMENT '受灾比例(%)',
    unit_yield          DECIMAL(10,2) DEFAULT NULL COMMENT '亩产标准(kg/亩)',
    unit_price          DECIMAL(10,2) DEFAULT NULL COMMENT '单价(元/kg)',
    compensate_ratio    DECIMAL(8,4)  DEFAULT NULL COMMENT '赔付比例(%)',
    disaster_coeff      DECIMAL(5,2)  DEFAULT NULL COMMENT '受灾系数',
    detail_amount       DECIMAL(14,2) DEFAULT NULL COMMENT '明细计算金额(元)',
    adjust_coeff        DECIMAL(5,2)  DEFAULT 1.00 COMMENT '调整系数',
    adjust_amount       DECIMAL(14,2) DEFAULT 0 COMMENT '调整金额(元)',
    final_amount        DECIMAL(14,2) DEFAULT NULL COMMENT '最终赔付金额(元)',
    polygon_wkt         TEXT         DEFAULT NULL COMMENT '地块多边形WKT',
    remark              VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time         DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted             TINYINT      DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_mission_id (mission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定损任务明细表';

-- ---------- 无人机飞行模块 ----------

CREATE TABLE IF NOT EXISTS drone_flight_template (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '模板ID',
    template_name       VARCHAR(100) NOT NULL COMMENT '模板名称',
    template_desc       VARCHAR(500) DEFAULT NULL COMMENT '模板描述',
    location_name       VARCHAR(200) DEFAULT NULL COMMENT '地点名称',
    center_lon          DECIMAL(15,8) DEFAULT NULL COMMENT '中心点经度',
    center_lat          DECIMAL(15,8) DEFAULT NULL COMMENT '中心点纬度',
    polygon_wkt         TEXT DEFAULT NULL COMMENT '多边形顶点WKT',
    flight_height       DECIMAL(8,2) DEFAULT 100.00 COMMENT '飞行高度(米)',
    front_overlap       DECIMAL(5,2) DEFAULT 80.00 COMMENT '航向重叠率(%)',
    side_overlap        DECIMAL(5,2) DEFAULT 60.00 COMMENT '旁向重叠率(%)',
    flight_speed        DECIMAL(8,2) DEFAULT 5.00 COMMENT '飞行速度(m/s)',
    camera_param_json   TEXT DEFAULT NULL COMMENT '相机参数JSON',
    estimated_time      DECIMAL(8,2) DEFAULT NULL COMMENT '预计飞行时间(分钟)',
    estimated_distance  DECIMAL(10,3) DEFAULT NULL COMMENT '预计航程(米)',
    estimated_area      DECIMAL(10,4) DEFAULT NULL COMMENT '预计覆盖面积(亩)',
    waypoint_count      INT DEFAULT NULL COMMENT '航点数量',
    photo_count         INT DEFAULT NULL COMMENT '预计拍摄照片数',
    estimated_battery   INT DEFAULT NULL COMMENT '预计电池消耗量(%)',
    route_plan_json     LONGTEXT DEFAULT NULL COMMENT '航线计划JSON',
    create_by           VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    create_time         DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted          TINYINT DEFAULT 0 COMMENT '逻辑删除 0否 1是',
    INDEX idx_create_by (create_by),
    INDEX idx_location (center_lon, center_lat)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='无人机飞行模板表';

CREATE TABLE IF NOT EXISTS drone_flight_task (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID',
    task_no             VARCHAR(32) NOT NULL UNIQUE COMMENT '任务编号',
    task_name           VARCHAR(100) DEFAULT NULL COMMENT '任务名称',
    template_id         BIGINT DEFAULT NULL COMMENT '关联模板ID',
    mission_id          BIGINT DEFAULT NULL COMMENT '关联定损任务ID',
    mission_no          VARCHAR(32) DEFAULT NULL COMMENT '关联定损任务编号',
    aircraft_sn         VARCHAR(100) DEFAULT NULL COMMENT '无人机序列号',
    aircraft_model      VARCHAR(100) DEFAULT NULL COMMENT '无人机型号',
    payload_model       VARCHAR(100) DEFAULT NULL COMMENT '载荷型号',
    pilot_id            BIGINT DEFAULT NULL COMMENT '飞手ID',
    pilot_name          VARCHAR(100) DEFAULT NULL COMMENT '飞手姓名',
    center_lon          DECIMAL(15,8) DEFAULT NULL COMMENT '中心点经度',
    center_lat          DECIMAL(15,8) DEFAULT NULL COMMENT '中心点纬度',
    polygon_wkt         TEXT DEFAULT NULL COMMENT '作业区域多边形WKT',
    flight_height       DECIMAL(8,2) DEFAULT NULL COMMENT '飞行高度(米)',
    front_overlap       DECIMAL(5,2) DEFAULT NULL COMMENT '航向重叠率(%)',
    side_overlap        DECIMAL(5,2) DEFAULT NULL COMMENT '旁向重叠率(%)',
    flight_speed        DECIMAL(8,2) DEFAULT NULL COMMENT '飞行速度(m/s)',
    takeoff_lon         DECIMAL(15,8) DEFAULT NULL COMMENT '起飞点经度',
    takeoff_lat         DECIMAL(15,8) DEFAULT NULL COMMENT '起飞点纬度',
    flight_status       VARCHAR(20) DEFAULT 'PENDING' COMMENT '飞行状态 PENDING/READY/FLYING/PAUSED/RETURNING/LANDING/COMPLETED/FAILED/CANCELED',
    start_time          DATETIME DEFAULT NULL COMMENT '开始时间',
    end_time            DATETIME DEFAULT NULL COMMENT '结束时间',
    actual_duration     DECIMAL(8,2) DEFAULT NULL COMMENT '实际飞行时长(分钟)',
    actual_distance     DECIMAL(10,3) DEFAULT NULL COMMENT '实际航程(米)',
    actual_photo_count  INT DEFAULT NULL COMMENT '实际拍摄照片数',
    battery_start       INT DEFAULT NULL COMMENT '起飞电池电量(%)',
    battery_end         INT DEFAULT NULL COMMENT '降落电池电量(%)',
    route_plan_json     LONGTEXT DEFAULT NULL COMMENT '航线计划JSON',
    result_json         LONGTEXT DEFAULT NULL COMMENT '飞行结果JSON',
    remark              VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_by           VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    create_time         DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted          TINYINT DEFAULT 0 COMMENT '逻辑删除 0否 1是',
    INDEX idx_task_no (task_no),
    INDEX idx_mission (mission_id),
    INDEX idx_pilot (pilot_id),
    INDEX idx_status (flight_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='无人机飞行任务表';

CREATE TABLE IF NOT EXISTS drone_flight_status (
    id                      BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '状态ID',
    task_id                 BIGINT NOT NULL COMMENT '任务ID',
    timestamp               BIGINT NOT NULL COMMENT '时间戳(毫秒)',
    aircraft_lon            DECIMAL(15,8) DEFAULT NULL COMMENT '无人机经度',
    aircraft_lat            DECIMAL(15,8) DEFAULT NULL COMMENT '无人机纬度',
    aircraft_altitude       DECIMAL(8,2) DEFAULT NULL COMMENT '相对高度(米)',
    absolute_altitude       DECIMAL(8,2) DEFAULT NULL COMMENT '绝对海拔(米)',
    speed_x                 DECIMAL(8,2) DEFAULT NULL COMMENT 'X轴速度(m/s)',
    speed_y                 DECIMAL(8,2) DEFAULT NULL COMMENT 'Y轴速度(m/s)',
    speed_z                 DECIMAL(8,2) DEFAULT NULL COMMENT 'Z轴速度(m/s)',
    ground_speed            DECIMAL(8,2) DEFAULT NULL COMMENT '对地速度(m/s)',
    heading                 DECIMAL(8,2) DEFAULT NULL COMMENT '航向角(°)',
    pitch                   DECIMAL(8,2) DEFAULT NULL COMMENT '俯仰角(°)',
    roll                    DECIMAL(8,2) DEFAULT NULL COMMENT '横滚角(°)',
    yaw                     DECIMAL(8,2) DEFAULT NULL COMMENT '偏航角(°)',
    gimbal_pitch            DECIMAL(8,2) DEFAULT NULL COMMENT '云台俯仰角(°)',
    gimbal_yaw              DECIMAL(8,2) DEFAULT NULL COMMENT '云台偏航角(°)',
    battery_percent         INT DEFAULT NULL COMMENT '电池电量(%)',
    battery_voltage         DECIMAL(8,2) DEFAULT NULL COMMENT '电池电压(V)',
    battery_current         DECIMAL(8,2) DEFAULT NULL COMMENT '电池电流(A)',
    battery_temperature     DECIMAL(8,2) DEFAULT NULL COMMENT '电池温度(°C)',
    flight_mode             VARCHAR(30) DEFAULT NULL COMMENT '飞行模式',
    current_waypoint_index  INT DEFAULT NULL COMMENT '当前航点索引',
    total_waypoints         INT DEFAULT NULL COMMENT '总航点数',
    distance_to_home        DECIMAL(10,3) DEFAULT NULL COMMENT '距返航点距离(米)',
    is_flying               TINYINT DEFAULT NULL COMMENT '是否飞行中',
    is_returning_home       TINYINT DEFAULT NULL COMMENT '是否返航中',
    is_landing              TINYINT DEFAULT NULL COMMENT '是否降落中',
    is_taking_off           TINYINT DEFAULT NULL COMMENT '是否起飞中',
    warnings                TEXT DEFAULT NULL COMMENT '警告信息',
    errors                  TEXT DEFAULT NULL COMMENT '错误信息',
    raw_json                LONGTEXT DEFAULT NULL COMMENT '原始状态JSON',
    INDEX idx_task_time (task_id, timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='无人机飞行状态表';

-- ---------- 初始化数据 ----------

-- 默认管理员用户 (密码: admin123 经过BCrypt加密)
INSERT INTO sys_user (user_name, nick_name, password, role_key, status) VALUES
('admin', '系统管理员', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 'admin', 0),
('surveyor', '查勘员小张', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 'surveyor', 0),
('auditor', '审核员小李', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 'auditor', 0)
ON DUPLICATE KEY UPDATE user_name = user_name;

-- 角色
INSERT INTO sys_role (role_name, role_key, sort, status) VALUES
('超级管理员', 'admin', 1, 0),
('查勘员', 'surveyor', 2, 0),
('审核员', 'auditor', 3, 0),
('普通用户', 'user', 4, 0)
ON DUPLICATE KEY UPDATE role_key = role_key;

-- 字典类型
INSERT INTO sys_dict (dict_name, dict_type, status) VALUES
('作物类型', 'crop_type', 0),
('灾害类型', 'disaster_type', 0),
('保险类型', 'insurance_type', 0),
('受灾等级', 'disaster_level', 0),
('影像类型', 'image_type', 0)
ON DUPLICATE KEY UPDATE dict_type = dict_type;

-- 字典数据：作物类型
INSERT INTO sys_dict_item (dict_id, dict_type, item_label, item_value, sort, status) VALUES
(1, 'crop_type', '小麦', '小麦', 1, 0),
(1, 'crop_type', '玉米', '玉米', 2, 0),
(1, 'crop_type', '水稻', '水稻', 3, 0),
(1, 'crop_type', '大豆', '大豆', 4, 0),
(1, 'crop_type', '棉花', '棉花', 5, 0),
(1, 'crop_type', '蔬菜', '蔬菜', 6, 0),
(1, 'crop_type', '水果', '水果', 7, 0),
(1, 'crop_type', '油菜', '油菜', 8, 0),
(1, 'crop_type', '花生', '花生', 9, 0),
(1, 'crop_type', '烟草', '烟草', 10, 0);

-- 字典数据：灾害类型
INSERT INTO sys_dict_item (dict_id, dict_type, item_label, item_value, sort, status) VALUES
(2, 'disaster_type', '淹水灾害', 'FLOOD', 1, 0),
(2, 'disaster_type', '倒伏灾害', 'LODGE', 2, 0),
(2, 'disaster_type', '枯黄灾害', 'WITHER', 3, 0);

-- 字典数据：受灾等级
INSERT INTO sys_dict_item (dict_id, dict_type, item_label, item_value, sort, status) VALUES
(4, 'disaster_level', '轻度受灾', 'LIGHT', 1, 0),
(4, 'disaster_level', '中度受灾', 'MODERATE', 2, 0),
(4, 'disaster_level', '重度受灾', 'SEVERE', 3, 0);

-- 字典数据：影像类型
INSERT INTO sys_dict_item (dict_id, dict_type, item_label, item_value, sort, status) VALUES
(5, 'image_type', '灾前影像', 'BEFORE', 1, 0),
(5, 'image_type', '灾后影像', 'AFTER', 2, 0),
(5, 'image_type', 'DOM正射', 'DOM', 3, 0),
(5, 'image_type', '掩膜图', 'MASK', 4, 0);

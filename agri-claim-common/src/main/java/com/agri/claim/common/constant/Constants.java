package com.agri.claim.common.constant;

public class Constants {

    public static final String UTF8 = "UTF-8";

    public static final String TOKEN_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String TOKEN_SECRET = "agri-claim-ai-secret-key-2024@#$%^&*()_+";
    public static final Long TOKEN_EXPIRE = 7200L;

    public static final String USER_ID = "userId";
    public static final String USER_NAME = "userName";
    public static final String DEPT_ID = "deptId";
    public static final String ROLE_KEY = "roleKey";

    public static final String CAPTCHA_KEY = "captcha:";
    public static final Integer CAPTCHA_EXPIRE = 120;

    public static final String REDIS_USER_KEY = "login_user:";
    public static final Integer REDIS_USER_EXPIRE = 7200;

    public static final String SUPER_ADMIN = "admin";
    public static final String SUPER_ADMIN_ROLE = "admin";

    public static final Integer ENABLE = 0;
    public static final Integer DISABLE = 1;

    public static final Integer DEFAULT_PAGE_NUM = 1;
    public static final Integer DEFAULT_PAGE_SIZE = 10;
    public static final Integer MAX_PAGE_SIZE = 1000;

    public static final String MINIO_BUCKET_IMAGE = "agri-images";
    public static final String MINIO_BUCKET_REPORT = "agri-reports";
    public static final String MINIO_BUCKET_MODEL = "agri-models";
    public static final String MINIO_BUCKET_THUMBNAIL = "agri-thumbnails";

    public static final String IMAGE_TYPE_BEFORE = "BEFORE";
    public static final String IMAGE_TYPE_AFTER = "AFTER";
    public static final String IMAGE_TYPE_DOM = "DOM";
    public static final String IMAGE_TYPE_MASK = "MASK";
    public static final String IMAGE_TYPE_THUMBNAIL = "THUMBNAIL";

    public static final String IMAGE_STATUS_UPLOADED = "UPLOADED";
    public static final String IMAGE_STATUS_PREPROCESSING = "PREPROCESSING";
    public static final String IMAGE_STATUS_PREPROCESSED = "PREPROCESSED";
    public static final String IMAGE_STATUS_FAILED = "FAILED";

    public static final String AI_STATUS_PENDING = "PENDING";
    public static final String AI_STATUS_PROCESSING = "PROCESSING";
    public static final String AI_STATUS_COMPLETED = "COMPLETED";
    public static final String AI_STATUS_FAILED = "FAILED";

    public static final String SEGMENT_CLASS_FARMLAND = "FARMLAND";
    public static final String SEGMENT_CLASS_ROAD = "ROAD";
    public static final String SEGMENT_CLASS_BUILDING = "BUILDING";
    public static final String SEGMENT_CLASS_WATER = "WATER";

    public static final String DISASTER_TYPE_FLOOD = "FLOOD";
    public static final String DISASTER_TYPE_LODGE = "LODGE";
    public static final String DISASTER_TYPE_WITHER = "WITHER";

    public static final String DISASTER_LEVEL_LIGHT = "LIGHT";
    public static final String DISASTER_LEVEL_MODERATE = "MODERATE";
    public static final String DISASTER_LEVEL_SEVERE = "SEVERE";

    public static final String ASSESS_STATUS_PENDING = "PENDING";
    public static final String ASSESS_STATUS_PROCESSING = "PROCESSING";
    public static final String ASSESS_STATUS_AUDIT = "AUDIT";
    public static final String ASSESS_STATUS_APPROVED = "APPROVED";
    public static final String ASSESS_STATUS_REJECTED = "REJECTED";
    public static final String ASSESS_STATUS_PAID = "PAID";

    public static final String MQ_TOPIC_IMAGE_UPLOAD = "IMAGE_UPLOAD_TOPIC";
    public static final String MQ_TOPIC_IMAGE_PREPROCESS = "IMAGE_PREPROCESS_TOPIC";
    public static final String MQ_TOPIC_AI_PROCESS = "AI_PROCESS_TOPIC";
    public static final String MQ_TOPIC_ASSESS_CALC = "ASSESS_CALC_TOPIC";
    public static final String MQ_TOPIC_REPORT_GENERATE = "REPORT_GENERATE_TOPIC";
    public static final String MQ_CONSUMER_GROUP_IMAGE = "IMAGE_CONSUMER_GROUP";
    public static final String MQ_CONSUMER_GROUP_AI = "AI_CONSUMER_GROUP";
    public static final String MQ_CONSUMER_GROUP_ASSESS = "ASSESS_CONSUMER_GROUP";

    public static final String DICT_TYPE_CROP = "crop_type";
    public static final String DICT_TYPE_DISASTER = "disaster_type";
    public static final String DICT_TYPE_INSURANCE = "insurance_type";
}

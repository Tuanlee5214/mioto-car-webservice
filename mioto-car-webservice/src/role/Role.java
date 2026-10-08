package role;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author tuanlee
 */
public class Role {
    // Car
    public static final String CAR_READ = "car:read";
    public static final String CAR_WRITE = "car:write";
    public static final String CAR_BRAND_READ = "car_brand:read";
    public static final String CAR_BRAND_WRITE = "car_brand:write";
    public static final String CAR_FEATURE_READ = "car_feature:read";
    public static final String CAR_FEATURE_WRITE = "car_feature:write";
    public static final String CAR_IMAGE_READ = "car_image:read";
    public static final String CAR_IMAGE_WRITE = "car_image:write";
    public static final String CAR_UNAVAIL_READ = "car_unavail:read";
    public static final String CAR_UNAVAIL_WRITE = "car_unavail:write";

    // Danh mục
    public static final String FEATURE_READ = "feature:read";
    public static final String FEATURE_WRITE = "feature:write";
    public static final String DISTRICT_READ = "district:read";
    public static final String DISTRICT_WRITE = "district:write";
    public static final String PROVINCE_READ = "province:read";
    public static final String PROVINCE_WRITE = "province:write";

    // Nghiệp vụ
    public static final String FEE_POLICY_READ = "fee_policy:read";
    public static final String FEE_POLICY_WRITE = "fee_policy:write";
    public static final String FEEDBACK_READ = "feedback:read";
    public static final String FEEDBACK_WRITE = "feedback:write";
    public static final String VOUCHER_READ = "voucher:read";
    public static final String VOUCHER_WRITE = "voucher:write";

    // Phân quyền
    public static final String ROLE_READ = "role:read";
    public static final String ROLE_WRITE = "role:write";
    public static final String USER_ROLE_READ = "user_role:read";
    public static final String USER_ROLE_WRITE = "user_role:write";
}
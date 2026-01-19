package net.lab1024.sa.common.cache.constant;

import java.time.Duration;

/**
 * 缓存Key常量
 *
 * <p>定义所有缓存名称和默认过期时间
 *
 * @author 1024创新实验室
 * @since 2025-01-19 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
public final class CacheKeyConst {

  private CacheKeyConst() {}

  /** 默认缓存过期时间配置 */
  public static final class Expire {
    /** 短期缓存：5分钟 */
    public static final Duration SHORT = Duration.ofMinutes(5);

    /** 中期缓存：30分钟 */
    public static final Duration MEDIUM = Duration.ofMinutes(30);

    /** 长期缓存：2小时 */
    public static final Duration LONG = Duration.ofHours(2);

    /** 持久缓存：24小时 */
    public static final Duration PERSISTENT = Duration.ofHours(24);

    private Expire() {}
  }

  /** 字典相关缓存 */
  public static final class Dict {
    /** 字典数据缓存 */
    public static final String DICT_DATA = "dict_data_cache";

    private Dict() {}
  }

  /** 部门相关缓存 */
  public static final class Department {
    /** 部门列表 */
    public static final String DEPARTMENT_LIST_CACHE = "department_list_cache";

    /** 部门树 */
    public static final String DEPARTMENT_TREE_CACHE = "department_tree_cache";

    /** 某个部门以及下级的id列表 */
    public static final String DEPARTMENT_SELF_CHILDREN_CACHE = "department_self_children_cache";

    /** 部门路径缓存 */
    public static final String DEPARTMENT_PATH_CACHE = "department_path_cache";

    private Department() {}
  }

  /** 分类相关缓存 */
  public static final class Category {
    /** 分类实体缓存 */
    public static final String CATEGORY_ENTITY = "category_cache";

    /** 分类子级缓存 */
    public static final String CATEGORY_SUB = "category_sub_cache";

    /** 分类树缓存 */
    public static final String CATEGORY_TREE = "category_tree_cache";

    private Category() {}
  }

  /** 登录相关缓存 */
  public static final class Login {
    /** 请求用户信息 */
    public static final String REQUEST_EMPLOYEE = "login_request_employee";

    /** 请求用户权限 */
    public static final String USER_PERMISSION = "login_user_permission";

    private Login() {}
  }
}

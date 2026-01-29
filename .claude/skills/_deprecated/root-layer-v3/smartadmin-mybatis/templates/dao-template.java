package {{package_name}}.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import {{package_name}}.domain.entity.{{entity_class_name}};
import {{package_name}}.domain.form.{{query_form_class_name}};
import {{package_name}}.domain.vo.{{vo_class_name}};
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * {{table_comment}} Dao
 *
 * @author SmartAdmin MyBatis Generator
 * @since {{current_date}}
 */
@Mapper
public interface {{dao_class_name}} extends BaseMapper<{{entity_class_name}}> {

    /**
     * 分页查询{{table_comment}}列表
     *
     * @param page 分页参数
     * @param queryForm 查询条件
     * @return {{table_comment}}列表
     */
    List<{{vo_class_name}}> query{{business_name}}(
        Page<?> page,
        @Param("queryForm") {{query_form_class_name}} queryForm);

{{#if has_unique_field}}
    /**
     * 根据{{unique_field_comment}}查询
     *
     * @param {{unique_field_name}} {{unique_field_comment}}
     * @return {{table_comment}}实体
     */
    {{entity_class_name}} getBy{{unique_field_name_pascal}}(@Param("{{unique_field_name}}") {{unique_field_type}} {{unique_field_name}});

{{/if}}
{{#if has_deleted_flag}}
    /**
     * 逻辑删除
     *
     * @param {{id_field_name}} 主键ID
     * @return 影响行数
     */
    Integer logicDelete(@Param("{{id_field_name}}") {{id_type}} {{id_field_name}});

{{/if}}
}

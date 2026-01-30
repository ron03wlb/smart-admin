package {{package_name}}.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
{{#if has_table_logic}}
import com.baomidou.mybatisplus.annotation.TableLogic;
{{/if}}
import java.time.LocalDateTime;
{{#if has_big_decimal}}
import java.math.BigDecimal;
{{/if}}
import lombok.Data;

/**
 * {{table_comment}} 实体表
 *
 * @author SmartAdmin MyBatis Generator
 * @since {{current_date}}
 */
@Data
@TableName("{{table_name}}")
public class {{entity_class_name}} {

    @TableId(type = IdType.AUTO)
    private {{id_type}} {{id_field_name}};

{{#each fields}}
    /** {{field_comment}} */
{{#if is_table_logic}}
    @TableLogic
{{/if}}
    private {{field_type}} {{field_name}};

{{/each}}
    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

package {{package_name}}.domain.form;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.media.Schema;
{{#if has_validation}}
import jakarta.validation.constraints.Size;
{{/if}}
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.core.domain.request.PageParam;
import org.hibernate.validator.constraints.Length;
{{#if has_list_field}}
import java.util.List;
{{/if}}

/**
 * {{table_comment}} 查询表单
 *
 * @author SmartAdmin MyBatis Generator
 * @since {{current_date}}
 */
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
@EqualsAndHashCode(callSuper = true)
@Data
public class {{query_form_class_name}} extends PageParam {

{{#each query_fields}}
    @Schema(description = "{{field_comment}}"{{#if is_hidden}}, hidden = true{{/if}})
{{#if has_length_validation}}
    @Length(max = {{max_length}}, message = "{{field_comment}}最多{{max_length}}字符")
{{/if}}
{{#if is_list}}
    @Size(max = 99, message = "最多查询99条")
{{/if}}
    private {{field_type}} {{field_name}};

{{/each}}
{{#if has_deleted_flag}}
    @Schema(description = "删除标识", hidden = true)
    private Boolean deletedFlag;
{{/if}}
}

package com.divination.liuyao.pojo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import lombok.Data;

@Data
public class GeneratePromptRequest {

    @NotNull(message = "castTime不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime castTime;

    @NotBlank(message = "卦象数字不能为空")
    @Pattern(regexp = "^[0-3]{6}$", message = "卦象数字格式错误，必须是6位且每位为0-3")
    private String number;

    @NotBlank(message = "问题不能为空")
    private String question;

    @NotBlank(message = "背景不能为空")
    private String background;
}

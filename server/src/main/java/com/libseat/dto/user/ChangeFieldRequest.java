package com.libseat.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ChangeFieldRequest {

    /** 可申请修改的字段：userNo、realName、department */
    @NotBlank
    @Size(max = 50)
    private String fieldName;

    @NotBlank
    @Size(max = 255)
    private String newValue;
}

package com.libseat.dto.admin;

import com.libseat.entity.UserRole;
import com.libseat.entity.UserStatus;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UpdateUserRequest {

    private UserRole role;
    private UserStatus status;

    @Size(min = 8, max = 100)
    private String newPassword;

    /** true 时清零爽约计数 */
    private Boolean resetNoShowCount;
}

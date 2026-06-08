package com.libseat.dto.admin;

import com.libseat.entity.ChangeRequestStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class HandleChangeRequestRequest {

    /** APPROVED 或 REJECTED */
    @NotNull
    private ChangeRequestStatus action;

    @Size(max = 255)
    private String handleNote;
}

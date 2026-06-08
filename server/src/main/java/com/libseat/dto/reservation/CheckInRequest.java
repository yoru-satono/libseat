package com.libseat.dto.reservation;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CheckInRequest {

    @NotBlank
    private String qrToken;
}

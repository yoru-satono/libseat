package com.libseat.dto.reservation;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@NoArgsConstructor
public class RenewRequest {

    @NotNull
    private LocalTime newEndTime;
}

package com.libseat.dto.reservation;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancelReservationRequest {
    @Size(max = 200)
    private String cancelReason;
}

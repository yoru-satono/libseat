package com.libseat.dto.seat;

import com.libseat.entity.SeatArea;
import com.libseat.entity.SeatStatus;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class UpdateSeatRequest {

    @Size(max = 20)
    private String seatNo;

    private Short floor;
    private SeatArea area;
    private Boolean hasComputer;
    private Boolean hasPower;
    private Boolean hasWindow;
    private SeatStatus status;
    private BigDecimal posX;
    private BigDecimal posY;
}

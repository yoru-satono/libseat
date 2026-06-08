package com.libseat.dto.seat;

import com.libseat.entity.SeatArea;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
public class CreateSeatRequest {

    @NotNull
    private UUID libraryId;

    @NotBlank
    @Size(max = 20)
    private String seatNo;

    @NotNull
    private Short floor;

    @NotNull
    private SeatArea area;

    private Boolean hasComputer = false;
    private Boolean hasPower = false;
    private Boolean hasWindow = false;

    private BigDecimal posX;
    private BigDecimal posY;
}

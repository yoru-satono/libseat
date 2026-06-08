package com.libseat.dto.library;

import java.time.OffsetDateTime;
import java.util.UUID;

public record LibraryResponse(
        UUID id,
        String name,
        String address,
        String logoUrl,
        OffsetDateTime createdAt
) {}

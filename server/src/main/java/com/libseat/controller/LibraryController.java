package com.libseat.controller;

import com.libseat.common.Result;
import com.libseat.dto.library.LibraryResponse;
import com.libseat.service.LibraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/libraries")
@RequiredArgsConstructor
public class LibraryController {

    private final LibraryService libraryService;

    @GetMapping
    public Result<List<LibraryResponse>> list() {
        return Result.success(libraryService.listLibraries());
    }

    @GetMapping("/{id}")
    public Result<LibraryResponse> get(@PathVariable UUID id) {
        return Result.success(libraryService.getLibrary(id));
    }
}

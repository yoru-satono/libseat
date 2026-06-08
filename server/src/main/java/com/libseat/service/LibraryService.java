package com.libseat.service;

import com.libseat.common.ErrorCode;
import com.libseat.dto.library.CreateLibraryRequest;
import com.libseat.dto.library.LibraryResponse;
import com.libseat.dto.library.UpdateLibraryRequest;
import com.libseat.entity.Library;
import com.libseat.exception.BusinessException;
import com.libseat.repository.LibraryRepository;
import com.libseat.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LibraryService {

    private final LibraryRepository libraryRepository;
    private final SeatRepository seatRepository;

    public List<LibraryResponse> listLibraries() {
        return libraryRepository.findAll().stream()
                .map(LibraryService::toResponse)
                .toList();
    }

    public LibraryResponse getLibrary(UUID id) {
        return toResponse(findLibrary(id));
    }

    @Transactional
    public LibraryResponse createLibrary(CreateLibraryRequest req) {
        if (libraryRepository.existsByName(req.getName())) {
            throw new BusinessException(ErrorCode.LIBRARY_NAME_DUPLICATE);
        }
        Library lib = new Library();
        lib.setName(req.getName());
        lib.setAddress(req.getAddress());
        lib.setLogoUrl(req.getLogoUrl());
        return toResponse(libraryRepository.save(lib));
    }

    @Transactional
    public LibraryResponse updateLibrary(UUID id, UpdateLibraryRequest req) {
        Library lib = findLibrary(id);
        if (req.getName() != null && !req.getName().equals(lib.getName())) {
            if (libraryRepository.existsByName(req.getName())) {
                throw new BusinessException(ErrorCode.LIBRARY_NAME_DUPLICATE);
            }
            lib.setName(req.getName());
        }
        if (req.getAddress() != null) lib.setAddress(req.getAddress());
        if (req.getLogoUrl() != null)  lib.setLogoUrl(req.getLogoUrl());
        return toResponse(lib);
    }

    @Transactional
    public void deleteLibrary(UUID id) {
        Library lib = findLibrary(id);
        if (seatRepository.existsByLibraryId(id)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "该图书馆下仍有座位，无法删除");
        }
        libraryRepository.delete(lib);
    }

    private Library findLibrary(UUID id) {
        return libraryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "图书馆不存在"));
    }

    static LibraryResponse toResponse(Library lib) {
        return new LibraryResponse(
                lib.getId(), lib.getName(), lib.getAddress(), lib.getLogoUrl(), lib.getCreatedAt());
    }
}

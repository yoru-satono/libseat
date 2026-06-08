package com.libseat.service;

import com.libseat.common.ErrorCode;
import com.libseat.dto.library.CreateLibraryRequest;
import com.libseat.dto.library.LibraryResponse;
import com.libseat.dto.library.UpdateLibraryRequest;
import com.libseat.entity.Library;
import com.libseat.exception.BusinessException;
import com.libseat.repository.LibraryRepository;
import com.libseat.repository.SeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LibraryServiceTest {

    @Mock LibraryRepository libraryRepository;
    @Mock SeatRepository seatRepository;
    @InjectMocks LibraryService libraryService;

    private UUID libraryId;
    private Library library;

    @BeforeEach
    void setUp() {
        libraryId = UUID.randomUUID();
        library = new Library();
        library.setId(libraryId);
        library.setName("总馆");
        library.setAddress("校园路1号");
    }

    // ── listLibraries ─────────────────────────────────────────────────────

    @Test
    void listLibraries_returnsAll() {
        when(libraryRepository.findAll()).thenReturn(List.of(library));

        List<LibraryResponse> result = libraryService.listLibraries();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("总馆");
    }

    // ── getLibrary ────────────────────────────────────────────────────────

    @Test
    void getLibrary_success() {
        when(libraryRepository.findById(libraryId)).thenReturn(Optional.of(library));

        LibraryResponse resp = libraryService.getLibrary(libraryId);

        assertThat(resp.id()).isEqualTo(libraryId);
    }

    @Test
    void getLibrary_notFound_throws() {
        when(libraryRepository.findById(libraryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> libraryService.getLibrary(libraryId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);
    }

    // ── createLibrary ─────────────────────────────────────────────────────

    @Test
    void createLibrary_success() {
        when(libraryRepository.existsByName("新馆")).thenReturn(false);
        when(libraryRepository.save(any())).thenAnswer(i -> {
            Library lib = i.getArgument(0);
            lib.setId(UUID.randomUUID());
            return lib;
        });

        CreateLibraryRequest req = new CreateLibraryRequest();
        req.setName("新馆"); req.setAddress("东区");

        LibraryResponse resp = libraryService.createLibrary(req);

        assertThat(resp.name()).isEqualTo("新馆");
        verify(libraryRepository).save(any(Library.class));
    }

    @Test
    void createLibrary_duplicateName_throws() {
        when(libraryRepository.existsByName("总馆")).thenReturn(true);

        CreateLibraryRequest req = new CreateLibraryRequest();
        req.setName("总馆");

        assertThatThrownBy(() -> libraryService.createLibrary(req))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LIBRARY_NAME_DUPLICATE);
    }

    // ── updateLibrary ─────────────────────────────────────────────────────

    @Test
    void updateLibrary_nameChanged_success() {
        when(libraryRepository.findById(libraryId)).thenReturn(Optional.of(library));
        when(libraryRepository.existsByName("分馆")).thenReturn(false);

        UpdateLibraryRequest req = new UpdateLibraryRequest();
        req.setName("分馆");

        LibraryResponse resp = libraryService.updateLibrary(libraryId, req);

        assertThat(resp.name()).isEqualTo("分馆");
    }

    @Test
    void updateLibrary_sameNameNotChecked() {
        when(libraryRepository.findById(libraryId)).thenReturn(Optional.of(library));

        UpdateLibraryRequest req = new UpdateLibraryRequest();
        req.setName("总馆"); // 名称不变，不需要重复性校验

        libraryService.updateLibrary(libraryId, req);

        verify(libraryRepository, never()).existsByName(any());
    }

    @Test
    void updateLibrary_duplicateName_throws() {
        when(libraryRepository.findById(libraryId)).thenReturn(Optional.of(library));
        when(libraryRepository.existsByName("分馆")).thenReturn(true);

        UpdateLibraryRequest req = new UpdateLibraryRequest();
        req.setName("分馆");

        assertThatThrownBy(() -> libraryService.updateLibrary(libraryId, req))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LIBRARY_NAME_DUPLICATE);
    }

    // ── deleteLibrary ─────────────────────────────────────────────────────

    @Test
    void deleteLibrary_success() {
        when(libraryRepository.findById(libraryId)).thenReturn(Optional.of(library));
        when(seatRepository.existsByLibraryId(libraryId)).thenReturn(false);

        libraryService.deleteLibrary(libraryId);

        verify(libraryRepository).delete(library);
    }

    @Test
    void deleteLibrary_hasSeats_throws() {
        when(libraryRepository.findById(libraryId)).thenReturn(Optional.of(library));
        when(seatRepository.existsByLibraryId(libraryId)).thenReturn(true);

        assertThatThrownBy(() -> libraryService.deleteLibrary(libraryId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PARAM_INVALID);
    }

    @Test
    void deleteLibrary_notFound_throws() {
        when(libraryRepository.findById(libraryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> libraryService.deleteLibrary(libraryId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);
    }
}

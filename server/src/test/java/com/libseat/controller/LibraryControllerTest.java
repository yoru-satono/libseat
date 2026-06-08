package com.libseat.controller;

import com.libseat.config.SecurityConfig;
import com.libseat.dto.library.LibraryResponse;
import com.libseat.repository.UserRepository;
import com.libseat.security.JwtAuthenticationFilter;
import com.libseat.service.JwtService;
import com.libseat.service.LibraryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LibraryController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class LibraryControllerTest extends ControllerTestBase {

    @Autowired MockMvc mockMvc;

    @MockitoBean LibraryService libraryService;
    @MockitoBean JwtService jwtService;
    @MockitoBean UserRepository userRepository;

    private static final UUID LIB_ID = UUID.randomUUID();

    @Test
    void list_anonymous_returnsLibraries() throws Exception {
        when(libraryService.listLibraries()).thenReturn(List.of(
                new LibraryResponse(LIB_ID, "总馆", "校园路1号", null, OffsetDateTime.now())));

        mockMvc.perform(get("/v1/libraries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data[0].name").value("总馆"));
    }

    @Test
    void get_anonymous_returnsLibrary() throws Exception {
        when(libraryService.getLibrary(LIB_ID)).thenReturn(
                new LibraryResponse(LIB_ID, "总馆", "校园路1号", null, OffsetDateTime.now()));

        mockMvc.perform(get("/v1/libraries/{id}", LIB_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.name").value("总馆"));
    }
}

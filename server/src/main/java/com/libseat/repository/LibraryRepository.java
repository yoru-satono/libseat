package com.libseat.repository;

import com.libseat.entity.Library;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LibraryRepository extends JpaRepository<Library, UUID> {

    boolean existsByName(String name);
}

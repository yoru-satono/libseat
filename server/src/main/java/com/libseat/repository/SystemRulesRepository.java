package com.libseat.repository;

import com.libseat.entity.SystemRules;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SystemRulesRepository extends JpaRepository<SystemRules, Long> {

    /** 全局默认规则（library 为 null） */
    Optional<SystemRules> findByLibraryIsNull();

    /** 指定图书馆的覆盖规则 */
    Optional<SystemRules> findByLibraryId(UUID libraryId);
}

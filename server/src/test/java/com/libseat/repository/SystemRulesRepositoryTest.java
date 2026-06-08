package com.libseat.repository;

import com.libseat.entity.Library;
import com.libseat.entity.SystemRules;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SystemRulesRepositoryTest extends RepositoryTestBase {

    @Autowired SystemRulesRepository systemRulesRepository;
    @Autowired LibraryRepository     libraryRepository;

    @Test
    void findByLibraryIsNull_returnsGlobalRule() {
        // 01_schema.sql 中已 INSERT 一条全局规则
        assertThat(systemRulesRepository.findByLibraryIsNull()).isPresent();
    }

    @Test
    void findByLibraryId_libraryRuleExists_returnsRule() {
        Library lib = libraryRepository.save(library());
        SystemRules rule = new SystemRules();
        rule.setLibrary(lib);
        rule.setAdvanceDaysMax((short) 3);
        systemRulesRepository.save(rule);

        assertThat(systemRulesRepository.findByLibraryId(lib.getId()))
                .isPresent()
                .hasValueSatisfying(r -> assertThat(r.getAdvanceDaysMax()).isEqualTo((short) 3));
    }

    @Test
    void findByLibraryId_noLibraryRule_returnsEmpty() {
        assertThat(systemRulesRepository.findByLibraryId(UUID.randomUUID())).isEmpty();
    }

    private Library library() {
        Library l = new Library();
        l.setName("分馆");
        l.setAddress("测试路");
        return l;
    }
}

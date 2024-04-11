package com.example.WebQuizService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface CompletionRepository extends PagingAndSortingRepository<Completion, Long>, JpaRepository<Completion, Long> {
    Page<Completion> findCompletionsByUser(AppUser user, PageRequest page);
}

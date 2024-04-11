package com.example.WebQuizService;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;

@Repository
public interface QuizRepository extends JpaRepository<Quiz,Long>, PagingAndSortingRepository<Quiz,Long> {
}

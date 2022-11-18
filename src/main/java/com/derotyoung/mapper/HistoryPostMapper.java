package com.derotyoung.mapper;

import com.derotyoung.entity.HistoryPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoryPostMapper extends JpaRepository<HistoryPost, Long> {

    @Query(value = "FROM HistoryPost WHERE userId = ?1 ORDER BY id DESC")
    List<HistoryPost> findAllByUserId(String userId);

}

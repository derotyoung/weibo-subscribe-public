package com.derotyoung.mapper;

import com.derotyoung.entity.HistoryPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface HistoryPostMapper extends JpaRepository<HistoryPost, Long>, JpaSpecificationExecutor<HistoryPost> {

}

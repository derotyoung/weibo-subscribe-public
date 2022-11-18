package com.derotyoung.weibosubscribe;

import com.derotyoung.entity.HistoryPost;
import com.derotyoung.mapper.HistoryPostMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class DatabaseTest {

    @Autowired
    private HistoryPostMapper historyPostMapper;

    @Test
    void testQuery() {
        List<HistoryPost> list = historyPostMapper.findAllByUserId("1");
    }

    @Test
    void testSave() {

        HistoryPost post1 = new HistoryPost();
        post1.setUserId("1");
        post1.setPostId("1");

        HistoryPost post2 = new HistoryPost();
        post2.setUserId("2");
        post2.setPostId("2");

        historyPostMapper.saveAll(List.of(post1, post2));
    }
}

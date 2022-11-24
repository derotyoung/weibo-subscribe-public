package com.derotyoung.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.derotyoung.config.WeiboSubscribe;
import com.derotyoung.dto.WeiboPost;
import com.derotyoung.entity.HistoryPost;
import com.derotyoung.repository.HistoryPostRepository;
import com.derotyoung.repository.UserSubscribeRepository;
import com.derotyoung.util.OkHttpClientUtil;
import com.derotyoung.util.PostUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PostService {

    private final static Logger logger = LoggerFactory.getLogger(PostService.class);

    @Resource
    private UserSubscribeRepository userSubscribeRepository;

    @Resource
    private HistoryPostRepository historyPostRepository;

    @Resource
    private WeiboSubscribe weiboSubscribe;

    @Resource
    private MessageService messageService;

    public void run() {
        List<String> userIds = userSubscribeRepository.getAllUserId(true);
        if (CollectionUtils.isEmpty(userIds)) {
            return;
        }
        List<WeiboPost> postList = new ArrayList<>();
        for (String userId : userIds) {
            JSONArray cards = PostUtil.getUserIndex(userId);
            if (CollectionUtils.isEmpty(cards)) {
                continue;
            }
            Map<String, List<HistoryPost>> postMap = historyPostRepository.getPostMapByUserId(userId);
            for (int i = 0; i < cards.size(); i++) {
                // 每次只取最新的4条博文
                if (i >= 4) {
                    break;
                }
                JSONObject card = cards.getJSONObject(i);
                JSONObject mblog = card.getJSONObject("mblog");
                String id = mblog.getString("id");

                // 错误文章发送失败3次后不再发送
                if (PostUtil.countErrorId(id) >= 3) {
                    continue;
                }

                // 文章已发送不再发送
                List<HistoryPost> historyPosts = postMap.get(id);
                boolean haveSent = PostUtil.haveSent(mblog, historyPosts);
                if (haveSent) {
                    continue;
                }

                WeiboPost weiboPost = PostUtil.getWeiboPost(mblog);
                if (weiboPost != null) {
                    postList.add(weiboPost);
                }
            }
        }
        // 发送消息
        messageService.sendMessageBatch(postList);
    }

    public void testWeiboUserId() {
        List<String> userIds = userSubscribeRepository.getAllUserId(false);
        if (CollectionUtils.isEmpty(userIds)) {
            return;
        }
        String baseUrl = "https://m.weibo.cn/api/container/getIndex";
        for (String userId : userIds) {
            Map<String, String> paramsMap = Map.of("containerid", "100505" + userId);
            String url = OkHttpClientUtil.getRequestUrl(baseUrl, paramsMap);
            String reqBody;
            try {
                reqBody = OkHttpClientUtil.requestGetWithSleep(url);
            } catch (Exception ignored) {
                continue;
            }
            if (PostUtil.strIsNotJson(reqBody)) {
                continue;
            }
            JSONObject result = JSON.parseObject(reqBody);
            JSONObject data = result.getJSONObject("data");
            if (data == null) {
                continue;
            }
            JSONObject userInfo = data.getJSONObject("userInfo");
            if (userInfo != null) {
                String screenName = userInfo.getString("screen_name");
                if (StringUtils.hasText(screenName)) {
                    logger.info("当前设置的微博账户为@{}({}})", screenName, userId);
                }
            }
        }
    }

    public void testProxy() {
        String proxy = weiboSubscribe.getProxy();
        if (StringUtils.hasText(proxy)) {
            try {
                int code = OkHttpClientUtil.requestGet("https://www.google.com", proxy);
                if (code == 200) {
                    logger.info("代理配置正确，可正常访问");
                }
            } catch (IOException e) {
                throw new RuntimeException("代理无法访问到电报服务器");
            }
        } else {
            logger.info("未配置代理");

        }
    }

}
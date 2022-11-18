package com.derotyoung.service;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ReUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.derotyoung.dto.Media;
import com.derotyoung.dto.MediaPhoto;
import com.derotyoung.dto.MediaVideo;
import com.derotyoung.dto.WeiboPost;
import com.derotyoung.entity.HistoryPost;
import com.derotyoung.enums.MonthEnum;
import com.derotyoung.properties.WeiboSubscribeProperties;
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
import java.math.BigDecimal;
import java.time.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class SearchService {

    private final static Logger logger = LoggerFactory.getLogger(SearchService.class);

    @Resource
    private UserSubscribeRepository userSubscribeRepository;

    @Resource
    private HistoryPostRepository historyPostRepository;

    @Resource
    private WeiboSubscribeProperties weiboSubscribeProperties;

    @Resource
    private MessageService messageService;

    public void run() {
        List<String> userIds = userSubscribeRepository.getAllUserId(true);
        if (CollectionUtils.isEmpty(userIds)) {
            return;
        }
        String baseUrl = "https://m.weibo.cn/api/container/getIndex";
        List<WeiboPost> postList = new ArrayList<>();
        for (String userId : userIds) {
            String url = OkHttpClientUtil.getRequestUrl(baseUrl, Map.of("containerid", ("107603" + userId)));
            String reqBody;
            try {
                reqBody = OkHttpClientUtil.requestGetWithSleep(url);
            } catch (Exception ignored) {
                logger.error("查询微博错误,url={}", url);
                continue;
            }
            if (strIsNotJson(reqBody)) {
                continue;
            }
            JSONObject entries = JSON.parseObject(reqBody);
            JSONObject data = entries.getJSONObject("data");
            Map<String, List<HistoryPost>> postMap = historyPostRepository.getPostMapByUserId(userId);
            JSONArray cards = data.getJSONArray("cards");
            for (int i = 0; i < cards.size(); i++) {
                // 每次只取最新的4条博文
                if (i >= 4) {
                    break;
                }
                JSONObject card = cards.getJSONObject(i);
                JSONObject mblog = card.getJSONObject("mblog");
                String id = mblog.getString("id");

                // 文章已发送不再发送
                List<HistoryPost> historyPosts = postMap.get(id);
                boolean haveSent = haveSent(mblog, historyPosts);
                if (haveSent) {
                    continue;
                }

                WeiboPost weiboPost = getWeiboPost(mblog);
                if (weiboPost != null) {
                    postList.add(weiboPost);
                }
            }
        }
        // 发送消息
        messageService.sendMessageBatch(postList);
    }

    public boolean haveSent(JSONObject mblog, List<HistoryPost> historyPosts) {
        if (!CollectionUtils.isEmpty(historyPosts)) {
            historyPosts.sort(Comparator.comparing(HistoryPost::getId).reversed());
            HistoryPost lastHisPost = historyPosts.get(0);
            LocalDateTime createdAt = parseDate(mblog.getString("created_at"));
            LocalDateTime editAt = parseDate(mblog.getString("edit_at"));
            if (lastHisPost.getEditAt() == null) {
                return editAt == null && lastHisPost.getCreatedAt().equals(createdAt);
            } else {
                // 已编辑
                return lastHisPost.getEditAt().equals(editAt);
            }
        }
        return false;
    }

    public WeiboPost getWeiboPost(JSONObject mblog) {
        boolean isLongText = mblog.getBooleanValue("isLongText");
        int topFlag = isTopPost(mblog) ? 1 : 0;
        WeiboPost weiboPost;
        if (isLongText) {
            weiboPost = getWeiboLongText(mblog);
        } else {
            weiboPost = parseToWeiboPost(mblog);
        }
        if (weiboPost != null) {
            weiboPost.setTopFlag(topFlag);
        }
        return weiboPost;
    }

    public boolean isTopPost(JSONObject mblog) {
        JSONObject title = mblog.getJSONObject("title");
        if (title != null) {
            return "置顶".equals(title.getString("text"));
        }
        return false;
    }

    public WeiboPost parseToWeiboPost(JSONObject mblog) {
        String id = mblog.getString("id");
        String bid = mblog.getString("bid");
        String text0 = mblog.getString("text");
        JSONObject user = mblog.getJSONObject("user");
        String userId = user.getString("id");
        String nickname = user.getString("screen_name");
        String source = mblog.getString("source");
        String createdAt = mblog.getString("created_at");
        String editAt = mblog.getString("edit_at");

        WeiboPost weiboPost = new WeiboPost();
        weiboPost.setId(id);
        weiboPost.setUserId(userId);
        weiboPost.setNickname(nickname);
        weiboPost.setSource(source);
        weiboPost.setLink(PostUtil.getPcPostUrl(userId, bid));
        weiboPost.setCreatedAt(parseDate(createdAt));
        weiboPost.setEditAt(parseDate(editAt));

        // retweet 不为空表示转发微博
        JSONObject retweet = mblog.getJSONObject("retweeted_status");
        if (retweet != null) {
            String appendText;
            WeiboPost retweetPost = getWeiboPost(retweet);
            if (retweetPost != null && StringUtils.hasText(retweetPost.getText())) {
                appendText = "\n\n//@" + retweetPost.getNickname() + "\n" + retweetPost.getText();
            } else {
                appendText = "\n//转发原文不可见，可能无法查看或已被删除";
            }
            String retweetText = PostUtil.beautifyRetweetText(text0);
            weiboPost.setText(retweetText + appendText);
        } else {
            weiboPost.setText(PostUtil.beautifyText(text0));
        }

        // 图片
        JSONArray pics = mblog.getJSONArray("pics");
        List<Media> mediaList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(pics)) {
            for (int i = 0; i < pics.size(); i++) {
                JSONObject picJSON = pics.getJSONObject(i);
                String thumb = picJSON.getString("url");
                String media = picJSON.getJSONObject("large").getString("url");
                MediaPhoto mediaPhoto = new MediaPhoto(media, thumb);
                mediaList.add(mediaPhoto);
            }
        }

        // 视频
        JSONObject pageInfo = mblog.getJSONObject("page_info");
        if (pageInfo != null) {
            JSONObject urls = pageInfo.getJSONObject("urls");
            if (urls != null) {
                List<String> urlList = urls.values().stream().map(Object::toString).sorted(Comparator.comparing(i -> {
                    String template0 = ReUtil.getGroup0("&template.*?&", String.valueOf(i));
                    String template = template0.replace("template=", "").replace("&", "");
                    String[] xes = template.split("x");
                    if (NumberUtil.isNumber(xes[0])) {
                        return new BigDecimal(xes[0]);
                    }
                    return BigDecimal.ZERO;
                }).reversed()).toList();
                MediaVideo mediaVideo = new MediaVideo(urlList.get(0));
                mediaVideo.setThumb(urlList.get(urlList.size() - 1));
                mediaList.add(mediaVideo);
            }
        }

        weiboPost.setMediaList(mediaList);

        return weiboPost;
    }

    public WeiboPost getWeiboLongText(JSONObject mblog) {
        String bid = mblog.getString("bid");
        String url = "https://m.weibo.cn/statuses/show?id=" + bid;
        String reqBody;
        try {
            reqBody = OkHttpClientUtil.requestGetWithSleep(url);
        } catch (Exception e) {
            logger.error("无法获取微博长文,url={}", url, e);
            return null;
        }
        // 无法查看的微博
        if (strIsNotJson(reqBody)) {
            logger.warn("您所访问的内容因版权问题不适合展示,postId={},url={}", mblog.getString("id"), url);
            return null;
        }
        JSONObject detail = JSON.parseObject(reqBody);
        JSONObject mblog2 = detail.getJSONObject("data");
        return parseToWeiboPost(mblog2);
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
            if (strIsNotJson(reqBody)) {
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
        String proxy = weiboSubscribeProperties.getProxy();
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

    /**
     * 解析时间字符串
     *
     * @param str "Sat Apr 13 16:50:18 +0800 2019"
     * @return LocalDateTime
     */
    public LocalDateTime parseDate(String str) {
        if (StringUtils.hasText(str)) {
            String[] strings = str.split(" ");
            if (strings.length == 6) {
                int year = Integer.parseInt(strings[strings.length - 1]);
                int month = MonthEnum.valueFrom(strings[1]);
                int day = Integer.parseInt(strings[2]);
                LocalTime localTime = LocalTime.parse(strings[3]);
                LocalDateTime ldt = LocalDateTime.of(LocalDate.of(year, month, day), localTime);
                ZoneId zoneId = ZoneId.of("UTC" + strings[4]);
                ZonedDateTime zonedDateTime = ldt.atZone(zoneId);
                return LocalDateTime.ofInstant(zonedDateTime.toInstant(), zoneId);
            }
        }

        return null;
    }

    public boolean strIsJson(String str) {
        if (StringUtils.hasLength(str)) {
            return str.startsWith("{") || str.startsWith("[");
        }
        return false;
    }

    private boolean strIsNotJson(String str) {
        return !strIsJson(str);
    }
}


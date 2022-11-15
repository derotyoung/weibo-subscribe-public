package com.derotyoung.service;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ReUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.derotyoung.dto.WeiboPost;
import com.derotyoung.entity.HistoryPost;
import com.derotyoung.enums.MonthEnum;
import com.derotyoung.properties.WeiboSubscribeProperties;
import com.derotyoung.repository.HistoryPostRepository;
import com.derotyoung.repository.UserSubscribeRepository;
import com.derotyoung.util.OkHttpClientUtil;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class SearchService {

    private final static Logger logger = LoggerFactory.getLogger(SearchService.class);

    @Autowired
    private UserSubscribeRepository userSubscribeRepository;

    @Autowired
    private HistoryPostRepository historyPostRepository;

    @Autowired
    private WeiboSubscribeProperties weiboSubscribeProperties;

    @Autowired
    private TelegramBot telegramBot;

    public void run() {
        List<String> userIds = userSubscribeRepository.getAllUserId(true);
        if (CollectionUtils.isEmpty(userIds)) {
            return;
        }
        String url = "https://m.weibo.cn/api/container/getIndex";
        List<WeiboPost> postList = new ArrayList<>();
        for (String userId : userIds) {
            Map<String, String> paramsMap = Collections.singletonMap("containerid", "107603" + userId);
            String reqBody;
            try {
                reqBody = OkHttpClientUtil.requestGet(url, paramsMap);
            } catch (Exception ignored) {
                logger.error("查询微博错误,url={}", OkHttpClientUtil.getRequestUrl(url, paramsMap));
                continue;
            }
            if (!stringIsJson(reqBody)) {
                continue;
            }
            JSONObject entries = JSON.parseObject(reqBody);
            JSONObject data = entries.getJSONObject("data");
            Map<String, List<HistoryPost>> postMap = historyPostRepository.getPostMapByUserId(userId);
            JSONArray cards = data.getJSONArray("cards");
            for (int i = 0; i < cards.size(); i++) {
                // 每3分钟执行一次，只取最新的5条博文
                if (i > 4) {
                    break;
                }
                JSONObject card = cards.getJSONObject(i);
                JSONObject mblog = card.getJSONObject("mblog");
                String id = mblog.getString("id");

                // 文章已发送不再发送
                List<HistoryPost> historyPosts = postMap.get(id);
                if (!CollectionUtils.isEmpty(historyPosts)) {
                    historyPosts.sort(Comparator.comparing(HistoryPost::getId).reversed());
                    HistoryPost lastHisPost = historyPosts.get(0);
                    LocalDateTime createdAt = parseDate(mblog.getString("created_at"));
                    LocalDateTime editAt = parseDate(mblog.getString("edit_at"));
                    if (lastHisPost.getEditAt() == null) {
                        if (editAt == null && lastHisPost.getCreatedAt().equals(createdAt)) {
                            // String nickname = mblog.getJSONObject("user").getString("screen_name");
                            // logger.info("@{}微博文章id={}已经推送过", nickname, id);
                            continue;
                        }
                    } else {
                        // 已编辑
                        if (lastHisPost.getEditAt().equals(editAt)) {
                            continue;
                        }
                    }
                }
                WeiboPost weiboPost = getWeiboPost(mblog);
                if (weiboPost != null) {
                    postList.add(weiboPost);
                }
            }
        }
        // 发送消息
        sendMessageBatch(postList);
    }

    public void sendMessageBatch(List<WeiboPost> weiboPostList) {
        String chatId = weiboSubscribeProperties.getTelegramChatId();

        if (CollectionUtils.isEmpty(weiboPostList)) {
            return;
        }

        List<WeiboPost> successList = new LinkedList<>();
        weiboPostList.sort(Comparator.nullsLast(Comparator.comparing(WeiboPost::getCreatedAt)));
        for (WeiboPost weiboPost : weiboPostList) {
            StringBuilder sb = new StringBuilder();
            sb.append("@");
            sb.append(escapeSymbol(weiboPost.getNickname()));

            LocalDateTime createdAt = weiboPost.getCreatedAt();
            if (createdAt != null) {
                String createdAtShow = createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                sb.append("\n");
                sb.append(createdAtShow);
            }

            sb.append(" 来自 ").append(weiboPost.getSource());

            LocalDateTime editAt = weiboPost.getEditAt();
            if (editAt != null) {
                String editAtShow = editAt.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                sb.append(" 编辑于");
                sb.append(editAtShow);
            }

            sb.append("\n\n");
            sb.append(escapeSymbol(weiboPost.getText()));

            if (weiboPost.getVideoUrl() != null) {
                sb.append("\n");
                sb.append("[").append(weiboPost.getNickname()).append("的微博视频](").append(weiboPost.getVideoUrl()).append(")");
            }

            sb.append("\n\n");
            sb.append("[微博原文](").append(weiboPost.getLink()).append(")");
            // 图片
            if (!CollectionUtils.isEmpty(weiboPost.getPics())) {
                sb.append("\n\n");
                for (int i = 0; i < weiboPost.getPics().size(); i++) {
                    if (i > 0) {
                        sb.append("  ");
                    }
                    sb.append("[图").append(i + 1).append("](").append(weiboPost.getPics().get(i)).append(")");
                }
            }

            String message = sb.toString();

            SendMessage sendMessage = new SendMessage(chatId, message);
            sendMessage.parseMode(ParseMode.Markdown);
            try {
                SendResponse response = telegramBot.execute(sendMessage);
                if (response.isOk()) {
                    weiboPost.setText(message);
                    successList.add(weiboPost);
                } else {
                    logger.error("发送TelegramBot错误,message={}", message);
                }
            } catch (Exception e) {
                logger.error("发送Telegram错误，微博文章={}", JSON.toJSONString(weiboPost));
                return;
            } finally {
                ThreadUtil.safeSleep(1000);
            }
        }
        saveHistory(successList);
    }

    /**
     * 保存发送记录
     */
    public void saveHistory(List<WeiboPost> weiboPostList) {
        if (CollectionUtils.isEmpty(weiboPostList)) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC+08"));
        List<HistoryPost> postList = new LinkedList<>();
        for (WeiboPost weiboPost : weiboPostList) {
            HistoryPost historyPost = new HistoryPost();
            historyPost.setUserId(weiboPost.getUserId());
            historyPost.setNickname(weiboPost.getNickname());
            historyPost.setPostId(weiboPost.getId());
            historyPost.setTopFlag(weiboPost.getTopFlag());
            historyPost.setCreatedAt(weiboPost.getCreatedAt());
            historyPost.setEditAt(weiboPost.getEditAt());
            historyPost.setCreateTime(now);
            postList.add(historyPost);
        }
        historyPostRepository.saveBatch(postList);
    }

    public WeiboPost getWeiboPost(JSONObject mblog) {
        boolean isLongText = mblog.getBooleanValue("isLongText");
        String bid = mblog.getString("bid");
        int topFlag = 0;
        JSONObject title = mblog.getJSONObject("title");
        if (title != null) {
            if ("置顶".equals(title.getString("text"))) {
                topFlag = 1;
            }
        }
        WeiboPost weiboPost;
        if (isLongText) {
            weiboPost = getWeiboLongText(bid);
        } else {
            weiboPost = parseToWeiboPost(mblog);
        }
        if (weiboPost != null) {
            weiboPost.setTopFlag(topFlag);
        }
        return weiboPost;
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
        weiboPost.setText(text0);
        weiboPost.setUserId(userId);
        weiboPost.setNickname(nickname);
        weiboPost.setSource(source);
        removeTags(weiboPost);
        weiboPost.setLink(getPcPostUrl(userId, bid));
        weiboPost.setCreatedAt(parseDate(createdAt));
        weiboPost.setEditAt(parseDate(editAt));

        // retweet 不为空表示转发微博
        JSONObject retweet = mblog.getJSONObject("retweeted_status");
        if (retweet != null) {
            String rawText = mblog.getString("raw_text");
            if (StringUtils.hasText(rawText)) {
                String appendText;
                WeiboPost retweetPost = getWeiboPost(retweet);
                if (retweetPost != null && StringUtils.hasText(retweetPost.getText())) {
                    appendText = "\n\n//@" + retweetPost.getNickname() + "\n" + retweetPost.getText();
                } else {
                    appendText = "\n//转发原文不可见，可能无法查看或已被删除";
                }
                weiboPost.setText(rawText + appendText);
            }
        }

        JSONArray pics = mblog.getJSONArray("pics");
        List<String> picUrlList = new LinkedList<>();
        if (pics != null && !pics.isEmpty()) {
            for (int i = 0; i < pics.size(); i++) {
                String picUrl = pics.getJSONObject(i).getJSONObject("large").getString("url");
                picUrlList.add(picUrl);
            }
        }
        weiboPost.setPics(picUrlList);

        // 视频链接
        String videoUrl = null;
        JSONObject pageInfo = mblog.getJSONObject("page_info");
        if (pageInfo != null) {
            JSONObject mediaInfo = pageInfo.getJSONObject("media_info");
            if (mediaInfo != null) {
                videoUrl = mediaInfo.getString("stream_url_hd");
                if (!StringUtils.hasText(videoUrl)) {
                    videoUrl = mediaInfo.getString("stream_url");
                }
            }
        }
        weiboPost.setVideoUrl(videoUrl);

        return weiboPost;
    }

    public WeiboPost getWeiboLongText(String bid) {
        String url = "https://m.weibo.cn/statuses/show?id=" + bid;
        String reqBody;
        try {
            reqBody = OkHttpClientUtil.requestGet(url);
        } catch (Exception ignored) {
            logger.error("无法获取微博长文,url={}", url);
            return null;
        }
        // 无法查看的微博
        if (!stringIsJson(reqBody)) {
            logger.error("您所访问的内容因版权问题不适合展示,url={}", url);
            return null;
        }
        JSONObject detail = JSON.parseObject(reqBody);
        JSONObject mblog = detail.getJSONObject("data");
        return parseToWeiboPost(mblog);
    }

    public void removeTags(WeiboPost weiboPost) {
        String text = weiboPost.getText().replace("<br />", "\n");

        List<String> strList0 = ReUtil.findAll(Pattern.compile("<a href.*?</a>",
                Pattern.CASE_INSENSITIVE), text, 0, new ArrayList<>());
        if (!CollectionUtils.isEmpty(strList0)) {
            for (String str : strList0) {
                int lio = str.lastIndexOf("@");
                String replacement;
                if (lio != -1) {
                    replacement = str.substring(lio, str.length() - 4);
                } else {
                    // <a href='http://t.cn/A6o1Xuh0' data-hide=''><span class='url-icon'><img style='width: 1rem;height: 1rem' src='//h5.sinaimg.cn/upload/2015/09/25/3/timeline_card_small_web_default.png'></span> <span class='surl-text'>网页链接</span></a>
                    String url = ReUtil.getGroup0("href='.*?'", str);
                    if (StringUtils.hasLength(url)) {
                        url = url.replace("'", "").replace("href=", "");
                    }
                    String str2 = ReUtil.getGroup0("<span class='surl-text'>.*?</span>", str);
                    if (StringUtils.hasLength(str2)) {
                        str2 = str2.replace("<span class='surl-text'>", "").replace("</span>", "");
                    }
                    replacement = "";
                    if (StringUtils.hasLength(url) && StringUtils.hasLength(str2)) {
                        replacement = "[" + str2 + "](" + url + ")";
                    }
                }

                text = text.replace(str, replacement);
            }
        }

        List<String> strList = ReUtil.findAll(Pattern.compile("<a  href.*?</a>",
                Pattern.CASE_INSENSITIVE), text, 0, new ArrayList<>());
        if (!CollectionUtils.isEmpty(strList)) {
            for (String str : strList) {
                String group0 = ReUtil.getGroup0("href=\".*?\"", str);
                String link = group0.replace("href=", "").replace("\"", "");
                String replacement = "";
                String name = ReUtil.getGroup0("#+[\\u4e00-\\u9fa5]+#", str);
                if (StringUtils.hasLength(name) && StringUtils.hasLength(link)) {
                    replacement = "[" + name + "](" + link + ")";
                }
                text = text.replace(str, replacement + " ");
            }
        }

        List<String> str2List = ReUtil.findAll(Pattern.compile("<span.*?</span>",
                Pattern.CASE_INSENSITIVE), text, 0, new ArrayList<>());
        if (!CollectionUtils.isEmpty(str2List)) {
            for (String str : str2List) {
                String emojiText = ReUtil.getGroup0("\\[+[\\u4e00-\\u9fa5]+\\]", str);
                if (StringUtils.hasLength(emojiText)) {
                    text = text.replace(str, emojiText);
                    continue;
                }

                text = text.replace(str, "");
            }
        }

        weiboPost.setText(text);
    }

    /**
     * 转义特殊字符
     *
     * @param str String
     * @return String
     */
    public String escapeSymbol(String str) {
        if (str == null) {
            return null;
        }
        return str.replace("_", "\\_")
                .replace("&gt;", ">");
    }

    public String getRetweetText(String text) {
        Set<String> strList = ReUtil.findAll(Pattern.compile("<a href.*?</a>",
                Pattern.CASE_INSENSITIVE), text, 0, new HashSet<>());
        if (!CollectionUtils.isEmpty(strList)) {
            for (String str : strList) {
                int lio = str.lastIndexOf("@");
                String substring = str.substring(lio, str.length() - 4);
                text = text.replace(str, substring);
            }
        }

        Set<String> strList2 = ReUtil.findAll(Pattern.compile("<span class.*?</span>",
                Pattern.CASE_INSENSITIVE), text, 0, new HashSet<>());
        if (!CollectionUtils.isEmpty(strList2)) {
            for (String str : strList2) {
                String replacement = "";
                List<String> list = ReUtil.findAllGroup0("[\\u4e00-\\u9fa5]", str);
                if (!CollectionUtils.isEmpty(list)) {
                    String join = String.join("", list);
                    replacement = "[" + join + "]";
                }
                text = text.replace(str, replacement);
            }
        }

        return text;
    }

    private String getPcPostUrl(String userId, String bid) {
        return "https://weibo.com/" + userId + "/" + bid;
    }

    public void testWeiboUserId() {
        List<String> userIds = userSubscribeRepository.getAllUserId(false);
        if (CollectionUtils.isEmpty(userIds)) {
            return;
        }
        String url = "https://m.weibo.cn/api/container/getIndex";
        for (String userId : userIds) {
            Map<String, String> paramsMap = Collections.singletonMap("containerid", "100505" + userId);
            String reqBody;
            try {
                reqBody = OkHttpClientUtil.requestGet(url, paramsMap);
            } catch (Exception ignored) {
                continue;
            }
            if (!stringIsJson(reqBody)) {
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

    private boolean stringIsJson(String str) {
        if (StringUtils.hasLength(str)) {
            return str.startsWith("{") || str.startsWith("[");
        }
        return false;
    }
}


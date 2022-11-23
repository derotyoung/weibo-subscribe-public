package com.derotyoung.util;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class PostUtil {

    private final static Logger logger = LoggerFactory.getLogger(PostUtil.class);

    private static final String USER_INDEX_URL = "https://m.weibo.cn/api/container/getIndex";

    private static final String LONG_TEXT_URL = "https://m.weibo.cn/statuses/show";

    private PostUtil() {
    }

    public static String getPcPostUrl(String userId, String bid) {
        return "https://weibo.com/" + userId + "/" + bid;
    }

    public static JSONArray getUserIndex(String userId) {
        Map<String, String> paramsMap = Map.of("containerid", ("107603" + userId),
                "uid", userId, "type", "uid", "value", userId);
        String url = OkHttpClientUtil.getRequestUrl(USER_INDEX_URL, paramsMap);
        String response;
        try {
            response = OkHttpClientUtil.requestGetWithSleep(url);
        } catch (Exception ignored) {
            logger.error("查询用户微博错误, userId={}, url={}", userId, url);
            return null;
        }
        if (strIsNotJson(response)) {
            logger.error("查询用户微博错误返回结果不是json无法解析, response={}", response);
            return null;
        }
        return JSON.parseObject(response).getJSONObject("data").getJSONArray("cards");
    }

    public static boolean isTopPost(JSONObject mblog) {
        JSONObject title = mblog.getJSONObject("title");
        if (title != null) {
            return "置顶".equals(title.getString("text"));
        }
        return false;
    }

    public static boolean haveSent(JSONObject mblog, List<HistoryPost> historyPosts) {
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

    public static WeiboPost getWeiboPost(JSONObject mblog) {
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

    public static WeiboPost parseToWeiboPost(JSONObject mblog) {
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
        weiboPost.setLink(getPcPostUrl(userId, bid));
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
            String retweetText = beautifyRetweetText(text0);
            weiboPost.setText(retweetText + appendText);
        } else {
            weiboPost.setText(beautifyText(text0));
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

    public static WeiboPost getWeiboLongText(JSONObject mblog) {
        String bid = mblog.getString("bid");
        String url = OkHttpClientUtil.getRequestUrl(LONG_TEXT_URL, Map.of("id", bid));
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

    public static String beautifyText(String text) {
        if (!StringUtils.hasLength(text)) {
            return text;
        }

        text = text.replace("<br />", "\n");

        List<String> strList1 = regexFindAll("<a href.*?</a>", text);
        if (!CollectionUtils.isEmpty(strList1)) {
            for (String str : strList1) {
                int lio = str.lastIndexOf("@");
                String replacement;
                if (lio != -1) {
                    replacement = str.substring(lio, str.length() - 4);
                } else {
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

        List<String> strList2 = regexFindAll("<a {2}href.*?</a>", text);
        if (!CollectionUtils.isEmpty(strList2)) {
            for (String str : strList2) {
                String link = parseUrlLink(str);
                if (StringUtils.hasLength(link)) {
                    String name1 = ReUtil.getGroup0("#.*?#", str);
                    if (StringUtils.hasLength(name1)) {
                        String replacement = "[" + name1 + "](" + link + ")";
                        text = text.replace(str, replacement + " ");
                    }

                    String name2 = parseText("[\\u4e00-\\u9fa5]", str);
                    if (StringUtils.hasLength(name2)) {
                        String replacement = "[" + name2 + "](" + link + ")";
                        text = text.replace(str, replacement + " ");
                    }
                }
                text = text.replace(str, " ");
            }
        }

        List<String> str3List = regexFindAll("<span.*?</span>", text);
        if (!CollectionUtils.isEmpty(str3List)) {
            for (String str : str3List) {
                String emojiText = ReUtil.getGroup0("\\[+[\\u4e00-\\u9fa5]+\\]", str);
                if (StringUtils.hasLength(emojiText)) {
                    text = text.replace(str, ("\\" + emojiText));
                    continue;
                }

                text = text.replace(str, "");
            }
        }

        return text;
    }

    public static String beautifyRetweetText(String text) {
        List<String> strList1 = regexFindAll("<a href.*?</a>", text);
        if (!CollectionUtils.isEmpty(strList1)) {
            for (String str : strList1) {
                int lio = str.lastIndexOf("@");
                String substring = str.substring(lio, str.length() - 4);
                text = text.replace(str, substring);
            }
        }

        List<String> strList2 = regexFindAll("<span class.*?</span>", text);
        if (!CollectionUtils.isEmpty(strList2)) {
            for (String str : strList2) {
                String replacement = "";
                List<String> list = ReUtil.findAllGroup0("[\\u4e00-\\u9fa5]", str);
                if (!CollectionUtils.isEmpty(list)) {
                    String join = String.join("", list);
                    replacement = "\\[" + join + "]";
                }
                text = text.replace(str, replacement);
            }
        }

        List<String> strList3 = regexFindAll("<a {2}href.*?</a>", text);
        if (!CollectionUtils.isEmpty(strList3)) {
            for (String str : strList3) {
                String link = parseUrlLink(str);
                if (StringUtils.hasLength(link)) {
                    String name2 = parseText("[\\u4e00-\\u9fa5]", str);
                    if (StringUtils.hasLength(name2)) {
                        String replacement = "[" + name2 + "](" + link + ")";
                        text = text.replace(str, replacement + " ");
                    }
                }
                text = text.replace(str, " ");
            }
        }

        return text;
    }

    public static List<String> regexFindAll(String regex, String str) {
        return ReUtil.findAll(Pattern.compile(regex, Pattern.CASE_INSENSITIVE), str, 0, new ArrayList<>());
    }

    public static String parseUrlLink(String str) {
        String group0 = ReUtil.getGroup0("href=\".*?\"", str);
        return group0.replace("href=", "").replace("\"", "");
    }

    public static String parseText(String regex, String str) {
        String text = null;
        List<String> zhs = regexFindAll(regex, str);
        if (!CollectionUtils.isEmpty(zhs)) {
            text = String.join("", zhs);
        }
        return text;
    }

    /**
     * 解析时间字符串
     *
     * @param str "Sat Apr 13 16:50:18 +0800 2019"
     * @return LocalDateTime
     */
    public static LocalDateTime parseDate(String str) {
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

    public static boolean strIsJson(String str) {
        if (StringUtils.hasLength(str)) {
            return str.startsWith("{") || str.startsWith("[");
        }
        return false;
    }

    public static boolean strIsNotJson(String str) {
        return !strIsJson(str);
    }
}

package com.derotyoung.util;

import cn.hutool.core.util.ReUtil;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class PostUtil {

    private PostUtil() {
    }

    public static String getPcPostUrl(String userId, String bid) {
        return "https://weibo.com/" + userId + "/" + bid;
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
                    String name1 = ReUtil.getGroup0("#+[\\u4e00-\\u9fa5]+#", str);
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
}

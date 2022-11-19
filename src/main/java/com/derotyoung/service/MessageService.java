package com.derotyoung.service;

import com.derotyoung.config.WeiboSubscribe;
import com.derotyoung.dto.Media;
import com.derotyoung.dto.WeiboPost;
import com.derotyoung.enums.MediaTypeEnum;
import com.derotyoung.util.FileUtil;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.InputMedia;
import com.pengrad.telegrambot.model.request.InputMediaPhoto;
import com.pengrad.telegrambot.model.request.InputMediaVideo;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.request.SendMediaGroup;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.BaseResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

@Service
public class MessageService {

    private final static Logger logger = LoggerFactory.getLogger(MessageService.class);

    @Resource
    private WeiboSubscribe weiboSubscribe;

    @Resource
    private TelegramBot telegramBot;

    @Resource
    private HistoryService historyService;

    public void sendMessageBatch(List<WeiboPost> weiboPostList) {
        String chatId = weiboSubscribe.getTelegramChatId();

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

            sb.append("\n\n");
            sb.append("[微博原文](").append(weiboPost.getLink()).append(")");
            sb.append("\n\n");
            String message = sb.toString();
            weiboPost.setText(message);

            if (CollectionUtils.isEmpty(weiboPost.getMediaList())) {
                SendMessage sendMessage = new SendMessage(chatId, message);
                sendMessage.parseMode(ParseMode.Markdown);
                sendMessage.disableWebPagePreview(true);
                execute(chatId, sendMessage, successList, weiboPost);
            } else {
                List<Media> mediaList = weiboPost.getMediaList();
                InputMedia<?>[] arr = new InputMedia<?>[mediaList.size()];
                for (int i = 0; i < mediaList.size(); i++) {
                    Media media = mediaList.get(i);
                    InputMedia<?> inputMedia = null;
                    if (MediaTypeEnum.PHOTO.value().equals(media.getType())) {
                        inputMedia = new InputMediaPhoto(media.getMedia());
                    } else if (MediaTypeEnum.VIDEO.value().equals(media.getType())) {
                        inputMedia = new InputMediaVideo(media.getMedia());
                    }
                    assert inputMedia != null;
                    byte[] thumbBytes = FileUtil.getBytes(media.getThumb());
                    if (thumbBytes != null) {
                        inputMedia.thumb(thumbBytes);
                    }
                    if (i == mediaList.size() - 1) {
                        inputMedia.caption(message);
                        inputMedia.parseMode(ParseMode.Markdown);
                    }
                    arr[i] = inputMedia;
                }
                SendMediaGroup sendMediaGroup = new SendMediaGroup(chatId, arr);
                execute(chatId, sendMediaGroup, successList, weiboPost);
            }
        }
        historyService.saveHistory(successList);
    }

    private <T extends BaseRequest<T, R>, R extends BaseResponse> void execute(String chatId, BaseRequest<T, R> request,
                                                                               List<WeiboPost> successList,
                                                                               WeiboPost weiboPost) {
        try {
            R response = telegramBot.execute(request);
            if (response.isOk()) {
                successList.add(weiboPost);
            } else if (response.errorCode() == 400
                    && "Bad Request: failed to send message #1 with the error message \"Wrong file identifier/HTTP URL specified\"".equals(response.description())) {
                StringBuilder sb = new StringBuilder(weiboPost.getText());
                for (int i = 0; i < weiboPost.getMediaList().size(); i++) {
                    Media media = weiboPost.getMediaList().get(i);
                    sb.append("[");
                    sb.append(MediaTypeEnum.desc(media.getType())).append(i + 1);
                    sb.append("](");
                    sb.append(media.getMedia());
                    sb.append(")");
                    sb.append(" ");
                }
                sb.append("\n\n");
                SendMessage sendMessage = new SendMessage(chatId, sb.toString());
                sendMessage.parseMode(ParseMode.Markdown);
                sendMessage.disableWebPagePreview(true);
                execute(chatId, sendMessage, successList, weiboPost);
            } else {
                logger.error("发送TelegramBot错误,response={},message={}", response, weiboPost.getText());
            }
        } catch (Exception e) {
            logger.error("TelegramBot推送消息异常, request={}", request, e);
        }
    }

    /**
     * 转义特殊字符
     *
     * @param str String
     * @return String
     */
    private String escapeSymbol(String str) {
        if (str == null) {
            return null;
        }
        return str.replace("_", "\\_")
                .replace("&gt;", ">");
    }
}

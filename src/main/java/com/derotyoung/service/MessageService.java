package com.derotyoung.service;

import com.derotyoung.config.WeiboSubscribe;
import com.derotyoung.dto.Media;
import com.derotyoung.dto.WeiboPost;
import com.derotyoung.enums.MediaTypeEnum;
import com.derotyoung.util.FileUtil;
import com.derotyoung.util.PostUtil;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.request.InputMedia;
import com.pengrad.telegrambot.model.request.InputMediaPhoto;
import com.pengrad.telegrambot.model.request.InputMediaVideo;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.request.SendMediaGroup;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.BaseResponse;
import com.pengrad.telegrambot.response.SendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class MessageService {

    private final static Logger logger = LoggerFactory.getLogger(MessageService.class);

    @Resource
    private WeiboSubscribe weiboSubscribe;

    @Resource
    private TelegramBot telegramBot;

    @Resource
    private HistoryService historyService;

    private final Set<String> CODE400_MESSAGE = Set.of(
            "Bad Request: failed to send message #1 with the error message \"Wrong file identifier/HTTP URL specified\"",
            "Bad Request: failed to send message #1 with the error message \"Failed to get HTTP URL content\"",
            "Bad Request: too many messages to send as an album");

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
                sb.append(escapeSymbol(createdAtShow));
            }

            sb.append(" 来自 ").append(escapeSymbol(weiboPost.getSource()));

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

            sendWithMedia(chatId, weiboPost, successList);
        }
        historyService.saveHistory(successList);
    }

    private <T extends BaseRequest<T, R>, R extends BaseResponse> void execute(String chatId, BaseRequest<T, R> request,
                                                                               WeiboPost weiboPost, List<WeiboPost> successList) {
        R response;
        try {
            response = telegramBot.execute(request);
        } catch (Exception e) {
            logger.error("TelegramBot推送消息异常,request={},message={}", request, weiboPost.getText(), e);
            PostUtil.putErrorId(weiboPost.getId());
            return;
        }

        if (response.isOk()) {
            if (response instanceof SendResponse) {
                Message message = ((SendResponse) response).message();
                if (message != null) {
                    weiboPost.setMessageId(message.messageId());
                }
            }
            successList.add(weiboPost);
        } else if (response.errorCode() == 400 && CODE400_MESSAGE.contains(response.description())) {
            sendAsMessage(chatId, weiboPost, successList);
        } else {
            logger.error("发送TelegramBot错误,errorCode={},description={},message={}",
                    response.errorCode(), response.description(), weiboPost.getText());
            PostUtil.putErrorId(weiboPost.getId());
        }
    }

    private void sendAsMessage(String chatId, WeiboPost weiboPost, List<WeiboPost> successList) {
        StringBuilder sb = new StringBuilder(weiboPost.getText());
        if (!CollectionUtils.isEmpty(weiboPost.getMediaList())) {
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
        }
        SendMessage sendMessage = new SendMessage(chatId, sb.toString());
        sendMessage.parseMode(ParseMode.MarkdownV2);
        sendMessage.disableWebPagePreview(true);
        execute(chatId, sendMessage, weiboPost, successList);
    }

    private void sendWithMedia(String chatId, WeiboPost weiboPost, List<WeiboPost> successList) {
        List<Media> mediaList = weiboPost.getMediaList();

        // telegram最多支持9张图片，超出转成文本发送
        if (CollectionUtils.isEmpty(mediaList) || mediaList.size() > 9) {
            sendAsMessage(chatId, weiboPost, successList);
            return;
        }

        List<InputMedia<?>> inputMediaList = new ArrayList<>(mediaList.size());
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
                inputMedia.caption(weiboPost.getText());
                inputMedia.parseMode(ParseMode.MarkdownV2);
            }
            inputMediaList.add(inputMedia);
        }

        if (inputMediaList.isEmpty()) {
            sendAsMessage(chatId, weiboPost, successList);
            return;
        }
        SendMediaGroup sendMediaGroup = new SendMediaGroup(chatId, inputMediaList.toArray(new InputMedia<?>[0]));
        execute(chatId, sendMediaGroup, weiboPost, successList);
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
        // In all other places characters
        // '_', '*', '[', ']', '(', ')', '~', '`', '>', '#', '+', '-', '=', '|', '{', '}', '.', '!'
        // must be escaped with the preceding character '\'.
        return str
                .replace("_", "\\_")
                .replace("*", "\\*")
                .replace("~", "\\~")
                .replace("`", "\\`")
                // .replace(">", "\\>")
                .replace("#", "\\#")
                .replace("+", "\\+")
                .replace("-", "\\-")
                .replace("=", "\\=")
                .replace("|", "\\|")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace(".", "\\.")
                .replace("!", "\\!")

                .replace("&lt;", "<")
                .replace("&gt;", "\\>")
                .replace("&quot;", "\"");
    }
}
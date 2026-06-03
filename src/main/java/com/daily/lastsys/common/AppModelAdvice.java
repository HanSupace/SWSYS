package com.daily.lastsys.common;

import com.daily.lastsys.features.emotion.EmotionCatalog;
import com.daily.lastsys.features.emotion.EmotionCatalog.EmotionMeta;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice
public class AppModelAdvice {

    @ModelAttribute("appName")
    public String appName() {
        return AppMetadata.APP_NAME;
    }

    @ModelAttribute("emotions")
    public List<EmotionMeta> emotions() {
        return EmotionCatalog.all();
    }

    @ModelAttribute("emotionCatalogJson")
    public String emotionCatalogJson() {
        StringBuilder json = new StringBuilder("[");
        List<EmotionMeta> emotions = EmotionCatalog.all();

        for (int index = 0; index < emotions.size(); index += 1) {
            if (index > 0) {
                json.append(',');
            }

            EmotionMeta emotion = emotions.get(index);
            json.append('{')
                    .append("\"id\":").append(quoteJson(emotion.id())).append(',')
                    .append("\"code\":").append(quoteJson(emotion.code())).append(',')
                    .append("\"value\":").append(quoteJson(emotion.value())).append(',')
                    .append("\"label\":").append(quoteJson(emotion.label())).append(',')
                    .append("\"icon\":").append(quoteJson(emotion.icon())).append(',')
                    .append("\"color\":").append(quoteJson(emotion.color())).append(',')
                    .append("\"background\":").append(quoteJson(emotion.background())).append(',')
                    .append("\"border\":").append(quoteJson(emotion.border())).append(',')
                    .append("\"positive\":").append(emotion.positive())
                    .append('}');
        }

        return json.append(']').toString();
    }

    private String quoteJson(String value) {
        StringBuilder quoted = new StringBuilder("\"");

        for (int index = 0; index < value.length(); index += 1) {
            char character = value.charAt(index);

            switch (character) {
                case '\\' -> quoted.append("\\\\");
                case '"' -> quoted.append("\\\"");
                case '\b' -> quoted.append("\\b");
                case '\f' -> quoted.append("\\f");
                case '\n' -> quoted.append("\\n");
                case '\r' -> quoted.append("\\r");
                case '\t' -> quoted.append("\\t");
                default -> {
                    if (character < 0x20) {
                        quoted.append(String.format("\\u%04x", (int) character));
                    } else {
                        quoted.append(character);
                    }
                }
            }
        }

        return quoted.append('"').toString();
    }
}

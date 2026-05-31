package com.daily.lastsys.features.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UserReportForm {

    @NotBlank(message = "위치를 입력해주세요.")
    @Size(max = 80, message = "위치는 80자 이하로 입력해주세요.")
    private String location;

    @NotBlank(message = "제목을 입력해주세요.")
    @Size(max = 60, message = "제목은 60자 이하로 입력해주세요.")
    private String title;

    @NotBlank(message = "내용을 입력해주세요.")
    @Size(max = 500, message = "내용은 500자 이하로 입력해주세요.")
    private String content;

    @NotBlank(message = "신고 카테고리를 선택해주세요.")
    @Pattern(
            regexp = "ABUSE|SELF_HARM|HATE_SPEECH|SPAM|AD_SUSPECT|OTHER",
            message = "올바른 신고 카테고리를 선택해주세요."
    )
    private String category;

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}

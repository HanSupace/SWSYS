package com.daily.lastsys.features.dailymission;

public class MissionSettingsForm {

    private String mode = "PLAIN";
    private String lifeStage = "ANY";
    private String environmentType = "ANY";
    private String conditionType = "NORMAL";

    public MissionSettingsForm() {
    }

    public MissionSettingsForm(MissionSettings settings) {
        this.mode = settings.mode();
        this.lifeStage = settings.lifeStage();
        this.environmentType = settings.environmentType();
        this.conditionType = settings.conditionType();
    }

    public MissionSettings toSettings() {
        return new MissionSettings(
                normalize(mode, "PLAIN"),
                normalize(lifeStage, "ANY"),
                normalize(environmentType, "ANY"),
                normalize(conditionType, "NORMAL")
        );
    }

    private String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getLifeStage() {
        return lifeStage;
    }

    public void setLifeStage(String lifeStage) {
        this.lifeStage = lifeStage;
    }

    public String getEnvironmentType() {
        return environmentType;
    }

    public void setEnvironmentType(String environmentType) {
        this.environmentType = environmentType;
    }

    public String getConditionType() {
        return conditionType;
    }

    public void setConditionType(String conditionType) {
        this.conditionType = conditionType;
    }
}

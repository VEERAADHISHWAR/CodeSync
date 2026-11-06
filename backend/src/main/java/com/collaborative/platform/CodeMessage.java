package com.collaborative.platform;

public class CodeMessage {

    private String roomId;
    private String code;
    private String language;
    private String versionName;
    private String description;

    public CodeMessage() {
    }

    public CodeMessage(String roomId, String code, String language) {
        this.roomId = roomId;
        this.code = code;
        this.language = language;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
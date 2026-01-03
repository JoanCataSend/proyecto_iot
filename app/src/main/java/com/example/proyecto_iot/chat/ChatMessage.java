package com.example.proyecto_iot.chat;

import androidx.annotation.NonNull;

public class ChatMessage {
    public static final String ROLE_SYSTEM = "system";
    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";

    private final String role;
    private final String text;

    public ChatMessage(@NonNull String role, @NonNull String text) {
        this.role = role;
        this.text = text;
    }

    public String getRole() {
        return role;
    }

    public String getText() {
        return text;
    }

    public boolean isUser() {
        return ROLE_USER.equals(role);
    }
}

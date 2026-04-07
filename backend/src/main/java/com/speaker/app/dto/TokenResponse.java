package com.speaker.app.dto;

public record TokenResponse(String token, String tokenType, long expiresInMs) {}

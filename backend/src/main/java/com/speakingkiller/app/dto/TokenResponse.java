package com.speakingkiller.app.dto;

public record TokenResponse(String token, String tokenType, long expiresInMs) {}

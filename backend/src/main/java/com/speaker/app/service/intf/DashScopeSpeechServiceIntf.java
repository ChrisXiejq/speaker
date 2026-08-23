package com.speaker.app.service.intf;

public interface DashScopeSpeechServiceIntf {

    String transcribeEnglish(byte[] audioBytes, String filenameHint);

    byte[] synthesizeEnglish(String text);
}

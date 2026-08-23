package com.speaker.app.service.intf;

import java.util.List;
import java.util.Map;

public interface QwenServiceIntf {

    String chat(String systemPrompt, List<Map<String, String>> messages);
}

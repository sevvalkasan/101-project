package com.masabul.backend.notification;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface PushGateway {

    Set<String> send(
        List<String> tokens,
        String title,
        String body,
        Map<String, String> data
    );
}

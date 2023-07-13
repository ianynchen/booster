package io.github.booster.config.example.dto;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class GreetingResponse {

    private String from;

    private String greeting;
}

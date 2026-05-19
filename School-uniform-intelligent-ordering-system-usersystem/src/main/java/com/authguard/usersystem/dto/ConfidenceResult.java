package com.authguard.usersystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ConfidenceResult {
    private int score;
    private String level;
    private String message;
    private boolean lowConfidence;
}

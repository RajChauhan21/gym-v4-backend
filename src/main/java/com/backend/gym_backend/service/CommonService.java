package com.backend.gym_backend.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class CommonService {

    public LocalDate convertEpochToLocalDate(Long epochSeconds) {
        if (epochSeconds == null) {
            return null;
        }
        return Instant.ofEpochSecond(epochSeconds)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }
}

package com.backend.gym_backend.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class InvoiceTemplateResponse {

    private int id;
    private String name;
    private String category;
    private boolean featured;
    private String previewUrl;
    private String description;
}

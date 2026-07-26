package com.backend.gym_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportMemberError {

    private int rowNumber;
    private String memberName;
    private List<String> errors;
}

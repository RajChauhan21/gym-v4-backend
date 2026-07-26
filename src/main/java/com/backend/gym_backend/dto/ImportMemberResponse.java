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
public class ImportMemberResponse {

    private int totalRows;

    private int importedRows;

    private int failedRows;

    private List<ImportMemberError> errors;

}
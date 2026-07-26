package com.backend.gym_backend.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportErrorDto {

    private Integer rowNumber;

    private List<String> messages;

}

package com.backend.gym_backend.response;

import lombok.Data;

@Data
public class Payload {

    private Payment payment;
    private Order order;
}

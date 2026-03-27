package com.backend.gym_backend.response;

import lombok.Data;

import java.util.ArrayList;

@Data
public class PaymentPayload {

    public String entity;
    public String account_id;
    public String event;
    public ArrayList<String> contains;
    public Payload payload;
    public int created_at;
}

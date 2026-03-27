package com.backend.gym_backend.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Entity {
    public String id;
    public String entity;
    public int amount;
    public String currency;
    public String status;
    public String order_id;
    public Object invoice_id;
    public boolean international;
    public String method;
    public int amount_refunded;
    public Object refund_status;
    public boolean captured;
    public Object description;
    public Object card_id;
    public Object bank;
    public Object wallet;
    public String vpa;
    public String email;
    public String contact;
    public ArrayList<Object> notes;
    public int fee;
    public int tax;
    public Object error_code;
    public Object error_description;
    public int created_at;
    public int amount_paid;
    public int amount_due;
    public String receipt;
    public Object offer_id;
    public int attempts;
}

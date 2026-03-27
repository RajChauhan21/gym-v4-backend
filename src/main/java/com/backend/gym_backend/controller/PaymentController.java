package com.backend.gym_backend.controller;

import com.backend.gym_backend.dto.PaymentRequest;
import com.backend.gym_backend.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pay")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/save")
    public ResponseEntity<?> save(@RequestBody PaymentRequest request){
        return new ResponseEntity<>(paymentService.save(request), HttpStatus.ACCEPTED);
    }

    @GetMapping("/getAllPaymentsOfMember")
    public ResponseEntity<?> save(@RequestParam("q") Integer id){
        return new ResponseEntity<>(paymentService.getAllPaymentsOfMember(id), HttpStatus.ACCEPTED);
    }

    @DeleteMapping("/deleteById")
    public ResponseEntity<?> deleteById(@RequestParam("q") Integer id){
        return new ResponseEntity<>(paymentService.deleteById(id), HttpStatus.ACCEPTED);
    }
}

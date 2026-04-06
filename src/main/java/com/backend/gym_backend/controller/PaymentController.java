package com.backend.gym_backend.controller;

import com.backend.gym_backend.dto.PaymentRequest;
import com.backend.gym_backend.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@CrossOrigin("*")
@RestController
@RequestMapping("/pay")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/save")
    public ResponseEntity<?> save(@RequestBody PaymentRequest request) {
        return new ResponseEntity<>(paymentService.save(request), HttpStatus.ACCEPTED);
    }

    @GetMapping("/getAllPaymentsOfMember")
    public ResponseEntity<?> save(@RequestParam("q") Integer id) {
        return new ResponseEntity<>(paymentService.getAllPaymentsOfMember(id), HttpStatus.ACCEPTED);
    }

    @GetMapping("/getAllPaymentsOfMembersByOwnerId")
    public ResponseEntity<?> getAllPaymentsOfMemberByOwnerId(@RequestParam("q") Integer ownerId,
                                                             @RequestParam(value = "m", required = false) String memberName,
                                                             @RequestParam(value = "p",required = false) String plan,
                                                             @RequestParam(value = "meth",required = false) String method,
                                                             @RequestParam(value = "a",required = false) Integer amount,
                                                             @RequestParam(value = "df",required = false) LocalDate dateFrom,
                                                             @RequestParam(value = "dt",required = false) LocalDate dateTo, Pageable pageable) {
        return new ResponseEntity<>(paymentService.getAllPaymentsOfMemberByOwnerId(Long.valueOf(ownerId), memberName, plan, method, amount, dateFrom, dateTo, pageable), HttpStatus.ACCEPTED);
    }

    @DeleteMapping("/deleteById")
    public ResponseEntity<?> deleteById(@RequestParam("q") Integer id) {
        return new ResponseEntity<>(paymentService.deleteById(id), HttpStatus.ACCEPTED);
    }
}

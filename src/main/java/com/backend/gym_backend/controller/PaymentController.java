package com.backend.gym_backend.controller;

import com.backend.gym_backend.dto.PaymentRequest;
import com.backend.gym_backend.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/pay")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/update")
    public ResponseEntity<?> save(@RequestBody PaymentRequest request) {
        return new ResponseEntity<>(paymentService.update(request), HttpStatus.ACCEPTED);
    }

    @GetMapping("/getAllPaymentsOfMember")
    public ResponseEntity<?> save(@RequestParam("q") Integer id) {
        return new ResponseEntity<>(paymentService.getAllPaymentsOfMember(id), HttpStatus.ACCEPTED);
    }

    @GetMapping("/getAllPaymentsOfMembersByOwnerId")
    public ResponseEntity<?> getAllPaymentsOfMemberByOwnerId(@RequestParam("q") Integer ownerId,
                                                             @RequestParam(value = "name", required = false) String memberName,
                                                             @RequestParam(value = "plan",required = false) String plan,
                                                             @RequestParam(value = "method",required = false) String method,
                                                             @RequestParam(value = "amount",required = false) Integer amount,
                                                             @RequestParam(value = "from",required = false) LocalDate dateFrom,
                                                             @RequestParam(value = "to",required = false) LocalDate dateTo, Pageable pageable) {
        return new ResponseEntity<>(paymentService.getAllPaymentsOfMemberByOwnerId(Long.valueOf(ownerId), memberName, plan, method, amount, dateFrom, dateTo, pageable), HttpStatus.ACCEPTED);
    }

    @GetMapping("/getTotalAmount")
    public ResponseEntity<?> getTotalAmount(){
        return new ResponseEntity<>(paymentService.getTotalAmountPaid(),HttpStatus.ACCEPTED);
    }

    @GetMapping("/getRevenue")
    public ResponseEntity<?> getRevenueThisMonth(@RequestParam("q") Integer ownerId){
        return new ResponseEntity<>(paymentService.getRevenues(ownerId),HttpStatus.ACCEPTED);
    }

    @GetMapping("/getRevenueChartDetails")
    public ResponseEntity<?> getRevenueChartDetails(@RequestParam("q") Integer ownerId, @RequestParam("d") Integer days){
        return new ResponseEntity<>(paymentService.getRevenueOverview(ownerId,days),HttpStatus.ACCEPTED);
    }



    @DeleteMapping("/deleteById")
    public ResponseEntity<?> deleteById(@RequestParam("q") Integer id) {
        return new ResponseEntity<>(paymentService.deleteById(id), HttpStatus.ACCEPTED);
    }
}

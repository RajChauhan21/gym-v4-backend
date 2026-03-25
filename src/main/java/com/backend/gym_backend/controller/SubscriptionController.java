package com.backend.gym_backend.controller;

import com.backend.gym_backend.service.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/subs")
public class SubscriptionController {

    @Autowired
    private SubscriptionService subscriptionService;

    @GetMapping("/add")
    public ResponseEntity<?> save(@RequestParam("p") Integer pId, @RequestParam("o") Integer oId){
        return new ResponseEntity<>(subscriptionService.ownerSubscribesToPlan(oId,pId), HttpStatus.ACCEPTED);
    }

    @GetMapping("/findById")
    public ResponseEntity<?> findById(@RequestParam("p") Integer id){
        return new ResponseEntity<>(subscriptionService.findById(id), HttpStatus.ACCEPTED);
    }

    @GetMapping("/findAll")
    public ResponseEntity<?> findAll(){
        return new ResponseEntity<>(subscriptionService.findAllSubscriptions(), HttpStatus.ACCEPTED);
    }
}

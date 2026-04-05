package com.backend.gym_backend.controller;

import com.backend.gym_backend.dto.PlanRequest;
import com.backend.gym_backend.service.PlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin("*")
@RestController
@RequestMapping("/plan")
public class PlanController {

    @Autowired
    private PlanService planService;

    @PostMapping("/save")
    public ResponseEntity<?> save(@RequestBody PlanRequest requestDto){
        return new ResponseEntity<>(planService.save(requestDto), HttpStatus.ACCEPTED);
    }

    @PutMapping("/update")
    public ResponseEntity<?> update(@RequestBody PlanRequest requestDto){
        return new ResponseEntity<>(planService.update(requestDto), HttpStatus.ACCEPTED);
    }

    @GetMapping("/findById")
    public ResponseEntity<?> findById(@RequestParam("q") Integer id){
        return new ResponseEntity<>(planService.findById(id), HttpStatus.ACCEPTED);
    }

    @GetMapping("/findAll")
    public ResponseEntity<?> findAllPlans(){
        return new ResponseEntity<>(planService.findAllPlans(), HttpStatus.ACCEPTED);
    }

    @GetMapping("/getAllFeatures")
    public ResponseEntity<?> getAllFeaturesOfPlan(@RequestParam("q") Integer planId){
        return new ResponseEntity<>(planService.getAllFeaturesOfPlan(planId), HttpStatus.ACCEPTED);
    }

    @DeleteMapping("/deleteById")
    public ResponseEntity<?> deleteById(@RequestParam("q") Integer id){
        return new ResponseEntity<>(planService.deleteById(id), HttpStatus.ACCEPTED);
    }
}

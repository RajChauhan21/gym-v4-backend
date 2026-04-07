package com.backend.gym_backend.controller;

import com.backend.gym_backend.service.PlanFeatureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/plan-feat")
public class PlanFeatureController {

    @Autowired
    private PlanFeatureService planFeatureService;

    @GetMapping("/add")
    public ResponseEntity<?> addFeaturesToPlan(@RequestParam("f") Integer fId,@RequestParam("p") Integer pId){
        return new ResponseEntity<>(planFeatureService.addFeaturesToPlan(fId,pId), HttpStatus.ACCEPTED);
    }

    @GetMapping("/update")
    public ResponseEntity<?> updateFeaturesInPlan(@RequestParam("q") Integer PfId, @RequestParam("f") Integer fId,@RequestParam("p") Integer pId){
        return new ResponseEntity<>(planFeatureService.updateFeaturesInPlan(PfId,fId,pId), HttpStatus.ACCEPTED);
    }

    @GetMapping("/findById")
    public ResponseEntity<?> findById(@RequestParam("q") Integer PfId){
        return new ResponseEntity<>(planFeatureService.findById(PfId), HttpStatus.ACCEPTED);
    }

    @GetMapping("/findAll")
    public ResponseEntity<?> findAllPlanFeatures(){
        return new ResponseEntity<>(planFeatureService.findAllPlanFeatures(), HttpStatus.ACCEPTED);
    }

    @DeleteMapping("/deleteById")
    public ResponseEntity<?> deleteById(@RequestParam("q") Integer PfId){
        return new ResponseEntity<>(planFeatureService.deleteById(PfId), HttpStatus.ACCEPTED);
    }
}

package com.backend.gym_backend.controller;

import com.backend.gym_backend.dto.FeatureRequest;
import com.backend.gym_backend.service.FeatureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/feat")
public class FeatureController {

    @Autowired
    private FeatureService featureService;


    @PostMapping("/save")
    public ResponseEntity<?> save(@RequestBody FeatureRequest requestDto) {
        return new ResponseEntity<>(featureService.save(requestDto), HttpStatus.ACCEPTED);
    }

    @PutMapping("/update")
    public ResponseEntity<?> update(@RequestBody FeatureRequest requestDto) {
        return new ResponseEntity<>(featureService.update(requestDto), HttpStatus.ACCEPTED);
    }

    @GetMapping("/findById")
    public ResponseEntity<?> findById(@RequestParam("q") Integer id) {
        return new ResponseEntity<>(featureService.findById(id), HttpStatus.ACCEPTED);
    }

    @GetMapping("/findAll")
    public ResponseEntity<?> findAll() {
        return new ResponseEntity<>(featureService.getAllFeatures(), HttpStatus.ACCEPTED);
    }

    @DeleteMapping("/deleteById")
    public ResponseEntity<?> deleteById(@RequestParam("q") Integer id){
        return new ResponseEntity<>(featureService.deleteById(id),HttpStatus.ACCEPTED);
    }

}

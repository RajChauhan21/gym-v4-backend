package com.backend.gym_backend.controller;

import com.backend.gym_backend.dto.GymDetailsRequestDto;
import com.backend.gym_backend.service.GymService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/gym")
public class GymController {

    @Autowired
    private GymService gymService;

    @PostMapping("/save")
    public ResponseEntity<?> save(@RequestBody GymDetailsRequestDto requestDto){
        return new ResponseEntity<>(gymService.save(requestDto), HttpStatus.ACCEPTED);
    }

    @PutMapping("/update")
    public ResponseEntity<?> update(@RequestBody GymDetailsRequestDto requestDto){
        return new ResponseEntity<>(gymService.update(requestDto), HttpStatus.ACCEPTED);
    }

    @GetMapping("/findById")
    public ResponseEntity<?> findById(@RequestParam("q") Integer id){
        return new ResponseEntity<>(gymService.findById(id), HttpStatus.ACCEPTED);
    }

    @DeleteMapping("/deleteById")
    public ResponseEntity<?> deleteById(@RequestParam("q") Integer id){
        return new ResponseEntity<>(gymService.deleteById(id), HttpStatus.ACCEPTED);
    }
}

package com.backend.gym_backend.controller;

import com.backend.gym_backend.dto.MemberShipRequest;
import com.backend.gym_backend.service.MemberShipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/member-ship")
public class MemberShipController {

    @Autowired
    private MemberShipService memberShipService;

    @PostMapping("/save")
    public ResponseEntity<?> save(@RequestBody MemberShipRequest request){
        return new ResponseEntity<>(memberShipService.save(request), HttpStatus.ACCEPTED);
    }

    @PostMapping("/update")
    public ResponseEntity<?> update(@RequestBody MemberShipRequest request){
        return new ResponseEntity<>(memberShipService.update(request), HttpStatus.ACCEPTED);
    }

    @GetMapping("/getAll")
    public ResponseEntity<?> getAll(@RequestParam(value = "q",required = false) Integer gymId){
        return new ResponseEntity<>(memberShipService.getAllByGymId(gymId), HttpStatus.ACCEPTED);
    }

    @GetMapping("/findById")
    public ResponseEntity<?> findById(@RequestParam("q") Integer id){
        return new ResponseEntity<>(memberShipService.findById(id), HttpStatus.ACCEPTED);
    }

    @DeleteMapping("/deleteById")
    public ResponseEntity<?> deleteById(@RequestParam("q") Integer id){
        return new ResponseEntity<>(memberShipService.deleteById(id), HttpStatus.ACCEPTED);
    }
}

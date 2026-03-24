package com.backend.gym_backend.controller;

import com.backend.gym_backend.dto.OwnerDetailsRequestDto;
import com.backend.gym_backend.service.OwnerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/owner")
public class OwnerController {

    @Autowired
    private OwnerService ownerService;

    @PostMapping("/save")
    public ResponseEntity<?> saveOwner(@RequestBody OwnerDetailsRequestDto detailsRequestDto){
        return new ResponseEntity<>(ownerService.save(detailsRequestDto), HttpStatus.ACCEPTED);
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateOwner(@RequestBody OwnerDetailsRequestDto detailsRequestDto){
        return new ResponseEntity<>(ownerService.update(detailsRequestDto), HttpStatus.ACCEPTED);
    }

    @GetMapping("/findById")
    public ResponseEntity<?> findOwnerById(@RequestParam("q") Integer id){
        return new ResponseEntity<>(ownerService.findById(id), HttpStatus.ACCEPTED);
    }

    @DeleteMapping("/deleteById")
    public ResponseEntity<?> deleteOwnerById(@RequestParam("q") Integer id){
        return new ResponseEntity<>(ownerService.deleteById(id), HttpStatus.ACCEPTED);
    }
}

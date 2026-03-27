package com.backend.gym_backend.controller;

import com.backend.gym_backend.dto.OwnerDetailsRequest;
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
    public ResponseEntity<?> saveOwner(@RequestBody OwnerDetailsRequest detailsRequestDto){
        return new ResponseEntity<>(ownerService.save(detailsRequestDto), HttpStatus.ACCEPTED);
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateOwner(@RequestBody OwnerDetailsRequest detailsRequestDto){
        return new ResponseEntity<>(ownerService.update(detailsRequestDto), HttpStatus.ACCEPTED);
    }

    @GetMapping("/findById")
    public ResponseEntity<?> findOwnerById(@RequestParam("q") Integer id){
        return new ResponseEntity<>(ownerService.findById(id), HttpStatus.ACCEPTED);
    }

    @GetMapping("/findAll")
    public ResponseEntity<?> findAllOwners(){
        return new ResponseEntity<>(ownerService.findAllOwners(), HttpStatus.ACCEPTED);
    }

    @GetMapping("/getAllMembers")
    public ResponseEntity<?> getAllMembersOfOwner(@RequestParam("q") Integer ownerId){
        return new ResponseEntity<>(ownerService.getAllMembersOfOwner(ownerId), HttpStatus.ACCEPTED);
    }

    @DeleteMapping("/deleteById")
    public ResponseEntity<?> deleteOwnerById(@RequestParam("q") Integer id){
        return new ResponseEntity<>(ownerService.deleteById(id), HttpStatus.ACCEPTED);
    }
}

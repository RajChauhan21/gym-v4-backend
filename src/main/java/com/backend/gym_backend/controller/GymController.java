package com.backend.gym_backend.controller;

import com.backend.gym_backend.dto.GymDetailsRequest;
import com.backend.gym_backend.service.CloudinaryService;
import com.backend.gym_backend.service.GymService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/gym")
public class GymController {

    @Autowired
    private GymService gymService;

    @Autowired
    private CloudinaryService cloudinaryService;

    @PostMapping("/save")
    public ResponseEntity<?> save(@RequestBody GymDetailsRequest requestDto){
        return new ResponseEntity<>(gymService.save(requestDto), HttpStatus.ACCEPTED);
    }

    @PutMapping("/update")
    public ResponseEntity<?> update(@RequestBody GymDetailsRequest requestDto) throws Exception {
        return new ResponseEntity<>(gymService.update(requestDto), HttpStatus.ACCEPTED);
    }

    @GetMapping("/findById")
    public ResponseEntity<?> findById(@RequestParam("q") Integer id){
        return new ResponseEntity<>(gymService.findById(id), HttpStatus.ACCEPTED);
    }

    @PostMapping("upload/image")
    public ResponseEntity<?> uploadImageForGym(@RequestParam("g") int id,@RequestParam("t") String target, @RequestParam("file") MultipartFile file) {
        return cloudinaryService.uploadImage(id,target,file);
    }

    @GetMapping("/findAll")
    public ResponseEntity<?> findAllGyms(){
        return new ResponseEntity<>(gymService.findAllGyms(), HttpStatus.ACCEPTED);
    }

    @DeleteMapping("/deleteById")
    public ResponseEntity<?> deleteById(@RequestParam("q") Integer id){
        return new ResponseEntity<>(gymService.deleteById(id), HttpStatus.ACCEPTED);
    }
}

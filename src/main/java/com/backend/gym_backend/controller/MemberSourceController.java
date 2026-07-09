package com.backend.gym_backend.controller;

import com.backend.gym_backend.dto.MemberSourceRequest;
import com.backend.gym_backend.service.MemberSourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/member-source")
public class MemberSourceController {

    @Autowired
    private MemberSourceService service;

    @PostMapping("/save")
    public ResponseEntity<?> save(@RequestBody MemberSourceRequest request){
        return new ResponseEntity<>(service.save(request), HttpStatus.ACCEPTED);
    }

    @PostMapping("/update")
    public ResponseEntity<?> update(@RequestBody MemberSourceRequest request){
        return new ResponseEntity<>(service.update(request), HttpStatus.ACCEPTED);
    }

    @GetMapping("/getAll")
    public ResponseEntity<?> getAllSourcesOfOwner(@RequestParam("q") Integer ownerId){
        return new ResponseEntity<>(service.getAllSourcesOfOwner(ownerId),HttpStatus.ACCEPTED);
    }

    @GetMapping("/count-member-source")
    public ResponseEntity<?> countMemberSource(@RequestParam("q") Integer sourceId){
        return new ResponseEntity<>(service.countMembersBySourceId(sourceId),HttpStatus.ACCEPTED);
    }

    @GetMapping("/get-source-analytics")
    public ResponseEntity<?> getSourceAnalytics(@RequestParam("q") Integer ownerId){
        return new ResponseEntity<>(service.getSourceAnalytics(ownerId),HttpStatus.ACCEPTED);
    }

    @GetMapping("/delete")
    public ResponseEntity<?> deleteById(@RequestParam("q") Integer sourceId){
        return new ResponseEntity<>(service.deleteSource(sourceId),HttpStatus.ACCEPTED);
    }
}

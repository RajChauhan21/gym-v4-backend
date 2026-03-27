package com.backend.gym_backend.controller;

import com.backend.gym_backend.dto.MemberRequest;
import com.backend.gym_backend.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/member")
public class MemberController {

    @Autowired
    private MemberService memberService;

    @PostMapping("/save")
    public ResponseEntity<?> save(@RequestBody MemberRequest request){
        return new ResponseEntity<>(memberService.save(request), HttpStatus.ACCEPTED);
    }

    @PutMapping("/update")
    public ResponseEntity<?> update(@RequestBody MemberRequest request){
        return new ResponseEntity<>(memberService.update(request), HttpStatus.ACCEPTED);
    }

    @GetMapping("/findById")
    public ResponseEntity<?> findById(@RequestParam("q") Integer id){
        return new ResponseEntity<>(memberService.findById(id), HttpStatus.ACCEPTED);
    }

    @GetMapping("/findByMemberShipId")
    public ResponseEntity<?> getMembersOnMemberShipId(@RequestParam("q") Integer id){
        return new ResponseEntity<>(memberService.getMembersOnMemberShipId(id), HttpStatus.ACCEPTED);
    }

    @DeleteMapping("/deleteById")
    public ResponseEntity<?> deleteById(@RequestParam("q") Integer id){
        return new ResponseEntity<>(memberService.deleteById(id), HttpStatus.ACCEPTED);
    }

}

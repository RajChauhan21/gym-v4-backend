package com.backend.gym_backend.controller;

import com.backend.gym_backend.dto.MemberRequest;
import com.backend.gym_backend.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
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

    @PostMapping("/update")
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

    @GetMapping("/searchMembers")
    public ResponseEntity<?> searchMembers(@RequestParam("o") Integer ownerId,@RequestParam("q") String query){
        return new ResponseEntity<>(memberService.searchMembers(ownerId,query), HttpStatus.ACCEPTED);
    }

    @GetMapping("/getActiveMembers")
    public ResponseEntity<?> getActiveMembers(@RequestParam("o") Integer ownerId){
        return new ResponseEntity<>(memberService.countActiveMembers(ownerId), HttpStatus.ACCEPTED);
    }

    @GetMapping("/getMembersJoined")
    public ResponseEntity<?> getMembersJoinedCurrentMonth(@RequestParam("o") Integer ownerId){
        return new ResponseEntity<>(memberService.getMembersJoinedCurrentMonth(ownerId), HttpStatus.ACCEPTED);
    }

    @GetMapping("/getMembersExpiringSoon")
    public ResponseEntity<?> getMembersExpiringSoon(@RequestParam("o") Integer ownerId){
        return new ResponseEntity<>(memberService.getMembersCountExpiringIn7Days(ownerId), HttpStatus.ACCEPTED);
    }

    @GetMapping("/getAllStatsOfMember")
    public ResponseEntity<?> getAllStatsOfMember(@RequestParam("o") Integer ownerId){
        return new ResponseEntity<>(memberService.getAllStatsOfMembers(ownerId), HttpStatus.ACCEPTED);
    }

    @GetMapping("/getLatestMemberExpiry")
    public ResponseEntity<?> getLatestMemberExpiry(@RequestParam("o") Integer ownerId){
        return new ResponseEntity<>(memberService.getLatestMemberExpiry(ownerId), HttpStatus.ACCEPTED);
    }

    @DeleteMapping("/deleteById")
    public ResponseEntity<?> deleteById(@RequestParam("q") Integer id){
        return new ResponseEntity<>(memberService.deleteById(id), HttpStatus.ACCEPTED);
    }

}

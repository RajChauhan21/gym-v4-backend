package com.backend.gym_backend.controller;

import com.backend.gym_backend.dto.MemberShipAdjustmentRequest;
import com.backend.gym_backend.service.MemberShipAdjustmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/member-ship-adjust")
public class MemberShipAdjustmentController {

    @Autowired
    private MemberShipAdjustmentService memberShipAdjustmentService;

    @PostMapping("/save")
    public ResponseEntity<?> saveUpdate(@RequestBody MemberShipAdjustmentRequest request){
        return new ResponseEntity<>(memberShipAdjustmentService.saveUpdate(request), HttpStatus.ACCEPTED);
    }

    @GetMapping("/getMembershipAdjustment")
    public ResponseEntity<?> getMemberShipAdjustmentByMemberId(@RequestParam("m") Integer memberId){
        return new ResponseEntity<>(memberShipAdjustmentService.getMemberShipAdjustmentByMemberId(memberId), HttpStatus.ACCEPTED);
    }

    @GetMapping("/deleteAdjustment")
    public ResponseEntity<?> deleteMemberShipAdjustmentByMemberId(@RequestParam("m") Integer memberId){
        return new ResponseEntity<>(memberShipAdjustmentService.deleteMembershipAdjustment(memberId), HttpStatus.ACCEPTED);
    }

    @GetMapping("/updateMemberStatus")
    public void updateMemberStatus(){
        memberShipAdjustmentService.updateMembersStatus();
        return;
    }


}

package com.backend.gym_backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/sec")
public class SecurityController {

    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("/exchange")
    public ResponseEntity<?> exchangeToken(@RequestParam("q") String code) {
        try {
            String tokenEndpoint = "https://oauth2.googleapis.com/token";

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();

            map.add("code", code);

            map.add("client_id", clientId);
            map.add("client_secret", secret);
            map.add("redirect_uri", "https://developers.google.com/oauthplayground");
            map.add("grant_type", "authorization_code");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity<MultiValueMap<String, String>> params = new HttpEntity<>(map, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenEndpoint, params, Map.class);
            String token = (String) response.getBody().get("id_token");

            String userInfoUrl = "https://oauth2.googleapis.com/tokeninfo?id_token="+token;
            ResponseEntity<Map> userInfo = restTemplate.getForEntity(userInfoUrl, Map.class);
            if (userInfo.getStatusCode() == HttpStatus.OK){
                Map body = userInfo.getBody();
                return new ResponseEntity<>(body,HttpStatus.ACCEPTED);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return null;
    }
}
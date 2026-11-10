package com.ecom.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    @GetMapping("/info")
    public ResponseEntity<Map<String, String>> aboutSystem(){
        String os = System.getProperty("os.name");
        String version = System.getProperty("os.version");
        String architecture = System.getProperty("os.arch");
        String javaVersion = System.getProperty("java.version");
        String user = System.getProperty("user.name");

        Map<String, String> systemMap = new HashMap<>();
        systemMap.put("OS", os);
        systemMap.put("Version", version);
        systemMap.put("Architecture", architecture);
        systemMap.put("javaVersion", javaVersion);
        systemMap.put("User", user);
        return new ResponseEntity<>(systemMap, HttpStatus.OK);

    }
}

package org.authzorium.client.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class AdminController {

     @GetMapping("/admin/hello")
     public ResponseEntity<String> adminHello() {
         return ResponseEntity.ok("admin-hello");
     }
 }

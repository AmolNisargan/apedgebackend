//package com.automationedge.apedge.controller;
//
//import com.automationedge.apedge.custom_Exceptions.AccountLockedException;
//import com.automationedge.apedge.custom_Exceptions.AuthenticationException;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.ExceptionHandler;
//import org.springframework.web.bind.annotation.RestControllerAdvice;
//
//import java.util.Map;
//
//@RestControllerAdvice
//public class GlobalExceptionHandler {
//
//    @ExceptionHandler(AccountLockedException.class)
//    public ResponseEntity<?> locked(AccountLockedException ex) {
//        return ResponseEntity.status(HttpStatus.LOCKED)
//                .body(Map.of(
//                        "status", "LOCKED",
//                        "message", ex.getMessage()
//                ));
//    }
//
//    @ExceptionHandler(AuthenticationException.class)
//    public ResponseEntity<?> auth(AuthenticationException ex) {
//        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                .body(Map.of(
//                        "status", "FAILED",
//                        "message", ex.getMessage()
//                ));
//    }
//
//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<?> general(Exception ex) {
//        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                .body(Map.of(
//                        "status", "ERROR",
//                        "message", "Unexpected error occurred"
//                ));
//    }
//}

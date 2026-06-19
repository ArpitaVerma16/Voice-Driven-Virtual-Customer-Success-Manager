package com.vcsm.dto;

import java.util.ArrayList;
import java.util.List;

public class ValidationErrorResponse extends ErrorResponse {
    private List<String> errors = new ArrayList<>();
    
    public ValidationErrorResponse() {
        super();
    }
    
    public ValidationErrorResponse(int status, String error, String userMessage, String path) {
        super(status, error, "Validation failed", userMessage, path);
    }
    
    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }
    
    public void addError(String error) {
        this.errors.add(error);
    }
}
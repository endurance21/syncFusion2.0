package com.syncfusion.exceptions;

import org.springframework.http.HttpStatus;

public class TJSException extends RuntimeException{
    HttpStatus statusCode;
    public TJSException(String message){
        super(message);
    }
    public TJSException(String message, HttpStatus statusCode){
        super(message);
        this.statusCode = statusCode;
    }
    public HttpStatus getStatusCode(){
        return statusCode;
    }
    public TJSException(HttpStatus statusCode, String message){
        super(message);
        this.statusCode = statusCode;
    }
    public TJSException(HttpStatus statusCode, String message, Throwable cause){
        super(message, cause);
        this.statusCode = statusCode;
    }

}

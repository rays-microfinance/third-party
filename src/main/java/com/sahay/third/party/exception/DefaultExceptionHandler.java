package com.sahay.third.party.exception;

import com.sahay.third.party.object.ErrorMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@ControllerAdvice
public class DefaultExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorMessage> handleCustomException(CustomException exception, HttpServletRequest request) {

        var errorMessage = new ErrorMessage();

        errorMessage.setResponse("004");
        errorMessage.setResponseMessage(exception.getMessage());
        errorMessage.setPath(request.getRequestURI());
        errorMessage.setDate(LocalDateTime.now());

        return new ResponseEntity<>(errorMessage, HttpStatus.OK);
    }
    
}

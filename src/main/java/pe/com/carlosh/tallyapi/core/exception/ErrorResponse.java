package pe.com.carlosh.tallyapi.core.exception;

import java.time.LocalDateTime;

public record ErrorResponse(
        int status,
        String message,
        String path,
        LocalDateTime timestamp
) {
    public ErrorResponse(int status, String message, String path){
        this(status,message,path,LocalDateTime.now());
    }
}

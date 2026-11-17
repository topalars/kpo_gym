package com.example.fitlife.exception;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.util.Map;
import org.slf4j.LoggerFactory;
@RestControllerAdvice
public class ApiErrors {
 @ExceptionHandler(ApiException.class) ResponseEntity<?> business(ApiException e){return error(e.status,e.getMessage());}
 @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<?> validation(MethodArgumentNotValidException e){return error(400,"Проверьте поля: "+e.getBindingResult().getFieldErrors().stream().map(x->x.getField()+" "+x.getDefaultMessage()).distinct().toList());}
 @ExceptionHandler({HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class}) ResponseEntity<?> input(Exception e){return error(400,"Некорректные данные, дата или формат запроса.");}
 @ExceptionHandler(DataIntegrityViolationException.class) ResponseEntity<?> integrity(Exception e){return error(409,"Запись уже существует или нарушает ограничения данных.");}
 @ExceptionHandler(Exception.class) ResponseEntity<?> other(Exception e){LoggerFactory.getLogger(getClass()).error("API request failed",e);return error(500,"Не удалось выполнить запрос. Повторите позднее.");}
 private ResponseEntity<?> error(int status,String message){return ResponseEntity.status(status).body(Map.of("message",message,"status",status));}
}

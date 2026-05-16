package com.jt.controllers;

import com.jt.audit.LogData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 *
 * A sample greetings controller to return greeting text
 */
@RestController
public class TestController {
    Logger logger= LoggerFactory.getLogger(TestController.class);
    /**
     *
     * @param name the name to greet
     * @return greeting text
     */
    @RequestMapping(value = "/{name}", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @LogData(traceId ="#correlationId",actionType="hit-hello")
    public CompletableFuture<String> greetingText(@PathVariable String name, @RequestHeader("x-correlation-id") String correlationId) throws InterruptedException {

        return CompletableFuture.completedFuture("Hello " + name + "!");
    }

    @RequestMapping(value = "/submit", method = RequestMethod.POST)
    //anotasi dimatikan akan lolos HttpWrapperFilter
    //@LogData(traceId ="#body['orderId']",actionType="submit")
    public ResponseEntity<Map<String,Object>> submit(@RequestBody Map<String,Object> body) throws InterruptedException {
        body.put("status","paid");
        return  new ResponseEntity<>(body,HttpStatus.OK);
    }
}

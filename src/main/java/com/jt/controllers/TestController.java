package com.jt.controllers;

import com.jt.audit.LogData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 *
 * A sample greetings controller to return greeting text
 */
@RestController
public class GreetingsController {
    Logger logger= LoggerFactory.getLogger(GreetingsController.class);
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
}

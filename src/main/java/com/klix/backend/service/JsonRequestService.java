package com.klix.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.klix.backend.exceptions.FaceException;

import java.net.ConnectException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;



@Slf4j
@Service
public class JsonRequestService
{
    
    public <T> T sendJsonRequest(String url, Object requestData, Class<T> returnFormat) throws JsonProcessingException, FaceException, ConnectException
    {
        String jsonRequestValue = "";
        
        try {
            jsonRequestValue = new ObjectMapper().writeValueAsString(requestData);
        } catch (JsonProcessingException e)
        {
            log.error("Could not parse programmatically constructed Request Object to Json. Will reraise an Exception since this should be fixed before runtime, So please do!");
            throw e;
        }
        
        // call facenet api
        WebClient wcClient = WebClient.builder()
                        .baseUrl(url)
                        .defaultCookie("APPKEY", "0")
                        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        // .filters(exchangeFilterFunctions -> {
                        //     exchangeFilterFunctions.add(logRequest());
                        //     exchangeFilterFunctions.add(logResponse()); })
                        .build();

        BodyInserter<String, ReactiveHttpOutputMessage> wcInserter = BodyInserters.fromValue(jsonRequestValue);

        
        try
        {
            return wcClient
                .post()
                .body(wcInserter)
                .retrieve()
                // http status == Bad Request: throws a FaceException
                .onStatus(HttpStatus.BAD_REQUEST::equals, response -> response.bodyToMono(String.class)
                                                   .flatMap(error -> Mono.error(new FaceException(error))))
                
                .bodyToMono(returnFormat)
                .cache()
                .block();
        // Http status does not equal bad request throw connectionException
        } catch (WebClientResponseException e) {
            log.error(e.getMessage());
            throw new ConnectException("Connection to ai failed");
        }
    }

   
    

    
    // /**
    //  * 
    //  */
    // public ExchangeFilterFunction logRequest()
    // {
    //     log.debug("WebClient logRequest filter start");
    //     return ExchangeFilterFunction.ofRequestProcessor(clientRequest ->
    //     {
    //         if (log.isDebugEnabled())
    //         {
    //             log.debug("log debug is enabled");
    //             StringBuilder sb = new StringBuilder("Request: \n");
    //             log.debug("stringbuilder: " + sb);

    //             append clientRequest method and url
    //             clientRequest
    //                 .headers()
    //                 .forEach((name, values) -> values.forEach(value -> log.debug("{}={}", name, values)));

    //             log.debug("Logging logRequest done");
    //         }

    //         return Mono.just(clientRequest);
    //     });
    // }


    // /**
    //  * 
    //  */
    // public ExchangeFilterFunction logResponse()
    // {
    //     log.debug("WebClient logResponse filter start");

    //     return ExchangeFilterFunction.ofResponseProcessor(clientResponse ->
    //     {
    //         log.debug("This is the Response: {}", clientResponse.statusCode());

    //         clientResponse
    //             .headers()
    //             .asHttpHeaders()
    //             .forEach((name, values) -> values.forEach(value -> log.debug("{}={}", name, value)));

    //         log.debug("Logging logResponse done" + clientResponse.headers().toString());

    //         return Mono.just(clientResponse);
    //     });
    // }
}

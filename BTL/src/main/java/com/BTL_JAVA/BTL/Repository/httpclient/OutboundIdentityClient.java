package com.BTL_JAVA.BTL.Repository.httpclient;

import com.BTL_JAVA.BTL.DTO.Request.Auth.ExchangeTokenRequest;
import com.BTL_JAVA.BTL.DTO.Response.Auth.ExchangeTokenReponse;
import feign.QueryMap;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "outbound-identity", url = "https://oauth2.googleapis.com")
public interface OutboundIdentityClient {
    @PostMapping(value = "/token", produces = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    ExchangeTokenReponse exchangeToken(@QueryMap ExchangeTokenRequest request);
}

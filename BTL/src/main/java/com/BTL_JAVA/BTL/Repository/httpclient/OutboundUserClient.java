package com.BTL_JAVA.BTL.Repository.httpclient;

import com.BTL_JAVA.BTL.DTO.Response.Auth.OutboundUserReponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "outbound-user-client", url = "https://www.googleapis.com")
public interface OutboundUserClient {
    @GetMapping(value = "/oauth2/v1/userinfo")
    OutboundUserReponse getUserInfo(@RequestParam("alt") String alt,
                                    @RequestParam("access_token") String accessToken);

}

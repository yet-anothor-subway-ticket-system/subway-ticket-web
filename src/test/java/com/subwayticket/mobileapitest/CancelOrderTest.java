package com.subwayticket.mobileapitest;

import com.subwayticket.model.request.CancelOrderRequest;
import com.subwayticket.model.request.LoginRequest;
import com.subwayticket.model.request.SubmitOrderRequest;
import com.subwayticket.model.result.MobileLoginResult;

import javax.ws.rs.core.Response;
import java.util.Scanner;

/**
 * Created by zhou-shengyun on 7/5/16.
 */
public class CancelOrderTest {
    public static void main(String[] argv) {
        System.out.println("****Cancel Order Test****");
        Scanner reader = new Scanner(System.in);
        String phoneNumber = reader.next();
        String password = reader.next();
        LoginRequest loginRequest = new LoginRequest(phoneNumber, password);
        Response response = RESTServiceTestUtil.put(RESTServiceTestUtil.API_BASE_URL_V1 + "/account/login", loginRequest, null);
        MobileLoginResult result = (MobileLoginResult) RESTServiceTestUtil.showResponse(response, MobileLoginResult.class);
        if(result.getToken() != null)
            System.out.println("Token:" + result.getToken());
        else
            return;
        String orderID = reader.next();
        CancelOrderRequest cancelOrderRequest = new CancelOrderRequest(orderID);
        response = RESTServiceTestUtil.put(RESTServiceTestUtil.API_BASE_URL_V1 + "/ticket_order/cancel", cancelOrderRequest,
                result.getToken());
        RESTServiceTestUtil.showResponse(response);
        response = RESTServiceTestUtil.put(RESTServiceTestUtil.API_BASE_URL_V1 + "/account/logout", null,
                result.getToken());
        RESTServiceTestUtil.showResponse(response);
    }
}

/*
 * Copyright 2008-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.broadleafcommerce.payment.service.gateway;

import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.broadleafcommerce.common.money.Money;
import org.broadleafcommerce.common.payment.PaymentTransactionType;
import org.broadleafcommerce.common.payment.PaymentType;
import org.broadleafcommerce.common.payment.dto.PaymentResponseDTO;
import org.broadleafcommerce.common.payment.service.PaymentGatewayWebResponsePrintService;
import org.broadleafcommerce.common.payment.service.PaymentGatewayWebResponseService;
import org.broadleafcommerce.common.vendor.service.exception.PaymentException;
import org.broadleafcommerce.vendor.authorizenet.service.payment.AuthorizeNetGatewayType;
import org.broadleafcommerce.vendor.authorizenet.service.payment.type.MessageConstants;
import org.springframework.stereotype.Service;

/**
 * @author Chad Harchar (charchar)
 */
@Service("blAuthorizeNetWebResponseService")
public class AuthorizeNetWebResponseServiceImpl implements PaymentGatewayWebResponseService {

    @Resource(name = "blAuthorizeNetConfiguration")
    protected AuthorizeNetConfiguration configuration;
    
    @Resource(name = "blPaymentGatewayWebResponsePrintService")
    protected PaymentGatewayWebResponsePrintService webResponsePrintService;

    @Override
    public PaymentResponseDTO translateWebResponse(HttpServletRequest request) throws PaymentException {
      //NOTE: assumes only one payment info of type credit card on the order.
        //Start by removing any payment info of type credit card already on the order.
        //orderService.removePaymentsFromOrder(order, PaymentInfoType.CREDIT_CARD);

//        Map<PaymentInfo, Referenced> payments = paymentInfoTypeService.getPaymentsMap(order);
//        PaymentInfo authorizeNetPaymentInfo = paymentInfoService.create();
//        authorizeNetPaymentInfo.setOrder(order);
//        authorizeNetPaymentInfo.setType(PaymentInfoType.CREDIT_CARD);
//        authorizeNetPaymentInfo.setReferenceNumber(UUID.randomUUID().toString());
//        authorizeNetPaymentInfo.setAmount(new Money(responseMap.get(ResponseField.AMOUNT.getFieldName())[0]));
//        authorizeNetPaymentInfo.setRequestParameterMap(responseMap);
//
//        //finally add the authorizenet payment info to the order
//        order.getPaymentInfos().add(authorizeNetPaymentInfo);
//
//        CreditCardPaymentInfo creditCardPaymentInfo = ((CreditCardPaymentInfo) securePaymentInfoService.create(PaymentInfoType.CREDIT_CARD));
//        creditCardPaymentInfo.setReferenceNumber(authorizeNetPaymentInfo.getReferenceNumber());
//        payments.put(authorizeNetPaymentInfo, creditCardPaymentInfo);
        System.out.println("request: " + request);
        System.out.println("requestmap: " + webResponsePrintService.printRequest(request));
        
        
        PaymentResponseDTO responseDTO = new PaymentResponseDTO(PaymentType.CREDIT_CARD,
                AuthorizeNetGatewayType.AUTHORIZENET)
                .rawResponse(webResponsePrintService.printRequest(request));
//                .valid(verifySignature(request));

        Map<String,String[]> paramMap = request.getParameterMap();
        for (String key : paramMap.keySet())  {
            responseDTO.responseMap(key, paramMap.get(key)[0]);
        }

        Map<String, String> params = responseDTO.getResponseMap();

        Money amount = Money.ZERO;
        if (responseDTO.getResponseMap().containsKey(MessageConstants.X_AMOUNT)) {
            String amt = responseDTO.getResponseMap().get(MessageConstants.X_AMOUNT);
            amount = new Money(amt);
        }

        boolean approved = false;
        if (params.get(MessageConstants.X_RESPONSE_CODE).equals("1")) {
            approved = true;
        }
        System.out.println(params.get(MessageConstants.X_RESPONSE_CODE));
        System.out.println(params.get(MessageConstants.X_RESPONSE_CODE).equals("1"));
        System.out.println(approved);

        PaymentTransactionType type = PaymentTransactionType.AUTHORIZE_AND_CAPTURE;
        if (!configuration.isPerformAuthorizeAndCapture()) {
            type = PaymentTransactionType.AUTHORIZE;
        }
        
        responseDTO.successful(approved)
        .amount(amount)
        .paymentTransactionType(type)
        .orderId(params.get(MessageConstants.X_TRANS_ID))
        .customer()
            .firstName(params.get(MessageConstants.X_FIRST_NAME))
            .lastName(params.get(MessageConstants.X_LAST_NAME))
            .customerId(MessageConstants.X_CUST_ID)
            .done()
        .billTo()
            .addressFirstName(params.get(MessageConstants.X_FIRST_NAME))
            .addressLastName(params.get(MessageConstants.X_LAST_NAME))
            .addressLine1(params.get(MessageConstants.X_ADDRESS))
            .addressCityLocality(params.get(MessageConstants.X_CITY))
            .addressStateRegion(params.get(MessageConstants.X_STATE))
            .addressPostalCode(params.get(MessageConstants.X_ZIP))
            .addressCountryCode(params.get(MessageConstants.X_COUNTRY))
            .addressPhone(params.get(MessageConstants.X_PHONE))
            .done()
        .creditCard()
            .creditCardLastFour(params.get(MessageConstants.X_ACCOUNT_NUMBER))
            .creditCardType(params.get(MessageConstants.X_CARD_TYPE));

        return responseDTO;
    }
    

}

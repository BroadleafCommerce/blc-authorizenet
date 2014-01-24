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

import net.authorize.AuthNetField;

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
        PaymentResponseDTO responseDTO = new PaymentResponseDTO(PaymentType.CREDIT_CARD,
                AuthorizeNetGatewayType.AUTHORIZENET)
                .rawResponse(webResponsePrintService.printRequest(request));

        Map<String,String[]> paramMap = request.getParameterMap();
        for (String key : paramMap.keySet())  {
            responseDTO.responseMap(key, paramMap.get(key)[0]);
        }

        Map<String, String> params = responseDTO.getResponseMap();

        Money amount = Money.ZERO;
        if (responseDTO.getResponseMap().containsKey(AuthNetField.X_AMOUNT.getFieldName())) {
            String amt = responseDTO.getResponseMap().get(AuthNetField.X_AMOUNT.getFieldName());
            amount = new Money(amt);
        }

        boolean approved = false;
        if (params.get(AuthNetField.X_RESPONSE_CODE.getFieldName()).equals("1")) {
            approved = true;
        }

        PaymentTransactionType type = PaymentTransactionType.AUTHORIZE_AND_CAPTURE;
        if (!configuration.isPerformAuthorizeAndCapture()) {
            type = PaymentTransactionType.AUTHORIZE;
        }

        responseDTO.successful(approved)
        .amount(amount)
        .paymentTransactionType(type)
        .orderId(params.get(MessageConstants.BLC_OID))
        .customer()
            .firstName(params.get(AuthNetField.X_FIRST_NAME.getFieldName()))
            .lastName(params.get(AuthNetField.X_LAST_NAME.getFieldName()))
            .customerId(params.get(AuthNetField.X_CUST_ID.getFieldName()))
            .done()
        .billTo()
            .addressFirstName(params.get(AuthNetField.X_FIRST_NAME.getFieldName()))
            .addressLastName(params.get(AuthNetField.X_LAST_NAME.getFieldName()))
            .addressLine1(params.get(AuthNetField.X_ADDRESS.getFieldName()))
            .addressCityLocality(params.get(AuthNetField.X_CITY.getFieldName()))
            .addressStateRegion(params.get(AuthNetField.X_STATE.getFieldName()))
            .addressPostalCode(params.get(AuthNetField.X_ZIP.getFieldName()))
            .addressCountryCode(params.get(AuthNetField.X_COUNTRY.getFieldName()))
            .addressPhone(params.get(AuthNetField.X_PHONE.getFieldName()))
            .done()
        .creditCard()
            .creditCardLastFour(params.get(AuthNetField.X_ACCOUNT_NUMBER.getFieldName()))
            .creditCardType(params.get(AuthNetField.X_CARD_TYPE.getFieldName()));

        return responseDTO;
    }

}

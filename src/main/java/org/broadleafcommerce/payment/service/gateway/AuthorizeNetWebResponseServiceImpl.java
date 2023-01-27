/*-
 * #%L
 * BroadleafCommerce Authorize.net
 * %%
 * Copyright (C) 2009 - 2023 Broadleaf Commerce
 * %%
 * Licensed under the Broadleaf Fair Use License Agreement, Version 1.0
 * (the "Fair Use License" located  at http://license.broadleafcommerce.org/fair_use_license-1.0.txt)
 * unless the restrictions on use therein are violated and require payment to Broadleaf in which case
 * the Broadleaf End User License Agreement (EULA), Version 1.1
 * (the "Commercial License" located at http://license.broadleafcommerce.org/commercial_license-1.1.txt)
 * shall apply.
 * 
 * Alternatively, the Commercial License may be replaced with a mutually agreed upon license (the "Custom License")
 * between you and Broadleaf Commerce. You may not use this file except in compliance with the applicable license.
 * #L%
 */
package org.broadleafcommerce.payment.service.gateway;

import org.broadleafcommerce.common.money.Money;
import org.broadleafcommerce.common.payment.PaymentTransactionType;
import org.broadleafcommerce.common.payment.PaymentType;
import org.broadleafcommerce.common.payment.dto.PaymentResponseDTO;
import org.broadleafcommerce.common.payment.service.AbstractPaymentGatewayWebResponseService;
import org.broadleafcommerce.common.payment.service.PaymentGatewayWebResponsePrintService;
import org.broadleafcommerce.common.payment.service.PaymentGatewayWebResponseService;
import org.broadleafcommerce.common.vendor.service.exception.PaymentException;
import org.broadleafcommerce.vendor.authorizenet.service.payment.AuthorizeNetCheckoutService;
import org.broadleafcommerce.vendor.authorizenet.service.payment.AuthorizeNetGatewayType;
import org.broadleafcommerce.vendor.authorizenet.service.payment.type.MessageConstants;
import org.springframework.stereotype.Service;

import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import net.authorize.AuthNetField;

/**
 * @author Chad Harchar (charchar)
 */
@Deprecated
@Service("blAuthorizeNetWebResponseService")
public class AuthorizeNetWebResponseServiceImpl extends AbstractPaymentGatewayWebResponseService implements PaymentGatewayWebResponseService {

    @Resource(name = "blAuthorizeNetConfiguration")
    protected AuthorizeNetConfiguration configuration;
    
    @Resource(name = "blPaymentGatewayWebResponsePrintService")
    protected PaymentGatewayWebResponsePrintService webResponsePrintService;
    
    @Resource(name = "blAuthorizeNetCheckoutService")
    protected AuthorizeNetCheckoutService authorizeNetCheckoutService;

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

        // Validate this is a real request from Authorize.net
        String customerId = responseDTO.getResponseMap().get(MessageConstants.BLC_CID);
        String orderId = responseDTO.getResponseMap().get(MessageConstants.BLC_OID);
        String tps = responseDTO.getResponseMap().get(MessageConstants.BLC_TPS);
        responseDTO.valid(authorizeNetCheckoutService.verifyTamperProofSeal(customerId, orderId, tps));

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

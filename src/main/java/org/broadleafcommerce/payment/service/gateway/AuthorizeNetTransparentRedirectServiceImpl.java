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

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.annotation.Resource;

import net.authorize.sim.Fingerprint;

import org.broadleafcommerce.common.payment.PaymentType;
import org.broadleafcommerce.common.payment.dto.PaymentRequestDTO;
import org.broadleafcommerce.common.payment.dto.PaymentResponseDTO;
import org.broadleafcommerce.common.payment.service.PaymentGatewayTransparentRedirectService;
import org.broadleafcommerce.common.vendor.service.exception.PaymentException;
import org.broadleafcommerce.vendor.authorizenet.service.payment.AuthorizeNetCheckoutService;
import org.broadleafcommerce.vendor.authorizenet.service.payment.AuthorizeNetGatewayType;
import org.broadleafcommerce.vendor.authorizenet.service.payment.type.MessageConstants;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * @author Chad Harchar (charchar)
 */
@Service("blAuthorizeNetTransparentRedirectService")
public class AuthorizeNetTransparentRedirectServiceImpl implements PaymentGatewayTransparentRedirectService {

    @Resource(name = "blAuthorizeNetConfiguration")
    protected AuthorizeNetConfiguration configuration;

    @Resource(name="blAuthorizeNetCheckoutService")
    protected AuthorizeNetCheckoutService authorizeNetCheckoutService;
    
    private static final String AUTH_ONLY = "AUTH_ONLY";
    private static final String AUTH_CAPTURE = "AUTH_CAPTURE";

    @Override
    public PaymentResponseDTO createAuthorizeForm(PaymentRequestDTO requestDTO) throws PaymentException {
        return common(requestDTO, false);
    }

    @Override
    public PaymentResponseDTO createAuthorizeAndCaptureForm(PaymentRequestDTO requestDTO) throws PaymentException {
        return common(requestDTO, true);
    }

    public PaymentResponseDTO common(PaymentRequestDTO requestDTO, Boolean submitForSettlement) {
        Assert.isTrue(requestDTO.getOrderId() != null,
                "Must pass an OrderId value on the Payment Request DTO");
        Assert.isTrue(requestDTO.getTransactionTotal() != null,
                "Must pass a Transaction Total value on the Payment Request DTO");

        String apiLoginId = configuration.getLoginId();
        String transactionKey = configuration.getTransactionKey();
        String relayResponseURL = configuration.getResponseUrl();
        String merchantTransactionVersion = configuration.getTransactionVersion();
        String xTestRequest = configuration.getXTestRequest();
        String serverUrl = configuration.getServerUrl();
        String custId = requestDTO.getCustomer().getCustomerId();
        String orderId = requestDTO.getOrderId();

        Fingerprint fingerprint = Fingerprint.createFingerprint(apiLoginId, transactionKey, System.currentTimeMillis(), requestDTO.getTransactionTotal());
        PaymentResponseDTO responseDTO = new PaymentResponseDTO(PaymentType.CREDIT_CARD, AuthorizeNetGatewayType.AUTHORIZENET)
        .responseMap(MessageConstants.X_INVOICE_NUM, System.currentTimeMillis()+"")
        .responseMap(MessageConstants.X_RELAY_URL, relayResponseURL)
        .responseMap(MessageConstants.X_LOGIN, apiLoginId)
        .responseMap(MessageConstants.X_FP_SEQUENCE, fingerprint.getSequence()+"")
        .responseMap(MessageConstants.X_FP_TIMESTAMP, fingerprint.getTimeStamp()+"")
        .responseMap(MessageConstants.X_FP_HASH, fingerprint.getFingerprintHash())
        .responseMap(MessageConstants.X_VERSION, merchantTransactionVersion)
        .responseMap(MessageConstants.X_METHOD, "CC")
        .responseMap(MessageConstants.X_TYPE, submitForSettlement ? AUTH_CAPTURE : AUTH_ONLY)
        .responseMap(MessageConstants.X_AMOUNT, requestDTO.getTransactionTotal())
        .responseMap(MessageConstants.X_TEST_REQUEST, xTestRequest)
        .responseMap(MessageConstants.X_RELAY_RESPONSE, "true")
        .responseMap(MessageConstants.BLC_CID, custId)
        .responseMap(MessageConstants.BLC_OID, orderId)
        .responseMap(MessageConstants.X_CUST_ID, custId)
        .responseMap(MessageConstants.X_TRANS_ID, orderId)
        .responseMap(MessageConstants.AUTHORIZENET_SERVER_URL, serverUrl);

        if(requestDTO.billToPopulated()) {
            responseDTO.responseMap(MessageConstants.X_FIRST_NAME, requestDTO.getBillTo().getAddressFirstName())
            .responseMap(MessageConstants.X_LAST_NAME, requestDTO.getBillTo().getAddressLastName())
            .responseMap(MessageConstants.X_ADDRESS, requestDTO.getBillTo().getAddressLine1())
            .responseMap(MessageConstants.X_CITY, requestDTO.getBillTo().getAddressCityLocality())
            .responseMap(MessageConstants.X_STATE, requestDTO.getBillTo().getAddressStateRegion())
            .responseMap(MessageConstants.X_ZIP, requestDTO.getBillTo().getAddressPostalCode())
            .responseMap(MessageConstants.X_COUNTRY, requestDTO.getBillTo().getAddressCountryCode())
            .responseMap(MessageConstants.X_EMAIL, requestDTO.getBillTo().getAddressEmail() != null ? requestDTO.getBillTo().getAddressEmail() : requestDTO.getCustomer().getEmail())
            .responseMap(MessageConstants.X_PHONE, requestDTO.getBillTo().getAddressPhone());
            
        }

        for(String fieldKey : requestDTO.getAdditionalFields().keySet()) {
            responseDTO.responseMap(fieldKey, (String)requestDTO.getAdditionalFields().get(fieldKey));
        }

        try {
            responseDTO.responseMap(MessageConstants.BLC_TPS, authorizeNetCheckoutService.createTamperProofSeal(custId, orderId));
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return responseDTO;
    }
}

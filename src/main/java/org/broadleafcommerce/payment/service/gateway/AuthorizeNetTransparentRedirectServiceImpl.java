/*
 * #%L
 * BroadleafCommerce Authorize.net
 * %%
 * Copyright (C) 2009 - 2014 Broadleaf Commerce
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.broadleafcommerce.payment.service.gateway;

import net.authorize.AuthNetField;
import net.authorize.sim.Fingerprint;

import org.broadleafcommerce.common.payment.PaymentType;
import org.broadleafcommerce.common.payment.dto.PaymentRequestDTO;
import org.broadleafcommerce.common.payment.dto.PaymentResponseDTO;
import org.broadleafcommerce.common.payment.service.AbstractPaymentGatewayTransparentRedirectService;
import org.broadleafcommerce.common.payment.service.PaymentGatewayTransparentRedirectService;
import org.broadleafcommerce.common.vendor.service.exception.PaymentException;
import org.broadleafcommerce.vendor.authorizenet.service.payment.AuthorizeNetCheckoutService;
import org.broadleafcommerce.vendor.authorizenet.service.payment.AuthorizeNetGatewayType;
import org.broadleafcommerce.vendor.authorizenet.service.payment.type.MessageConstants;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.annotation.Resource;

/**
 * @author Chad Harchar (charchar)
 */
@Service("blAuthorizeNetTransparentRedirectService")
public class AuthorizeNetTransparentRedirectServiceImpl extends AbstractPaymentGatewayTransparentRedirectService implements PaymentGatewayTransparentRedirectService {

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
        String overrideRelayUrl = (String) requestDTO.getAdditionalFields().get("x_relay_url");
        String relayResponseURL = (overrideRelayUrl != null) ? overrideRelayUrl : configuration.getResponseUrl();
        String merchantTransactionVersion = configuration.getTransactionVersion();
        String xTestRequest = configuration.getXTestRequest();
        String serverUrl = configuration.getServerUrl();
        String custId = requestDTO.getCustomer().getCustomerId();
        String orderId = requestDTO.getOrderId();

        Fingerprint fingerprint = Fingerprint.createFingerprint(apiLoginId, transactionKey, System.currentTimeMillis(), requestDTO.getTransactionTotal());
        PaymentResponseDTO responseDTO = new PaymentResponseDTO(PaymentType.CREDIT_CARD, AuthorizeNetGatewayType.AUTHORIZENET)
        .responseMap(AuthNetField.X_INVOICE_NUM.getFieldName(), System.currentTimeMillis()+"")
        .responseMap(AuthNetField.X_RELAY_URL.getFieldName(), relayResponseURL)
        .responseMap(AuthNetField.X_LOGIN.getFieldName(), apiLoginId)
        .responseMap(AuthNetField.X_FP_SEQUENCE.getFieldName(), fingerprint.getSequence()+"")
        .responseMap(AuthNetField.X_FP_TIMESTAMP.getFieldName(), fingerprint.getTimeStamp()+"")
        .responseMap(AuthNetField.X_FP_HASH.getFieldName(), fingerprint.getFingerprintHash())
        .responseMap(AuthNetField.X_VERSION_FIELD.getFieldName(), merchantTransactionVersion)
        .responseMap(AuthNetField.X_METHOD.getFieldName(), "CC")
        .responseMap(AuthNetField.X_TYPE.getFieldName(), submitForSettlement ? AUTH_CAPTURE : AUTH_ONLY)
        .responseMap(AuthNetField.X_AMOUNT.getFieldName(), requestDTO.getTransactionTotal())
        .responseMap(AuthNetField.X_TEST_REQUEST.getFieldName(), xTestRequest)
        .responseMap(AuthNetField.X_RELAY_RESPONSE.getFieldName(), "true")
        .responseMap(AuthNetField.X_CUST_ID.getFieldName(), custId)
        .responseMap(AuthNetField.X_TRANS_ID.getFieldName(), orderId)
        .responseMap(MessageConstants.BLC_CID, custId)
        .responseMap(MessageConstants.BLC_OID, orderId)
        .responseMap(MessageConstants.AUTHORIZENET_SERVER_URL, serverUrl);

        if(requestDTO.billToPopulated()) {
            responseDTO.responseMap(AuthNetField.X_FIRST_NAME.getFieldName(), requestDTO.getBillTo().getAddressFirstName())
            .responseMap(AuthNetField.X_LAST_NAME.getFieldName(), requestDTO.getBillTo().getAddressLastName())
            .responseMap(AuthNetField.X_ADDRESS.getFieldName(), requestDTO.getBillTo().getAddressLine1())
            .responseMap(AuthNetField.X_CITY.getFieldName(), requestDTO.getBillTo().getAddressCityLocality())
            .responseMap(AuthNetField.X_STATE.getFieldName(), requestDTO.getBillTo().getAddressStateRegion())
            .responseMap(AuthNetField.X_ZIP.getFieldName(), requestDTO.getBillTo().getAddressPostalCode())
            .responseMap(AuthNetField.X_COUNTRY.getFieldName(), requestDTO.getBillTo().getAddressCountryCode())
            .responseMap(AuthNetField.X_EMAIL.getFieldName(), requestDTO.getBillTo().getAddressEmail() != null ? requestDTO.getBillTo().getAddressEmail() : requestDTO.getCustomer().getEmail())
            .responseMap(AuthNetField.X_PHONE.getFieldName(), requestDTO.getBillTo().getAddressPhone());
            
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

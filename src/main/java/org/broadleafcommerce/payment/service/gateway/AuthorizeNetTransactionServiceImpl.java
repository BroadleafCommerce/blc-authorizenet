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

import org.broadleafcommerce.common.money.Money;
import org.broadleafcommerce.common.payment.PaymentTransactionType;
import org.broadleafcommerce.common.payment.PaymentType;
import org.broadleafcommerce.common.payment.dto.PaymentRequestDTO;
import org.broadleafcommerce.common.payment.dto.PaymentResponseDTO;
import org.broadleafcommerce.common.payment.service.PaymentGatewayTransactionService;
import org.broadleafcommerce.common.time.SystemTime;
import org.broadleafcommerce.common.vendor.service.exception.PaymentException;
import org.broadleafcommerce.vendor.authorizenet.service.payment.AuthorizeNetGatewayType;
import org.broadleafcommerce.vendor.authorizenet.service.payment.type.MessageConstants;
import org.broadleafcommerce.vendor.authorizenet.util.AuthorizeNetUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Resource;

import net.authorize.AuthNetField;
import net.authorize.Environment;
import net.authorize.Merchant;
import net.authorize.ResponseField;
import net.authorize.TransactionType;
import net.authorize.aim.Result;
import net.authorize.aim.Transaction;
import net.authorize.api.contract.v1.CreateTransactionRequest;
import net.authorize.api.contract.v1.CreateTransactionResponse;
import net.authorize.api.contract.v1.MerchantAuthenticationType;
import net.authorize.api.contract.v1.MessageTypeEnum;
import net.authorize.api.contract.v1.OpaqueDataType;
import net.authorize.api.contract.v1.TransactionRequestType;
import net.authorize.api.contract.v1.TransactionTypeEnum;
import net.authorize.api.controller.CreateTransactionController;
import net.authorize.api.controller.base.ApiOperationBase;
import net.authorize.data.Order;
import net.authorize.data.ShippingCharges;
import net.authorize.data.cim.PaymentTransaction;
import net.authorize.data.creditcard.CreditCard;

@Service("blAuthorizeNetTransactionService")
public class AuthorizeNetTransactionServiceImpl implements PaymentGatewayTransactionService {

    @Resource(name = "blAuthorizeNetConfiguration")
    protected AuthorizeNetConfiguration configuration;
    
    @Resource
    protected AuthorizeNetUtil util;

    @Override
    public PaymentResponseDTO authorize(PaymentRequestDTO paymentRequestDTO) throws PaymentException {
        return common(paymentRequestDTO, TransactionType.AUTH_ONLY, PaymentTransactionType.AUTHORIZE);
    }

    @Override
    public PaymentResponseDTO capture(PaymentRequestDTO paymentRequestDTO) throws PaymentException {
        Assert.isTrue(paymentRequestDTO.getAdditionalFields().containsKey(ResponseField.TRANSACTION_ID.getFieldName()),
                "Must pass 'x_trans_id' value on the additionalFields of the Payment Request DTO");
        Assert.isTrue(paymentRequestDTO.getTransactionTotal() != null,
                "The Transaction Total must not be null on the Payment Request DTO");
        return common(paymentRequestDTO, TransactionType.PRIOR_AUTH_CAPTURE, PaymentTransactionType.CAPTURE);
    }

    @Override
    public PaymentResponseDTO authorizeAndCapture(PaymentRequestDTO paymentRequestDTO) throws PaymentException {
        return common(paymentRequestDTO, TransactionType.AUTH_CAPTURE, PaymentTransactionType.AUTHORIZE_AND_CAPTURE);
    }

    @Override
    public PaymentResponseDTO reverseAuthorize(PaymentRequestDTO paymentRequestDTO) throws PaymentException {
        return voidPayment(paymentRequestDTO);
    }

    @Override
    public PaymentResponseDTO refund(PaymentRequestDTO paymentRequestDTO) throws PaymentException {
        Assert.isTrue(paymentRequestDTO.getAdditionalFields().containsKey(AuthNetField.X_TRANS_ID.getFieldName()),
                "Must pass 'x_trans_id' value on the additionalFields of the Payment Request DTO");
        Assert.isTrue(paymentRequestDTO.getTransactionTotal() != null,
                "The Transaction Total must not be null on the Payment Request DTO");
        Boolean cardNumOrLastFourPopulated = paymentRequestDTO.creditCardPopulated() &&
                (paymentRequestDTO.getCreditCard().getCreditCardLastFour() != null || paymentRequestDTO.getCreditCard().getCreditCardNum() != null);
        Assert.isTrue(cardNumOrLastFourPopulated || (paymentRequestDTO.getAdditionalFields().get(AuthNetField.X_CARD_NUM.getFieldName()) != null),
                "Must pass the Last four card number digits on the credit card of the Payment Request DTO");
        return common(paymentRequestDTO, TransactionType.CREDIT, PaymentTransactionType.REFUND);
    }

    @Override
    public PaymentResponseDTO voidPayment(PaymentRequestDTO paymentRequestDTO) throws PaymentException {
        Assert.isTrue(paymentRequestDTO.getAdditionalFields().containsKey(AuthNetField.X_TRANS_ID.getFieldName()),
                "Must pass 'x_trans_id' value on the additionalFields of the Payment Request DTO");
        return common(paymentRequestDTO, TransactionType.VOID, PaymentTransactionType.VOID);
    }

    private PaymentResponseDTO common(PaymentRequestDTO paymentRequestDTO, TransactionType transactionType, PaymentTransactionType paymentTransactionType) {
        Environment e = Environment.createEnvironment(configuration.getServerUrl(), configuration.getXMLBaseUrl());
        Merchant merchant = Merchant.createMerchant(e, configuration.getLoginId(), configuration.getTransactionKey());

        PaymentResponseDTO responseDTO = new PaymentResponseDTO(PaymentType.CREDIT_CARD, AuthorizeNetGatewayType.AUTHORIZENET);
        
        parseOutConsolidatedTokenField(paymentRequestDTO);
        
        // Use the CIM API to send this transaction using the saved information
        if (paymentRequestDTO.getAdditionalFields().containsKey(MessageConstants.CUSTOMER_PROFILE_ID)
                && paymentRequestDTO.getAdditionalFields().containsKey(MessageConstants.PAYMENT_PROFILE_ID)) {
            
            net.authorize.cim.Transaction transaction = merchant.createCIMTransaction(net.authorize.cim.TransactionType.CREATE_CUSTOMER_PROFILE_TRANSACTION);
            transaction.setCustomerProfileId((String) paymentRequestDTO.getAdditionalFields().get(MessageConstants.CUSTOMER_PROFILE_ID));
            
            PaymentTransaction paymentTransaction = PaymentTransaction.createPaymentTransaction();
            transaction.setPaymentTransaction(paymentTransaction);
            paymentTransaction.setTransactionType(transactionType);
            paymentTransaction.setCustomerPaymentProfileId((String) paymentRequestDTO.getAdditionalFields().get(MessageConstants.PAYMENT_PROFILE_ID));
            
            Order order = Order.createOrder();
            paymentTransaction.setOrder(order);
            order.setTotalAmount(new BigDecimal(paymentRequestDTO.getTransactionTotal()));
            order.setInvoiceNumber(System.currentTimeMillis() + "");
            
            ShippingCharges shipping = ShippingCharges.createShippingCharges();
            order.setShippingCharges(shipping);
            shipping.setFreightAmount(paymentRequestDTO.getShippingTotal());
            shipping.setTaxAmount(paymentRequestDTO.getTaxTotal());
            
            // Submit the transaction
            net.authorize.cim.Result<net.authorize.cim.Transaction> gatewayResult = 
                    (net.authorize.cim.Result<net.authorize.cim.Transaction>)merchant.postTransaction(transaction);
            
            responseDTO.successful(gatewayResult.isOk());
            responseDTO.rawResponse(gatewayResult.getTarget().getCurrentResponse().dump());
            responseDTO.orderId(paymentRequestDTO.getOrderId());
            Map<ResponseField, String> responseMap = gatewayResult.getDirectResponseList().get(0).getDirectResponseMap();
            responseDTO.creditCard()
                .creditCardLastFour(responseMap.get(ResponseField.ACCOUNT_NUMBER))
                .creditCardType(responseMap.get(ResponseField.CARD_TYPE));
            responseDTO.amount(new Money(responseMap.get(ResponseField.AMOUNT)));
            responseDTO.customer().email(responseMap.get(ResponseField.EMAIL_ADDRESS));
            responseDTO.paymentTransactionType(paymentTransactionType);
        } else {
            if (paymentRequestDTO.getAdditionalFields().containsKey("OPAQUE_DATA_DESCRIPTOR")) {
                TransactionRequestType transaction = new TransactionRequestType();
                ApiOperationBase.setEnvironment(Environment.SANDBOX);
                MerchantAuthenticationType merchantAuthenticationType  = new MerchantAuthenticationType() ;
                merchantAuthenticationType.setName(configuration.getLoginId());
                merchantAuthenticationType.setTransactionKey(configuration.getTransactionKey());
                ApiOperationBase.setMerchantAuthentication(merchantAuthenticationType);

                net.authorize.api.contract.v1.PaymentType paymentType = new net.authorize.api.contract.v1.PaymentType();
                OpaqueDataType data = new OpaqueDataType();
                data.setDataDescriptor((String)paymentRequestDTO.getAdditionalFields().get("OPAQUE_DATA_DESCRIPTOR"));
                data.setDataValue((String)paymentRequestDTO.getAdditionalFields().get("OPAQUE_DATA_VALUE"));
                paymentType.setOpaqueData(data);
                if (transactionType.equals(TransactionType.AUTH_CAPTURE)) {
                    transaction.setTransactionType(TransactionTypeEnum.AUTH_CAPTURE_TRANSACTION.value());
                } else if (transactionType.equals(TransactionType.AUTH_ONLY)) {
                    transaction.setTransactionType(TransactionTypeEnum.AUTH_ONLY_TRANSACTION.value());
                }
                transaction.setPayment(paymentType);
                transaction.setAmount(new BigDecimal(paymentRequestDTO.getTransactionTotal()));
                CreateTransactionRequest apiRequest = new CreateTransactionRequest();
                apiRequest.setTransactionRequest(transaction);
                
                CreateTransactionController controller = new CreateTransactionController(apiRequest);
                controller.execute();
                
                CreateTransactionResponse response = controller.getApiResponse();
                
                responseDTO.paymentTransactionType(paymentTransactionType);
                responseDTO.amount(new Money(paymentRequestDTO.getTransactionTotal().toString()));
                responseDTO.orderId(paymentRequestDTO.getOrderId());
                responseDTO.responseMap(MessageConstants.TRANSACTION_TIME, SystemTime.asDate().toString());
                responseDTO.responseMap(ResponseField.RESPONSE_CODE.getFieldName(), "" + response.getTransactionResponse().getResponseCode());
                responseDTO.responseMap(ResponseField.RESPONSE_REASON_CODE.getFieldName(), "" + response.getTransactionResponse().getRawResponseCode());
                responseDTO.responseMap(ResponseField.AMOUNT.getFieldName(), paymentRequestDTO.getTransactionTotal().toString());
                responseDTO.responseMap(ResponseField.AUTHORIZATION_CODE.getFieldName(), response.getTransactionResponse().getAuthCode());
                responseDTO.responseMap(ResponseField.ACCOUNT_NUMBER.getFieldName(), response.getTransactionResponse().getAccountNumber())
                           .responseMap(AuthNetField.X_INVOICE_NUM.getFieldName(), System.currentTimeMillis()+"")
                           .responseMap(AuthNetField.X_LOGIN.getFieldName(), configuration.getLoginId())
                           .responseMap(AuthNetField.X_VERSION_FIELD.getFieldName(), configuration.getTransactionVersion())
                           .responseMap(AuthNetField.X_METHOD.getFieldName(), "CC")
                           .responseMap(AuthNetField.X_TYPE.getFieldName(), transactionType.getValue())
                           .responseMap(AuthNetField.X_AMOUNT.getFieldName(), paymentRequestDTO.getTransactionTotal())
                           .responseMap(AuthNetField.X_TEST_REQUEST.getFieldName(), configuration.getXTestRequest())
                           .responseMap(AuthNetField.X_CUST_ID.getFieldName(), paymentRequestDTO.getCustomer().getCustomerId())
                           .responseMap(AuthNetField.X_TRANS_ID.getFieldName(), paymentRequestDTO.getOrderId())
                           .responseMap(MessageConstants.BLC_CID, paymentRequestDTO.getCustomer().getCustomerId())
                           .responseMap(MessageConstants.BLC_OID, paymentRequestDTO.getOrderId())
                           .responseMap(MessageConstants.AUTHORIZENET_SERVER_URL, configuration.getServerUrl());
                
                if(paymentRequestDTO.billToPopulated()) {
                    responseDTO.responseMap(AuthNetField.X_FIRST_NAME.getFieldName(), paymentRequestDTO.getBillTo().getAddressFirstName())
                    .responseMap(AuthNetField.X_LAST_NAME.getFieldName(), paymentRequestDTO.getBillTo().getAddressLastName())
                    .responseMap(AuthNetField.X_ADDRESS.getFieldName(), paymentRequestDTO.getBillTo().getAddressLine1())
                    .responseMap(AuthNetField.X_CITY.getFieldName(), paymentRequestDTO.getBillTo().getAddressCityLocality())
                    .responseMap(AuthNetField.X_STATE.getFieldName(), paymentRequestDTO.getBillTo().getAddressStateRegion())
                    .responseMap(AuthNetField.X_ZIP.getFieldName(), paymentRequestDTO.getBillTo().getAddressPostalCode())
                    .responseMap(AuthNetField.X_COUNTRY.getFieldName(), paymentRequestDTO.getBillTo().getAddressCountryCode())
                    .responseMap(AuthNetField.X_EMAIL.getFieldName(), paymentRequestDTO.getBillTo().getAddressEmail() != null ? paymentRequestDTO.getBillTo().getAddressEmail() : paymentRequestDTO.getCustomer().getEmail())
                    .responseMap(AuthNetField.X_PHONE.getFieldName(), paymentRequestDTO.getBillTo().getAddressPhone());
                }
                
                for(String fieldKey : paymentRequestDTO.getAdditionalFields().keySet()) {
                    responseDTO.responseMap(fieldKey, (String)paymentRequestDTO.getAdditionalFields().get(fieldKey));
                }
                
                responseDTO.successful(response.getMessages().getResultCode() == MessageTypeEnum.OK);
                if (!responseDTO.isSuccessful()) {
                    responseDTO.valid(false);
                    responseDTO.completeCheckoutOnCallback(false);
                }
                
                
                
            } else {            
                Transaction transaction = merchant.createAIMTransaction(transactionType, new BigDecimal(paymentRequestDTO.getTransactionTotal()));
                transaction.getRequestMap().put(AuthNetField.X_TEST_REQUEST.getFieldName(), configuration.getXTestRequest());            transaction.getRequestMap().put(AuthNetField.X_TEST_REQUEST.getFieldName(), configuration.getXTestRequest());
                transaction.setMerchantDefinedField(MessageConstants.BLC_OID, paymentRequestDTO.getOrderId());
                for (Entry<String, Object> field : paymentRequestDTO.getAdditionalFields().entrySet()) {
                    if (field.getValue() != null) {
                        // do not send any fields that are null or the Auth net API flips out
                        transaction.setMerchantDefinedField(field.getKey(), (String) field.getValue());
                    }
                }
                transaction.setTransactionId((String) paymentRequestDTO.getAdditionalFields().get(AuthNetField.X_TRANS_ID.getFieldName()));
    
                if (transactionType.equals(TransactionType.AUTH_CAPTURE) || transactionType.equals(TransactionType.AUTH_ONLY)) {
                    CreditCard creditCard = CreditCard.createCreditCard();
                    creditCard.setCreditCardNumber(paymentRequestDTO.getCreditCard().getCreditCardNum());
                    creditCard.setExpirationMonth(paymentRequestDTO.getCreditCard().getCreditCardExpMonth());
                    creditCard.setExpirationYear(paymentRequestDTO.getCreditCard().getCreditCardExpYear());
                    transaction.setCreditCard(creditCard);
                }
                if (transactionType.equals(TransactionType.CREDIT)) {
                    String cardNumOrLastFour = null;
                    if (paymentRequestDTO.creditCardPopulated()) {
                        cardNumOrLastFour = paymentRequestDTO.getCreditCard().getCreditCardLastFour();
                    }
                    if (cardNumOrLastFour == null && ((String) paymentRequestDTO.getAdditionalFields().get(AuthNetField.X_CARD_NUM.getFieldName())).length() == 4) {
                        cardNumOrLastFour = (String) paymentRequestDTO.getAdditionalFields().get(AuthNetField.X_CARD_NUM.getFieldName());
                    }
                    if (cardNumOrLastFour == null && paymentRequestDTO.creditCardPopulated()) {
                        cardNumOrLastFour = paymentRequestDTO.getCreditCard().getCreditCardNum();
                    }
    
                    CreditCard creditCard = CreditCard.createCreditCard();
                    creditCard.setCreditCardNumber(cardNumOrLastFour);
                    transaction.setCreditCard(creditCard);
                }
    
                Result<Transaction> result = (Result<Transaction>) merchant.postTransaction(transaction);
    
                responseDTO.paymentTransactionType(paymentTransactionType);
                responseDTO.rawResponse(result.getTarget().toNVPString());
                if (result.getTarget().getResponseField(ResponseField.AMOUNT) != null) {
                    responseDTO.amount(new Money(result.getTarget().getResponseField(ResponseField.AMOUNT)));
                }
                responseDTO.orderId(result.getTarget().getMerchantDefinedField(MessageConstants.BLC_OID));
                responseDTO.responseMap(MessageConstants.TRANSACTION_TIME, SystemTime.asDate().toString());
                responseDTO.responseMap(ResponseField.RESPONSE_CODE.getFieldName(), "" + result.getResponseCode().getCode());
                responseDTO.responseMap(ResponseField.RESPONSE_REASON_CODE.getFieldName(), "" + result.getReasonResponseCode().getResponseReasonCode());
                responseDTO.responseMap(ResponseField.RESPONSE_REASON_TEXT.getFieldName(), result.getResponseText());
                responseDTO.responseMap(ResponseField.TRANSACTION_TYPE.getFieldName(), result.getTarget().getTransactionType().getValue());
                responseDTO.responseMap(ResponseField.AMOUNT.getFieldName(), result.getTarget().getResponseField(ResponseField.AMOUNT));
                responseDTO.responseMap(ResponseField.AUTHORIZATION_CODE.getFieldName(), result.getTarget().getAuthorizationCode());
    
                responseDTO.successful(result.isApproved());
                if (result.isError()) {
                    responseDTO.valid(false);
                    responseDTO.completeCheckoutOnCallback(false);
                }
    
                for (String fieldKey : result.getTarget().getMerchantDefinedMap().keySet()) {
                    responseDTO.responseMap(fieldKey, result.getTarget().getMerchantDefinedField(fieldKey));
                }
            }
        }

        return responseDTO;

    }

    /**
     * Takes a "TOKEN" field from the given <b>paymentRequestDTO</b> and parses that into distinct parts of
     * {@link MessageConstants#CUSTOMER_PROFILE_ID} and {@link MessageConstants#PAYMENT_PROFILE_ID} and puts each of those
     * into the given {@link PaymentRequestDTO#getAdditionalFields()}
     * 
     * @param paymentRequestDTO
     * 
     */
    protected void parseOutConsolidatedTokenField(PaymentRequestDTO paymentRequestDTO) {
        // NOTE: in Broadleaf 4.0.5+ the "TOKEN" field is an enum in PaymentAdditionalFieldType.TOKEN. This string is hardcoded
        // manually to keep backwards compatibility
        String consolidatedToken = (String) paymentRequestDTO.getAdditionalFields().get("TOKEN");
        if (consolidatedToken != null) {
            String[] profileIdPaymentId = util.parseConsolidatedPaymentToken(consolidatedToken);
            paymentRequestDTO.getAdditionalFields().put(MessageConstants.CUSTOMER_PROFILE_ID, profileIdPaymentId[0]);
            paymentRequestDTO.getAdditionalFields().put(MessageConstants.PAYMENT_PROFILE_ID, profileIdPaymentId[1]);
        }
    }

}

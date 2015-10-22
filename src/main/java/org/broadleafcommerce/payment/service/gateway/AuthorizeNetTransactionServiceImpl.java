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
            Transaction transaction = merchant.createAIMTransaction(transactionType, new BigDecimal(paymentRequestDTO.getTransactionTotal()));
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

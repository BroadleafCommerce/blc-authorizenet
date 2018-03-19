/*
 * #%L
 * BroadleafCommerce Authorize.net
 * %%
 * Copyright (C) 2009 - 2014 Broadleaf Commerce
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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadleafcommerce.common.money.Money;
import org.broadleafcommerce.common.payment.PaymentTransactionType;
import org.broadleafcommerce.common.payment.PaymentType;
import org.broadleafcommerce.common.payment.dto.AddressDTO;
import org.broadleafcommerce.common.payment.dto.PaymentRequestDTO;
import org.broadleafcommerce.common.payment.dto.PaymentResponseDTO;
import org.broadleafcommerce.common.payment.service.AbstractPaymentGatewayTransactionService;
import org.broadleafcommerce.common.payment.service.PaymentGatewayTransactionService;
import org.broadleafcommerce.common.time.SystemTime;
import org.broadleafcommerce.common.vendor.service.exception.PaymentException;
import org.broadleafcommerce.vendor.authorizenet.service.payment.AuthorizeNetGatewayType;
import org.broadleafcommerce.vendor.authorizenet.service.payment.type.MessageConstants;
import org.broadleafcommerce.vendor.authorizenet.util.AuthorizeNetUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Resource;
import javax.xml.bind.JAXBException;

import net.authorize.AuthNetField;
import net.authorize.Environment;
import net.authorize.Merchant;
import net.authorize.ResponseCode;
import net.authorize.ResponseField;
import net.authorize.TransactionType;
import net.authorize.aim.Result;
import net.authorize.aim.Transaction;
import net.authorize.api.contract.v1.CreateTransactionRequest;
import net.authorize.api.contract.v1.CreateTransactionResponse;
import net.authorize.api.contract.v1.CustomerAddressType;
import net.authorize.api.contract.v1.CustomerDataType;
import net.authorize.api.contract.v1.CustomerTypeEnum;
import net.authorize.api.contract.v1.MerchantAuthenticationType;
import net.authorize.api.contract.v1.MessageTypeEnum;
import net.authorize.api.contract.v1.OpaqueDataType;
import net.authorize.api.contract.v1.OrderType;
import net.authorize.api.contract.v1.TransactionRequestType;
import net.authorize.api.contract.v1.TransactionTypeEnum;
import net.authorize.api.controller.CreateTransactionController;
import net.authorize.api.controller.base.ApiOperationBase;
import net.authorize.data.Order;
import net.authorize.data.ShippingCharges;
import net.authorize.data.cim.PaymentTransaction;
import net.authorize.data.creditcard.CreditCard;
import net.authorize.util.XmlUtility;

@Service("blAuthorizeNetTransactionService")
public class AuthorizeNetTransactionServiceImpl extends AbstractPaymentGatewayTransactionService implements PaymentGatewayTransactionService {

    private static final Log LOG = LogFactory.getLog(AuthorizeNetTransactionServiceImpl.class);

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
        return common(paymentRequestDTO, TransactionType.VOID, PaymentTransactionType.VOID);
    }

    protected PaymentResponseDTO common(PaymentRequestDTO paymentRequestDTO, TransactionType transactionType, PaymentTransactionType paymentTransactionType) {
        Merchant merchant = getAuthorizenetMerchant(paymentRequestDTO);

        PaymentResponseDTO responseDTO = new PaymentResponseDTO(PaymentType.CREDIT_CARD, AuthorizeNetGatewayType.AUTHORIZENET);

        parseOutConsolidatedTokenField(paymentRequestDTO);

        // Use the CIM API to send this transaction using the saved information
        if (paymentRequestDTO.getAdditionalFields().containsKey(MessageConstants.CUSTOMER_PROFILE_ID)
                && paymentRequestDTO.getAdditionalFields().containsKey(MessageConstants.PAYMENT_PROFILE_ID)) {

            if (transactionType.equals(TransactionType.VOID)) {
                TransactionRequestType transactionVoid = new TransactionRequestType();
                if (configuration.isSandbox()) {
                    ApiOperationBase.setEnvironment(Environment.SANDBOX);
                } else {
                    ApiOperationBase.setEnvironment(Environment.PRODUCTION);
                }
                MerchantAuthenticationType merchantAuthenticationType  = new MerchantAuthenticationType() ;
                merchantAuthenticationType.setName(configuration.getLoginId());
                merchantAuthenticationType.setTransactionKey(configuration.getTransactionKey());
                ApiOperationBase.setMerchantAuthentication(merchantAuthenticationType);

                transactionVoid.setTransactionType(TransactionTypeEnum.VOID_TRANSACTION.value());
                transactionVoid.setRefTransId((String) paymentRequestDTO.getAdditionalFields().get(AuthNetField.X_TRANS_ID.getFieldName()));
                transactionVoid.setCustomer(null);
                transactionVoid.setPayment(null);
                transactionVoid.setAmount(null);

                CreateTransactionRequest apiRequest = new CreateTransactionRequest();
                apiRequest.setTransactionRequest(transactionVoid);

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
                responseDTO.responseMap(ResponseField.ACCOUNT_NUMBER.getFieldName(), response.getTransactionResponse().getAccountNumber());
                responseDTO.responseMap(AuthNetField.X_INVOICE_NUM.getFieldName(), (String)paymentRequestDTO.getAdditionalFields().get(AuthNetField.X_INVOICE_NUM.getFieldName()));
                responseDTO.responseMap(AuthNetField.X_LOGIN.getFieldName(), configuration.getLoginId());
                responseDTO.responseMap(AuthNetField.X_VERSION_FIELD.getFieldName(), configuration.getTransactionVersion());
                responseDTO.responseMap(AuthNetField.X_METHOD.getFieldName(), "CC");
                responseDTO.responseMap(AuthNetField.X_TYPE.getFieldName(), transactionType.getValue());
                responseDTO.responseMap(AuthNetField.X_AMOUNT.getFieldName(), paymentRequestDTO.getTransactionTotal());
                responseDTO.responseMap(AuthNetField.X_TEST_REQUEST.getFieldName(), configuration.getXTestRequest());
                responseDTO.responseMap(AuthNetField.X_CUST_ID.getFieldName(), paymentRequestDTO.getCustomer().getCustomerId());
                responseDTO.responseMap(AuthNetField.X_TRANS_ID.getFieldName(), response.getTransactionResponse().getTransId());
                responseDTO.responseMap(MessageConstants.BLC_CID, paymentRequestDTO.getCustomer().getCustomerId());
                responseDTO.responseMap(MessageConstants.BLC_OID, paymentRequestDTO.getOrderId());
                responseDTO.responseMap(MessageConstants.AUTHORIZENET_SERVER_URL, configuration.getServerUrl());

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

                responseDTO.successful(response.getMessages().getResultCode().equals(MessageTypeEnum.OK) &&
                    Integer.toString(ResponseCode.APPROVED.getCode()).equals(response.getTransactionResponse().getResponseCode()));
                if (!responseDTO.isSuccessful()) {
                    responseDTO.valid(false);
                    responseDTO.completeCheckoutOnCallback(false);
                }

                return responseDTO;
            }

            net.authorize.cim.Transaction transaction = merchant.createCIMTransaction(net.authorize.cim.TransactionType.CREATE_CUSTOMER_PROFILE_TRANSACTION);
            transaction.setCustomerProfileId((String) paymentRequestDTO.getAdditionalFields().get(MessageConstants.CUSTOMER_PROFILE_ID));

            PaymentTransaction paymentTransaction = PaymentTransaction.createPaymentTransaction();
            transaction.setPaymentTransaction(paymentTransaction);
            paymentTransaction.setTransactionType(transactionType);
            paymentTransaction.setCustomerPaymentProfileId((String) paymentRequestDTO.getAdditionalFields().get(MessageConstants.PAYMENT_PROFILE_ID));

            Order order = Order.createOrder();
            paymentTransaction.setOrder(order);
            order.setTotalAmount(new BigDecimal(paymentRequestDTO.getTransactionTotal()));
            order.setInvoiceNumber(paymentRequestDTO.getOrderId());
            order.setDescription(paymentRequestDTO.getOrderDescription());

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
            responseDTO.responseMap(AuthNetField.X_TRANS_ID.getFieldName(), responseMap.get(ResponseField.TRANSACTION_ID));
            for (String fieldKey : paymentRequestDTO.getAdditionalFields().keySet()) {
                responseDTO.responseMap(fieldKey, (String) paymentRequestDTO.getAdditionalFields().get(fieldKey));
            }
            for (ResponseField fieldKey : gatewayResult.getDirectResponseList().get(0).getDirectResponseMap().keySet()) {
                responseDTO.responseMap(fieldKey.getFieldName(), gatewayResult.getDirectResponseList().get(0).getDirectResponseMap().get(fieldKey));
            }

        } else {
            if (paymentRequestDTO.getAdditionalFields().containsKey("X_TRANS_ID")) {
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
            } else if (paymentRequestDTO.getAdditionalFields().containsKey("OPAQUE_DATA_DESCRIPTOR")) {
                OrderType orderType = new OrderType();
                orderType.setInvoiceNumber(paymentRequestDTO.getOrderId());
                orderType.setDescription(paymentRequestDTO.getOrderDescription());

                TransactionRequestType transaction = new TransactionRequestType();
                transaction.setOrder(orderType);

                net.authorize.api.contract.v1.PaymentType paymentType = new net.authorize.api.contract.v1.PaymentType();
                OpaqueDataType data = new OpaqueDataType();
                data.setDataDescriptor((String)paymentRequestDTO.getAdditionalFields().get("OPAQUE_DATA_DESCRIPTOR"));
                data.setDataValue((String)paymentRequestDTO.getAdditionalFields().get("OPAQUE_DATA_VALUE"));
                paymentType.setOpaqueData(data);

                CustomerDataType customer = new CustomerDataType();
                customer.setType(CustomerTypeEnum.INDIVIDUAL);
                customer.setId(paymentRequestDTO.getCustomer().getCustomerId());
                customer.setEmail(paymentRequestDTO.getCustomer().getEmail());

                transaction.setCustomer(customer);

                AddressDTO billing = paymentRequestDTO.getBillTo();

                CustomerAddressType customerAddress = new CustomerAddressType();
                customerAddress.setFirstName(billing.getAddressFirstName());
                customerAddress.setLastName(billing.getAddressLastName());
                customerAddress.setAddress(billing.getAddressLine1());
                customerAddress.setCity(billing.getAddressCityLocality());
                customerAddress.setState(billing.getAddressStateRegion());
                customerAddress.setZip(billing.getAddressPostalCode());
                customerAddress.setCountry(billing.getAddressCountryCode());
                customerAddress.setPhoneNumber(billing.getAddressPhone());
                if (StringUtils.isNotEmpty(billing.getAddressEmail())) {
                    customerAddress.setEmail(billing.getAddressEmail());
                } else {
                    customerAddress.setEmail(paymentRequestDTO.getCustomer().getEmail());
                }

                transaction.setPayment(paymentType);
                transaction.setBillTo(customerAddress);
                transaction.setAmount(new BigDecimal(paymentRequestDTO.getTransactionTotal()));

                if (transactionType.equals(TransactionType.AUTH_CAPTURE)) {
                    transaction.setTransactionType(TransactionTypeEnum.AUTH_CAPTURE_TRANSACTION.value());
                } else if (transactionType.equals(TransactionType.AUTH_ONLY)) {
                    transaction.setTransactionType(TransactionTypeEnum.AUTH_ONLY_TRANSACTION.value());
                } else if (transactionType.equals(TransactionType.VOID)) {
                    transaction.setTransactionType(TransactionTypeEnum.VOID_TRANSACTION.value());
                    transaction.setRefTransId((String) paymentRequestDTO.getAdditionalFields().get(AuthNetField.X_TRANS_ID.getFieldName()));
                    transaction.setCustomer(null);
                    transaction.setPayment(null);
                    transaction.setAmount(null);
                } else if (transactionType.equals(TransactionType.PRIOR_AUTH_CAPTURE)) {
                    transaction.setTransactionType(TransactionTypeEnum.PRIOR_AUTH_CAPTURE_TRANSACTION.value());
                }

                CreateTransactionRequest apiRequest = new CreateTransactionRequest();
                apiRequest.setTransactionRequest(transaction);
                apiRequest.setMerchantAuthentication(getMerchantAuthentication(paymentRequestDTO));

                CreateTransactionController controller = new CreateTransactionController(apiRequest);

                net.authorize.Environment authnetEnv = configuration.isSandbox() ? net.authorize.Environment.SANDBOX : net.authorize.Environment.PRODUCTION;
                controller.execute(authnetEnv);

                CreateTransactionResponse response = controller.getApiResponse();

                try {
                    responseDTO.rawResponse(XmlUtility.getXml(response));
                } catch (IOException | JAXBException e) {
                    LOG.error("Could not serialize raw response", e);
                }
                responseDTO.paymentTransactionType(paymentTransactionType);
                responseDTO.amount(new Money(paymentRequestDTO.getTransactionTotal().toString()));
                responseDTO.orderId(paymentRequestDTO.getOrderId());
                responseDTO.responseMap(MessageConstants.TRANSACTION_TIME, SystemTime.asDate().toString());
                responseDTO.responseMap(ResponseField.RESPONSE_CODE.getFieldName(), "" + response.getTransactionResponse().getResponseCode());
                responseDTO.responseMap(ResponseField.RESPONSE_REASON_CODE.getFieldName(), "" + response.getTransactionResponse().getRawResponseCode());
                responseDTO.responseMap(ResponseField.AMOUNT.getFieldName(), paymentRequestDTO.getTransactionTotal().toString());
                responseDTO.responseMap(ResponseField.AUTHORIZATION_CODE.getFieldName(), response.getTransactionResponse().getAuthCode());
                responseDTO.responseMap(ResponseField.ACCOUNT_NUMBER.getFieldName(), response.getTransactionResponse().getAccountNumber())
                           .responseMap(AuthNetField.X_INVOICE_NUM.getFieldName(), transaction.getOrder().getInvoiceNumber())
                           .responseMap(AuthNetField.X_LOGIN.getFieldName(), configuration.getLoginId())
                           .responseMap(AuthNetField.X_VERSION_FIELD.getFieldName(), configuration.getTransactionVersion())
                           .responseMap(AuthNetField.X_METHOD.getFieldName(), "CC")
                           .responseMap(AuthNetField.X_TYPE.getFieldName(), transactionType.getValue())
                           .responseMap(AuthNetField.X_AMOUNT.getFieldName(), paymentRequestDTO.getTransactionTotal())
                           .responseMap(AuthNetField.X_TEST_REQUEST.getFieldName(), configuration.getXTestRequest())
                           .responseMap(AuthNetField.X_CUST_ID.getFieldName(), paymentRequestDTO.getCustomer().getCustomerId())
                           .responseMap(AuthNetField.X_TRANS_ID.getFieldName(), response.getTransactionResponse().getTransId())
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

                responseDTO.successful(response.getMessages().getResultCode().equals(MessageTypeEnum.OK) &&
                    Integer.toString(ResponseCode.APPROVED.getCode()).equals(response.getTransactionResponse().getResponseCode()));
                if (!responseDTO.isSuccessful()) {
                    responseDTO.valid(false);
                    responseDTO.completeCheckoutOnCallback(false);
                }

            } else {

                throw new IllegalStateException( "Cannot determine action from additionalFields: \n" + paymentRequestDTO.getAdditionalFields());

            }
        }

        return responseDTO;

    }

    /**
     * Used for creating transactions from the CIM API as well as from the now-deprecated relay-response URL.
     * In later versions of this module this will be replaced with {@link #getMerchantAuthentication(PaymentRequestDTO)}
     * when support for the transparent redirect is removed.
     */
    protected Merchant getAuthorizenetMerchant(PaymentRequestDTO paymentRequestDTO) {
        Environment env = Environment.createEnvironment(configuration.getServerUrl(), configuration.getXMLBaseUrl());
        Merchant merchant = Merchant.createMerchant(env, configuration.getLoginId(), configuration.getTransactionKey());
        return merchant;
    }

    protected MerchantAuthenticationType getMerchantAuthentication(PaymentRequestDTO paymentRequestDTO) {
        MerchantAuthenticationType merchantAuthenticationType  = new MerchantAuthenticationType() ;
        merchantAuthenticationType.setName(configuration.getLoginId());
        merchantAuthenticationType.setTransactionKey(configuration.getTransactionKey());
        return merchantAuthenticationType;
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

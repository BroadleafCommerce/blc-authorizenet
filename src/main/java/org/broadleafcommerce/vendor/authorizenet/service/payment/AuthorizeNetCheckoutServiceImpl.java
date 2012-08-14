/*
 * Copyright 2008-2009 the original author or authors.
 *
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
 */

package org.broadleafcommerce.vendor.authorizenet.service.payment;

import net.authorize.ResponseField;
import net.authorize.sim.Fingerprint;
import net.authorize.sim.Result;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadleafcommerce.common.money.Money;
import org.broadleafcommerce.core.checkout.service.CheckoutService;
import org.broadleafcommerce.core.checkout.service.exception.CheckoutException;
import org.broadleafcommerce.core.checkout.service.workflow.CheckoutResponse;
import org.broadleafcommerce.core.checkout.service.workflow.CheckoutSeed;
import org.broadleafcommerce.core.order.domain.FulfillmentGroup;
import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.order.service.OrderService;
import org.broadleafcommerce.core.payment.domain.CreditCardPaymentInfo;
import org.broadleafcommerce.core.payment.domain.PaymentInfo;
import org.broadleafcommerce.core.payment.domain.PaymentResponseItem;
import org.broadleafcommerce.core.payment.domain.Referenced;
import org.broadleafcommerce.core.payment.service.PaymentInfoService;
import org.broadleafcommerce.core.payment.service.SecurePaymentInfoService;
import org.broadleafcommerce.core.payment.service.type.PaymentInfoType;
import org.broadleafcommerce.profile.core.domain.*;
import org.broadleafcommerce.profile.core.service.CountryService;
import org.broadleafcommerce.profile.core.service.CustomerService;
import org.broadleafcommerce.profile.core.service.StateService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: elbertbautista
 * Date: 6/27/12
 * Time: 11:42 AM
 */
@Service("blAuthorizeNetCheckoutService")
public class AuthorizeNetCheckoutServiceImpl implements AuthorizeNetCheckoutService {

    private static final Log LOG = LogFactory.getLog(AuthorizeNetCheckoutServiceImpl.class);
    public static final String BLC_CID = "blc_cid";
    public static final String BLC_OID = "blc_oid";
    public static final String BLC_TPS = "blc_tps";

    @Resource(name="blSecurePaymentInfoService")
    protected SecurePaymentInfoService securePaymentInfoService;

    @Resource(name="blPaymentInfoService")
    protected PaymentInfoService paymentInfoService;

    @Resource(name = "blAuthorizeNetVendorOrientedPaymentService")
    protected AuthorizeNetPaymentService authorizeNetPaymentService;

    @Resource(name="blCheckoutService")
    protected CheckoutService checkoutService;

    @Resource(name="blOrderService")
    protected OrderService orderService;

    @Value("${authorizenet.api.login.id}")
    protected String apiLoginId;

    @Value("${authorizenet.transaction.key}")
    protected String transactionKey;

    @Value("${authorizenet.relay.response.url}")
    protected String relayResponseURL;

    @Value("${authorizenet.merchant.transaction.version}")
    protected String merchantTransactionVersion;

    @Value("${authorizenet.x_test_request}")
    protected String xTestRequest;

    @Value("${authorizenet.server.url}")
    protected String serverUrl;

    @Override
    public Order findCartForCustomer(Map<String, String[]> responseMap) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        Result result = authorizeNetPaymentService.createResult(responseMap);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Result Reason Text - " + result.getResponseMap().get(ResponseField.RESPONSE_REASON_TEXT.getFieldName()));
        }

        if (result.isAuthorizeNet()){
            Long customerId = Long.parseLong(result.getResponseMap().get(BLC_CID));
            Long orderId = Long.parseLong(result.getResponseMap().get(BLC_OID));
            String formTps = result.getResponseMap().get(BLC_TPS);
            String tps = createTamperProofSeal(customerId, orderId);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Customer ID - " + customerId);
                LOG.debug("Order ID - " + orderId);
                LOG.debug("Form tps - " + formTps);
                LOG.debug("tps - " + tps);
            }

            if (tps.equalsIgnoreCase(formTps)) {
                Order order = orderService.findOrderById(orderId);
                if (order != null && order.getCustomer().getId().equals(customerId)){
                    return order;
                }
            }
        }

        return null;
    }

    @Override
    public CheckoutResponse completeAuthorizeAndDebitCheckout(Order order, Map<String, String[]> responseMap) throws CheckoutException {
        //Result result = Result.createResult(apiLoginId, merchantMD5Key, responseMap);
        //if (order != null) {
            Map<PaymentInfo, Referenced> payments = new HashMap<PaymentInfo, Referenced>();

            //NOTE: assumes only one payment info of type credit card on the order.
            //Start by removing any payment info of type credit card already on the order.
            orderService.removePaymentsFromOrder(order, PaymentInfoType.CREDIT_CARD);

            PaymentInfo authorizeNetPaymentInfo = paymentInfoService.create();
            authorizeNetPaymentInfo.setOrder(order);
            authorizeNetPaymentInfo.setType(PaymentInfoType.CREDIT_CARD);
            //authorizeNetPaymentInfo.setAmount(new Money(result.getResponseMap().get(ResponseField.AMOUNT.getFieldName())));
            authorizeNetPaymentInfo.setReferenceNumber(order.getOrderNumber());
            authorizeNetPaymentInfo.setRequestParameterMap(responseMap);

            //finally add the authorizenet payment info to the order
            order.getPaymentInfos().add(authorizeNetPaymentInfo);

            CreditCardPaymentInfo creditCardPaymentInfo = ((CreditCardPaymentInfo) securePaymentInfoService.create(PaymentInfoType.CREDIT_CARD));
            creditCardPaymentInfo.setReferenceNumber(authorizeNetPaymentInfo.getReferenceNumber());
            payments.put(authorizeNetPaymentInfo, creditCardPaymentInfo);

            return checkoutService.performCheckout(order, payments);
//            CheckoutResponse checkoutResponse = checkoutService.performCheckout(order, payments);
//
//            PaymentResponseItem responseItem = checkoutResponse.getPaymentResponse().getResponseItems().get(authorizeNetPaymentInfo);
//
//
//            return checkoutResponse;
        //}

        //throw new CheckoutException("Authorize.net DPM Relay Response is Invalid. Check your application keys and hash.", new CheckoutSeed(order, null, null));
    }

    @Override
    public Map<String, String> constructAuthorizeAndDebitFields(Order order) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        if (order != null) {
            Fingerprint fingerprint = Fingerprint.createFingerprint(apiLoginId, transactionKey, System.currentTimeMillis(), order.getTotal().toString());
            Map<String, String> formFields = new HashMap<String, String>();
            formFields.put("x_invoice_num", System.currentTimeMillis()+"");
            formFields.put("x_relay_url", relayResponseURL);
            formFields.put("x_login", apiLoginId);
            formFields.put("x_fp_sequence", fingerprint.getSequence()+"");
            formFields.put("x_fp_timestamp", fingerprint.getTimeStamp()+"");
            formFields.put("x_fp_hash", fingerprint.getFingerprintHash());
            formFields.put("x_version", merchantTransactionVersion);
            formFields.put("x_method", "CC");
            formFields.put("x_type", "AUTH_CAPTURE");
            formFields.put("x_amount", order.getTotal().toString());
            formFields.put("x_test_request", xTestRequest);

            formFields.put(BLC_CID, order.getCustomer().getId().toString());
            formFields.put(BLC_OID, order.getId().toString());
            formFields.put(BLC_TPS, createTamperProofSeal(order.getCustomer().getId(), order.getId()));

            formFields.put("authorizenet_server_url", serverUrl);

            return formFields;
        }

        return null;
    }

    protected String createTamperProofSeal(Long customerId, Long orderId) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        String tamperProofSeal = customerId.toString() + orderId.toString() + apiLoginId + transactionKey;
        md5.digest(tamperProofSeal.getBytes("UTF-8"));
        tamperProofSeal = new BigInteger(1, md5.digest()).toString(16).toUpperCase();
        while(tamperProofSeal.length() < 32) {
            tamperProofSeal = "0" + tamperProofSeal;
        }
        return tamperProofSeal;
    }

}

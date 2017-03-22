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

import org.apache.commons.collections.CollectionUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.broadleafcommerce.common.payment.PaymentType;
import org.broadleafcommerce.common.payment.dto.PaymentRequestDTO;
import org.broadleafcommerce.common.payment.dto.PaymentResponseDTO;
import org.broadleafcommerce.common.payment.service.AbstractPaymentGatewayCustomerService;
import org.broadleafcommerce.common.payment.service.PaymentGatewayCustomerService;
import org.broadleafcommerce.common.vendor.service.exception.PaymentException;
import org.broadleafcommerce.vendor.authorizenet.service.payment.AuthorizeNetGatewayType;
import org.broadleafcommerce.vendor.authorizenet.service.payment.type.MessageConstants;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.w3c.dom.Element;

import java.io.InputStream;
import java.net.URI;
import java.util.List;

import javax.annotation.Resource;

import net.authorize.AuthNetField;
import net.authorize.Environment;
import net.authorize.Merchant;
import net.authorize.cim.Result;
import net.authorize.cim.Transaction;
import net.authorize.cim.TransactionType;
import net.authorize.util.BasicXmlDocument;
import net.authorize.util.HttpClient;
import net.authorize.xml.Message;

/**
 * CIM Guide, as of this writing, is located at http://www.authorize.net/support/CIM_XML_guide.pdf
 * 
 * @author Phillip Verheyden (phillipuniverse)
 */
@Service("blAuthorizeNetCustomerService")
public class AuthorizeNetCustomerService extends AbstractPaymentGatewayCustomerService implements PaymentGatewayCustomerService {

    @Resource(name = "blAuthorizeNetConfiguration")
    protected AuthorizeNetConfiguration configuration;

    @Override
    public PaymentResponseDTO createGatewayCustomer(PaymentRequestDTO requestDTO) throws PaymentException {
        Environment e = Environment.createEnvironment(configuration.getServerUrl(), configuration.getXMLBaseUrl());
        Merchant merchant = Merchant.createMerchant(e, configuration.getLoginId(), configuration.getTransactionKey());

        Assert.isTrue(requestDTO.getAdditionalFields().containsKey(AuthNetField.X_TRANS_ID.getFieldName()),
                "Must pass 'x_trans_id' value on the additionalFields of the Payment Request DTO");
        
        String previousTransId = (String) requestDTO.getAdditionalFields().get(AuthNetField.X_TRANS_ID.getFieldName());
        BasicXmlDocument gatewayResponse = createCustomerFromTransaction(previousTransId, merchant, e);
        
        // In order to get Authorize.net to parse the response, I have to create a 'fake' auth.net.cim.Transaction object
        // to simulate like I actually made the request through the API. Even though I didn't, I can fake out the response
        // by using the dummy transaction object and setting the 'currentResponse' property on it
        // The TransactionType passed in here is COMPLETELY inconsequential. If they ever update their 
        Transaction fakeTransaction = Transaction.createTransaction(merchant, TransactionType.CREATE_CUSTOMER_PAYMENT_PROFILE);
        Transaction responseTransaction = Transaction.createTransaction(fakeTransaction, gatewayResponse);
        Result<Transaction> result = Result.createResult(responseTransaction, gatewayResponse);

        PaymentResponseDTO paymentResponse = new PaymentResponseDTO(PaymentType.CREDIT_CARD, AuthorizeNetGatewayType.AUTHORIZENET);
        paymentResponse.rawResponse(result.getTarget().getCurrentResponse().dump());
        paymentResponse.successful(result.isOk());
        if (result.isOk()) {
            paymentResponse.responseMap(MessageConstants.CUSTOMER_PROFILE_ID, result.getCustomerProfileId());
            paymentResponse.responseMap(MessageConstants.PAYMENT_PROFILE_ID, result.getCustomerPaymentProfileIdList().get(0));
        } else {
            List<Message> messages = result.getMessages();
            if (CollectionUtils.isNotEmpty(messages)) {
                paymentResponse.responseMap(Result.ERROR, messages.get(0).getCode());
            } else {
                paymentResponse.responseMap(Result.ERROR, result.getResultCode());
            }
        }
        
        return paymentResponse;
    }

    @Override
    public PaymentResponseDTO updateGatewayCustomer(PaymentRequestDTO requestDTO) throws PaymentException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PaymentResponseDTO deleteGatewayCustomer(PaymentRequestDTO requestDTO) throws PaymentException {
        // TODO Auto-generated method stub
        return null;
    }

    protected PaymentResponseDTO commonCustomerRequest(PaymentRequestDTO requestDTO) throws PaymentException {

        return null;
    }

    protected BasicXmlDocument createCustomerFromTransaction(String transactionId, Merchant merchant, Environment env) {
        // build the request
        BasicXmlDocument request = new BasicXmlDocument();
        request.parseString("<createCustomerProfileFromTransactionRequest"
                + " xmlns = \"" + Transaction.XML_NAMESPACE + "\" />");
        
        // Copied from net.authorize.cim.Transaction.addAuthentication()
        Element auth_el = request.createElement(AuthNetField.ELEMENT_MERCHANT_AUTHENTICATION.getFieldName());
        Element name_el = request.createElement(AuthNetField.ELEMENT_NAME.getFieldName());
        name_el.appendChild(request.getDocument().createTextNode(merchant.getLogin()));
        Element trans_key = request.createElement(AuthNetField.ELEMENT_TRANSACTION_KEY.getFieldName());
        trans_key.appendChild(request.getDocument().createTextNode(merchant.getTransactionKey()));
        auth_el.appendChild(name_el);
        auth_el.appendChild(trans_key);
        request.getDocumentElement().appendChild(auth_el);
        
        // Taken from the reference guide for creating a customer profile from an existing transaction
        Element transIdElement = request.createElement(AuthNetField.ELEMENT_TRANS_ID.getFieldName());
        transIdElement.appendChild(request.getDocument().createTextNode(transactionId));
        request.getDocumentElement().appendChild(transIdElement);

        // All of this was copied from HttpClient.executeXml(). Some other parts (like the createHttpPost) were pulled
        // out and put inline here as well. This is done because Authnet does not allow for hardly any extension
        BasicXmlDocument response = new BasicXmlDocument();
        try {
            DefaultHttpClient httpClient = new DefaultHttpClient();

            HttpClient.setProxyIfRequested(httpClient);

            // create the HTTP POST object
            URI postUrl = new URI(env.getXmlBaseUrl() + "/xml/v1/request.api");
            HttpPost httpPost = new HttpPost(postUrl);
            httpPost.setHeader("Content-Type", "text/xml; charset=utf-8");
            httpPost.setEntity(new StringEntity(request.dump()));

            // execute the request
            HttpResponse httpResponse = httpClient.execute(httpPost);
            String rawResponseString;
            if (httpResponse != null && httpResponse.getStatusLine().getStatusCode() == 200) {
                HttpEntity entity = httpResponse.getEntity();

                // get the raw data being received
                InputStream instream = entity.getContent();
                rawResponseString = HttpClient.convertStreamToString(instream);
            }
            else {
                StringBuilder responseBuilder = new StringBuilder();
                responseBuilder.append("<?xml version=\"1.0\" ?>");
                responseBuilder.append("<messages><resultCode>Error</resultCode>");
                responseBuilder.append("<message><code>E00001</code>");
                responseBuilder.append("<text>");
                responseBuilder.append(httpResponse != null ? httpResponse.getStatusLine().getReasonPhrase() : "");
                responseBuilder.append("</text></message></messages>");

                rawResponseString = responseBuilder.toString();
            }

            httpClient.getConnectionManager().shutdown();

            if (rawResponseString == null) return null;

            if (Environment.SANDBOX.equals(env)) {

            }

            int mark = rawResponseString.indexOf("<?xml");
            if (mark == -1) {
                return null;
            }

            response.parseString(rawResponseString.substring(mark, rawResponseString.length()));
            if (response.IsAccessible() == false) {
                return null;
            }
        } catch (Exception e) {
            //LogHelper.warn(logger, "Exception getting response: '%s': '%s', '%s'", e.getMessage(), e.getCause(), Arrays.toString(e.getStackTrace()));
        }
        return response;
    }

}

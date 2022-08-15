/*-
 * #%L
 * BroadleafCommerce Authorize.net
 * %%
 * Copyright (C) 2009 - 2022 Broadleaf Commerce
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

package org.broadleafcommerce.vendor.authorizenet.service.payment;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadleafcommerce.payment.service.gateway.AuthorizeNetConfiguration;
import org.springframework.stereotype.Service;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.annotation.Resource;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import net.authorize.ResponseField;
import net.authorize.sim.Result;

/**
 * @author elbertbautista
 * @deprecated - Transparent Redirect is no longer used in favor of Accept.js integration
 */
@Deprecated
@Service("blAuthorizeNetCheckoutService")
public class AuthorizeNetCheckoutServiceImpl implements AuthorizeNetCheckoutService {

    private static final Log LOG = LogFactory.getLog(AuthorizeNetCheckoutServiceImpl.class);
    public static final String BLC_CID = "blc_cid";
    public static final String BLC_OID = "blc_oid";
    public static final String BLC_TPS = "blc_tps";

    @Resource(name = "blAuthorizeNetConfiguration")
    protected AuthorizeNetConfiguration configuration;

    @Override
    public String buildRelayResponse(String receiptUrl, Result result) {
        receiptUrl = addParams(receiptUrl, result);
        StringBuffer response = new StringBuffer();
        response.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"\n \"http://www.w3.org/TR/html4/loose.dtd\">");
        response.append("<html>");
        response.append("<head>");
        response.append("</head>");
        response.append("<body>");
        response.append("<script type=\"text/javascript\">");
        response.append("var referrer = document.referrer;");
        response.append("if (referrer.substr(0,7)==\"http://\") referrer = referrer.substr(7);");
        response.append("if (referrer.substr(0,8)==\"https://\") referrer = referrer.substr(8);");
        response.append("if(referrer && referrer.indexOf(document.location.hostname) != 0) {");
        response.append("document.location = \"" + receiptUrl + "\";");
        response.append("}");
        response.append("</script>");
        response.append("<noscript>");
        response.append("<meta http-equiv=\"refresh\" content=\"0;url=" + receiptUrl + "\">");
        response.append("</noscript>");
        response.append("</body>");
        response.append("</html>");

        return response.toString();
    }

    private String addParams(String receiptUrl, Result result) {
        StringBuffer receiptUrlBuffer = new StringBuffer(receiptUrl);

        try {
            if (result != null) {
                receiptUrlBuffer.append("?");
                receiptUrlBuffer.append(ResponseField.RESPONSE_CODE.getFieldName()).append("=").append(result.getResponseCode().getCode());
                receiptUrlBuffer.append("&");
                receiptUrlBuffer.append(ResponseField.RESPONSE_REASON_CODE.getFieldName()).append("=").append(result.getReasonResponseCode().getResponseReasonCode());

                for (String fieldKey : result.getResponseMap().keySet()) {
                    receiptUrlBuffer.append("&");
                    receiptUrlBuffer.append(fieldKey).append("=");
                    if (fieldKey.equals(ResponseField.RESPONSE_REASON_TEXT.getFieldName())) {
                        String responseText = result.getResponseMap().get(fieldKey);
                        receiptUrlBuffer.append(responseText != null ? URLEncoder.encode(responseText, "UTF-8") : responseText);
                    } else {
                        receiptUrlBuffer.append(result.getResponseMap().get(fieldKey));
                    }
                }

                if (result.isApproved()) {
                    receiptUrlBuffer.append("&").append(ResponseField.TRANSACTION_ID.getFieldName()).append("=").append(result.getResponseMap().get(ResponseField.TRANSACTION_ID.getFieldName()));
                }
            }
        } catch (UnsupportedEncodingException e) {
        }

        return receiptUrlBuffer.toString();
    }

    @Override
    public String createTamperProofSeal(String customerId, String orderId) throws NoSuchAlgorithmException, InvalidKeyException {
        String transactionKey = configuration.getTransactionKey();

        Base64 encoder = new Base64();
        Mac sha1Mac = Mac.getInstance("HmacSHA1");
        SecretKeySpec publicKeySpec = new SecretKeySpec(transactionKey.getBytes(), "HmacSHA1");
        sha1Mac.init(publicKeySpec);
        String customerOrderString = customerId + orderId;
        byte[] publicBytes = sha1Mac.doFinal(customerOrderString.getBytes());
        String publicDigest = encoder.encodeToString(publicBytes);
        return publicDigest.replaceAll("\\r|\\n", "");
    }
    
    @Override
    public boolean verifyTamperProofSeal(String customerId, String orderId, String tps) {
        try {
            return tps != null && tps.equals(createTamperProofSeal(customerId, orderId));
        } catch (NoSuchAlgorithmException e) {
            LOG.error(e);
            return false;
        } catch (InvalidKeyException e) {
            LOG.error(e);
            return false;
        }
    }

}

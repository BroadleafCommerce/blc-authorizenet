/**
 * 
 */
package org.broadleafcommerce.vendor.authorizenet.util;

import org.apache.commons.lang3.StringUtils;
import org.broadleafcommerce.vendor.authorizenet.service.payment.type.MessageConstants;
import org.springframework.stereotype.Component;

/**
 * General utility used in the Authorize.net module
 * 
 * @author Phillip Verheyden (phillipuniverse)
 */
@Component("blAuthorizeNetUtil")
public class AuthorizeNetUtil {

    public static final String SEPARATOR = "|";
    
    /**
     * 
     * @param consolidatedPaymentToken
     * @return a 2-element array with the {@link MessageConstants#CUSTOMER_PROFILE_ID} in index 0 and {@link MessageConstants#PAYMENT_PROFILE_ID}
     * in index 1
     * @see {@link #buildConsolidatedPaymentToken(String, String)}
     */
    public String[] parseConsolidatedPaymentToken(String consolidatedPaymentToken) {
        return StringUtils.split(consolidatedPaymentToken, SEPARATOR);
    }
    
    /**
     * Returns a string that is the combination of the <b>customerProfileId</b> and <b>paymentProfileId</b>
     * @param customerProfileId
     * @param paymentProfileId
     * @see {@link #parseConsolidatedPaymentToken(String)}
     */
    public String buildConsolidatedPaymentToken(String customerProfileId, String paymentProfileId) {
        return StringUtils.join(customerProfileId, SEPARATOR, paymentProfileId);
    }
    
}

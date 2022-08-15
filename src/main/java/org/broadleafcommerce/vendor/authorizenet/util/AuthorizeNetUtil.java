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

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


import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import net.authorize.sim.Result;

/**
 * @author elbertbautista
 * @deprecated - Transparent Redirect is no longer used in favor of Accept.js integration
 */
@Deprecated
public interface AuthorizeNetCheckoutService {

    /**
     * Builds the Javascript snippet necessary 
     * 
     * @param receiptUrl
     * @param result
     * @return
     */
    public String buildRelayResponse(String receiptUrl, Result result);

    /**
     * Creates a seal for the current customer and order combined with the Authorize.net private key to verify that
     * requests and responses actually came from Broadleaf.
     * 
     * @param customerId
     * @param orderId
     * @return
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @see {@link #createTamperProofSeal(String, String)}
     */
    public String createTamperProofSeal(String customerId, String orderId) throws NoSuchAlgorithmException, InvalidKeyException;
    
    /**
     * Verifies that the given tamper proof seal is valid for the given customer and order
     * 
     * @param customerId
     * @param orderId
     * @param tps
     * @return
     * @see {@link #createTamperProofSeal(String, String)}
     */
    public boolean verifyTamperProofSeal(String customerId, String orderId, String tps);
    
}

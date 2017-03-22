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

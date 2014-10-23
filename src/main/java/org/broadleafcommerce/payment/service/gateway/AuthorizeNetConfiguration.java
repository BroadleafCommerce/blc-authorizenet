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

import org.broadleafcommerce.common.payment.service.PaymentGatewayConfiguration;

/**
 * @author Chad Harchar (charchar)
 */
public interface AuthorizeNetConfiguration extends PaymentGatewayConfiguration {
    
    /**
    * This is the merchant API Login ID, provided by the
    * Merchant Interface.
    *
    * @return String
    */
    public String getLoginId();

    /**
    * This is the transaction fingerprint, provided by the
    * Merchant Interface.
    *
    * @return String
    */
    public String getTransactionKey();

    /**
    * This is the MD5 Hash, generated through the
    * Merchant Interface.
    *
    * @return String
    */
    public String getMd5Key();

    /**
     * The merchant’s transaction version.
     * 
     * @return
     */
    public String getTransactionVersion();

    /**
    * This is the URL to which Authorize.net returns.
    * This must be a publicly accessible URL and also
    * must be the same value you entered in your account settings.
    * (e.g. http://mycompany.com/authorizenet/process)
    *
    * @return String
    */
    public String getResponseUrl();

    /**
    * This is the URL to which Authorize.net returns upon a 
    * successful transaction.
    * (e.g. http://mycompany.com/confirmation)
    *
    * @return String
    */
    public String getConfirmUrl();

    /**
    * This is the URL to which Authorize.net returns upon a
    * failed transaction.
    * (e.g. http://mycompany.com/authorizenet/error)
    *
    * @return String
    */
    public String getErrorUrl();

    /**
     * This is the URL to which Authorize.net returns upon a
     * failed transaction.
     * 
     * Developer test environment:
     * https://test.authorize.net/gateway/transact.dll
     * (Make sure xTestRequest is set to false)
     * 
     * Staging:
     * https://secure.authorize.net/gateway/transact.dll
     * (Make sure xTestRequest is set to true)
     * 
     * Production:
     * https://secure.authorize.net/gateway/transact.dll
     * (Make sure xTestRequest is set to false)
     *
     * @return String
     */
    public String getServerUrl();
    
    /**
     * Slightly different than {@link #getServerUrl()} in that this is used to communicate with the XML APIs. This does not
     * have the /gateway/transact.dll on the end of it and serves as a convenience method for whatever is in {@link #getServerUrl()}
     * 
     * @return
     */
    public String getXMLBaseUrl();

    /**
    * This value should only be true when testing in a live environment, e.g. staging.
    *
    * @return String
    */
    public String getXTestRequest();

}

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
     * This is the generated client key, provided by the
     * Merchant Interface that is used in an Accept.js implementation
     * @return
     */
    public String getClientKey();

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
    @Deprecated
    public String getResponseUrl();

    /**
    * This is the URL to which Authorize.net returns upon a 
    * successful transaction.
    * (e.g. http://mycompany.com/confirmation)
    *
    * @return String
    */
    @Deprecated
    public String getConfirmUrl();

    /**
    * This is the URL to which Authorize.net returns upon a
    * failed transaction.
    * (e.g. http://mycompany.com/authorizenet/error)
    *
    * @return String
    */
    @Deprecated
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
    
    public Boolean isSandbox();

    public String getAcceptJsUrl();

    public String getGatewayMerchantId();

    public String getAppleMerchantId();

    public String getAppleKeyStoreFilePath();

    public String getAppleKeyStorePassword();

    public String getVerifiedDomainName();

    public String getVerifiedDomainDisplayName();
}

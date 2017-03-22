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

import org.broadleafcommerce.common.payment.PaymentGatewayType;
import org.broadleafcommerce.common.util.BLCSystemProperty;
import org.broadleafcommerce.vendor.authorizenet.service.payment.AuthorizeNetGatewayType;
import org.springframework.stereotype.Service;

/**
 * @author Chad Harchar (charchar)
 */
@Service("blAuthorizeNetConfiguration")
public class AuthorizeNetConfigurationImpl implements AuthorizeNetConfiguration {

    protected int failureReportingThreshold = 1;

    protected boolean performAuthorizeAndCapture = true;

    @Override
    public String getLoginId() {
        return BLCSystemProperty.resolveSystemProperty("gateway.authorizenet.loginId");
    }

    @Override
    public String getClientKey() {
        return BLCSystemProperty.resolveSystemProperty("gateway.authorizenet.clientKey");
    }

    @Override
    public String getTransactionKey() {
        return BLCSystemProperty.resolveSystemProperty("gateway.authorizenet.transactionKey");
    }

    @Override
    public String getMd5Key() {
        return BLCSystemProperty.resolveSystemProperty("gateway.authorizenet.merchantMd5Key");
    }

    @Override
    public String getTransactionVersion() {
        return BLCSystemProperty.resolveSystemProperty("gateway.authorizenet.transactionVersion");
    }

    @Override
    public String getResponseUrl() {
        return BLCSystemProperty.resolveSystemProperty("gateway.authorizenet.responseUrl");
    }

    @Override
    public String getConfirmUrl() {
        return BLCSystemProperty.resolveSystemProperty("gateway.authorizenet.confirmUrl");
    }

    @Override
    public String getErrorUrl() {
        return BLCSystemProperty.resolveSystemProperty("gateway.authorizenet.errorUrl");
    }

    @Override
    public String getServerUrl() {
        return BLCSystemProperty.resolveSystemProperty("gateway.authorizenet.serverUrl");
    }
    
    @Override
    public String getXMLBaseUrl() {
        return getServerUrl().replace("/gateway/transact.dll", "")
                .replace("test", "apitest")
                .replace("secure", "api");
    }

    @Override
    public String getXTestRequest() {
        return BLCSystemProperty.resolveSystemProperty("gateway.authorizenet.xTestRequest");
    }

    @Override
    public boolean isPerformAuthorizeAndCapture() {
        return performAuthorizeAndCapture;
    }

    @Override
    public void setPerformAuthorizeAndCapture(boolean performAuthorizeAndCapture) {
        this.performAuthorizeAndCapture = performAuthorizeAndCapture;
    }

    @Override
    public int getFailureReportingThreshold() {
        return failureReportingThreshold;
    }

    @Override
    public void setFailureReportingThreshold(int failureReportingThreshold) {
        this.failureReportingThreshold = failureReportingThreshold;
    }

    @Override
    public boolean handlesAuthorize() {
        return true;
    }

    @Override
    public boolean handlesCapture() {
        return true;
    }

    @Override
    public boolean handlesAuthorizeAndCapture() {
        return true;
    }

    @Override
    public boolean handlesReverseAuthorize() {
        return true;
    }

    @Override
    public boolean handlesVoid() {
        return true;
    }

    @Override
    public boolean handlesRefund() {
        return true;
    }

    @Override
    public boolean handlesPartialCapture() {
        return false;
    }

    @Override
    public boolean handlesMultipleShipment() {
        return false;
    }

    @Override
    public boolean handlesRecurringPayment() {
        return false;
    }

    @Override
    public boolean handlesSavedCustomerPayment() {
        return false;
    }

    @Override
    public boolean handlesMultiplePayments() {
        return false;
    }

    @Override
    public PaymentGatewayType getGatewayType() {
        return AuthorizeNetGatewayType.AUTHORIZENET;
    }

}

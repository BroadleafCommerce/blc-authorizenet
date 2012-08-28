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

import org.springframework.beans.factory.annotation.Value;

/**
 * @author elbertbautista
 */
public class AuthorizeNetGatewayRequestImpl implements AuthorizeNetGatewayRequest {

    protected String apiLoginId;
    protected String transactionKey;
    protected String relayResponseUrl;
    protected String merchantMD5Key;
    protected String merchantTransactionVersion;
    protected String xTestRequest;
    protected String serverUrl;

    @Override
    public String getApiLoginId() {
        return apiLoginId;
    }

    @Override
    public void setApiLoginId(String apiLoginId) {
        this.apiLoginId = apiLoginId;
    }

    @Override
    public String getTransactionKey() {
        return transactionKey;
    }

    @Override
    public void setTransactionKey(String transactionKey) {
        this.transactionKey = transactionKey;
    }

    @Override
    public String getRelayResponseUrl() {
        return relayResponseUrl;
    }

    @Override
    public void setRelayResponseUrl(String relayResponseUrl) {
        this.relayResponseUrl = relayResponseUrl;
    }

    @Override
    public String getMerchantMD5Key() {
        return merchantMD5Key;
    }

    @Override
    public void setMerchantMD5Key(String merchantMD5Key) {
        this.merchantMD5Key = merchantMD5Key;
    }

    @Override
    public String getMerchantTransactionVersion() {
        return merchantTransactionVersion;
    }

    @Override
    public void setMerchantTransactionVersion(String merchantTransactionVersion) {
        this.merchantTransactionVersion = merchantTransactionVersion;
    }

    @Override
    public String getxTestRequest() {
        return xTestRequest;
    }

    @Override
    public void setxTestRequest(String xTestRequest) {
        this.xTestRequest = xTestRequest;
    }

    @Override
    public String getServerUrl() {
        return serverUrl;
    }

    @Override
    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }
}

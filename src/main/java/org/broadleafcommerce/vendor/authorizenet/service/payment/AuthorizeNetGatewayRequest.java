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

import java.io.Serializable;

/**
 * @author elbertbautista
 */
public interface AuthorizeNetGatewayRequest extends Serializable {

    String getApiLoginId();

    void setApiLoginId(String apiLoginId);

    String getTransactionKey();

    void setTransactionKey(String transactionKey);

    String getRelayResponseUrl();

    void setRelayResponseUrl(String relayResponseUrl);

    String getMerchantMD5Key();

    void setMerchantMD5Key(String merchantMD5Key);

    String getMerchantTransactionVersion();

    void setMerchantTransactionVersion(String merchantTransactionVersion);

    String getxTestRequest();

    void setxTestRequest(String xTestRequest);

    String getServerUrl();

    void setServerUrl(String serverUrl);
}

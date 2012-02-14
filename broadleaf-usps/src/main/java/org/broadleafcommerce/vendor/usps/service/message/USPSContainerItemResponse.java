/*
 * Copyright 2008-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.broadleafcommerce.vendor.usps.service.message;

import java.util.Map;

import org.broadleafcommerce.vendor.usps.service.type.USPSServiceResponseType;

public interface USPSContainerItemResponse {

    public String getPackageId();

    public void setPackageId(String packageId);

    public Map<USPSServiceResponseType, USPSPostage> getPostage();

    public void setPostage(Map<USPSServiceResponseType, USPSPostage> postage);

    public String getRestrictions();

    public void setRestrictions(String restrictions);

    public String getErrorCode();

    public void setErrorCode(String errorCode);

    public String getErrorText();

    public void setErrorText(String errorText);

    public boolean isErrorDetected();

    public void setErrorDetected(boolean isErrorDetected);

    public String getZone();

    public void setZone(String zone);

}
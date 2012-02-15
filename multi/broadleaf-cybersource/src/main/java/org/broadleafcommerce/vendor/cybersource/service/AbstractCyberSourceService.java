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

package org.broadleafcommerce.vendor.cybersource.service;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;

import javax.xml.rpc.ServiceException;

import org.apache.axis.AxisFault;
import org.apache.axis.EngineConfiguration;
import org.apache.axis.configuration.FileProvider;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.broadleafcommerce.profile.core.service.IdGenerationService;
import org.broadleafcommerce.common.vendor.service.monitor.ServiceStatusDetectable;
import org.broadleafcommerce.common.vendor.service.type.ServiceStatusType;
import org.broadleafcommerce.vendor.cybersource.service.api.ITransactionProcessorStub;
import org.broadleafcommerce.vendor.cybersource.service.api.ReplyMessage;
import org.broadleafcommerce.vendor.cybersource.service.api.RequestMessage;
import org.broadleafcommerce.vendor.cybersource.service.api.TransactionProcessorLocator;

/**
 * 
 * @author jfischer
 *
 */
public abstract class AbstractCyberSourceService implements ServiceStatusDetectable {

	protected String merchantId;
	protected String serverUrl;
	protected String libVersion;
	protected Integer failureReportingThreshold;
    protected Integer failureCount = 0;
    protected Boolean isUp = true;
    protected IdGenerationService idGenerationService;
    
    protected ReplyMessage sendRequest(RequestMessage request) throws AxisFault, MalformedURLException, RemoteException, ServiceException {
		EngineConfiguration config = new FileProvider("CyberSourceDeploy.wsdd");
		TransactionProcessorLocator service = new TransactionProcessorLocator(config);
        URL endpoint = new URL(getServerUrl());
        ITransactionProcessorStub stub = (ITransactionProcessorStub) service.getportXML(endpoint);
        stub._setProperty(WSHandlerConstants.USER, request.getMerchantID());
        ReplyMessage reply = stub.runTransaction(request);
        
        return reply;
	}
	
	protected void clearStatus() {
        synchronized(failureCount) {
            isUp = true;
            failureCount = 0;
        }
    }

    protected void incrementFailure() {
        synchronized(failureCount) {
            if (failureCount >= failureReportingThreshold) {
                isUp = false;
            } else {
                failureCount++;
            }
        }
    }
	
	public Integer getFailureReportingThreshold() {
		return failureReportingThreshold;
	}

	public String getMerchantId() {
		return merchantId;
	}

	public String getServerUrl() {
		return serverUrl;
	}
	
	public String getLibVersion() {
		return libVersion;
	}

	public String getServiceName() {
		return getClass().getName();
	}

	public ServiceStatusType getServiceStatus() {
		synchronized(failureCount) {
            if (isUp) {
                return ServiceStatusType.UP;
            } else {
                return ServiceStatusType.DOWN;
            }
        }
	}

	public void setFailureReportingThreshold(Integer failureReportingThreshold) {
		this.failureReportingThreshold = failureReportingThreshold;
	}

	public void setMerchantId(String merchantId) {
		this.merchantId = merchantId;
	}

	public void setServerUrl(String serverUrl) {
		this.serverUrl = serverUrl;
	}
	
	public void setLibVersion(String libVersion) {
		this.libVersion = libVersion;
	}
	
	public IdGenerationService getIdGenerationService() {
		return idGenerationService;
	}
	
	public void setIdGenerationService(IdGenerationService idGenerationService) {
		this.idGenerationService = idGenerationService;
	}
}

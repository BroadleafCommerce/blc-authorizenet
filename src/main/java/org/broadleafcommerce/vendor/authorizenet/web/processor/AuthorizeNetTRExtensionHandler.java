/*-
 * #%L
 * BroadleafCommerce Authorize.net
 * %%
 * Copyright (C) 2009 - 2023 Broadleaf Commerce
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
package org.broadleafcommerce.vendor.authorizenet.web.processor;

import org.broadleafcommerce.common.payment.dto.PaymentResponseDTO;
import org.broadleafcommerce.common.payment.service.PaymentGatewayConfiguration;
import org.broadleafcommerce.common.payment.service.PaymentGatewayTransparentRedirectService;
import org.broadleafcommerce.common.web.payment.processor.AbstractTRCreditCardExtensionHandler;
import org.broadleafcommerce.common.web.payment.processor.TRCreditCardExtensionManager;
import org.broadleafcommerce.payment.service.gateway.AuthorizeNetConfiguration;
import org.broadleafcommerce.vendor.authorizenet.service.payment.type.MessageConstants;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;


/**
 * @author Chad Harchar (charchar)
 * @deprecated - Transparent Redirect is no longer used in favor of Accept.js integration
 */
@Deprecated
@Service("blAuthorizeNetTRExtensionHandler")
public class AuthorizeNetTRExtensionHandler extends AbstractTRCreditCardExtensionHandler {

    public static final String FORM_ACTION_URL = MessageConstants.AUTHORIZENET_SERVER_URL;
    public static final String FORM_HIDDEN_PARAMS = "FORM_HIDDEN_PARAMS";
    public static final String FIELD_NAMES = "FIELD_NAMES";

    @Resource(name = "blTRCreditCardExtensionManager")
    protected TRCreditCardExtensionManager extensionManager;

    @Resource(name = "blAuthorizeNetConfiguration")
    protected AuthorizeNetConfiguration configuration;
    
    @Resource(name = "blAuthorizeNetTransparentRedirectService")
    protected PaymentGatewayTransparentRedirectService transparentRedirectService;

    @PostConstruct
    public void init() {
        if (isEnabled()) {
            extensionManager.registerHandler(this);
        }
    }

    @Override
    public String getFormActionURLKey() {
        return FORM_ACTION_URL;
    }

    @Override
    public String getHiddenParamsKey() {
        return FORM_HIDDEN_PARAMS;
    }

    @Override
    public PaymentGatewayConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public PaymentGatewayTransparentRedirectService getTransparentRedirectService() {
        return transparentRedirectService;
    }

    @Override
    public void populateFormParameters(Map<String, Map<String, String>> formParameters, PaymentResponseDTO responseDTO) {
        String actionUrl = (String) responseDTO.getResponseMap().get(MessageConstants.AUTHORIZENET_SERVER_URL);
        responseDTO.getResponseMap().remove(MessageConstants.AUTHORIZENET_SERVER_URL);
        
        Map<String, String> actionValue = new HashMap<String, String>();
        actionValue.put(getFormActionURLKey(), actionUrl);
        formParameters.put(getFormActionURLKey(), actionValue);

        Iterator<?> it = responseDTO.getResponseMap().entrySet().iterator();

        SortedMap<String, String> hiddenFields = new TreeMap<String, String>();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry)it.next();
            hiddenFields.put((String)pairs.getKey(), (String)pairs.getValue());
        }

        formParameters.put(getHiddenParamsKey(), hiddenFields);
    }

}

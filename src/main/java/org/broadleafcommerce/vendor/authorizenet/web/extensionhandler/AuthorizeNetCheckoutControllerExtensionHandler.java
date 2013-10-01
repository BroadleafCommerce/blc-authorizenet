/*
 * Copyright 2008-2012 the original author or authors.
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

package org.broadleafcommerce.vendor.authorizenet.web.extensionhandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadleafcommerce.common.extension.AbstractExtensionHandler;
import org.broadleafcommerce.common.extension.ExtensionResultStatusType;
import org.broadleafcommerce.core.order.domain.NullOrderImpl;
import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.web.controller.checkout.BroadleafCheckoutControllerExtensionHandler;
import org.broadleafcommerce.core.web.controller.checkout.BroadleafCheckoutControllerExtensionManager;
import org.broadleafcommerce.core.web.order.CartState;
import org.broadleafcommerce.vendor.authorizenet.service.payment.AuthorizeNetCheckoutService;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * Authorize.Net module extension handler for the BroadleafCheckoutController
 *
 * @author Joshua Skorton (jskorton)
 */
@Service("blAuthorizeNetCheckoutControllerExtensionHandler")
public class AuthorizeNetCheckoutControllerExtensionHandler extends AbstractExtensionHandler implements BroadleafCheckoutControllerExtensionHandler {

    private static final Log LOG = LogFactory.getLog(AuthorizeNetCheckoutControllerExtensionHandler.class);

    @Resource(name = "blCheckoutControllerExtensionManager")
    protected BroadleafCheckoutControllerExtensionManager extensionManager;

    @Resource(name = "blAuthorizeNetCheckoutService")
    protected AuthorizeNetCheckoutService authorizeNetCheckoutService;

    @PostConstruct
    public void init() {
        if (isEnabled()) {
            extensionManager.registerHandler(this);
        }
    }

    /**
     * Adds Authorize.Net module attributes to the BroadleafCheckoutController model
     * 
     * @param model
     * @return
     */
    @Override
    public ExtensionResultStatusType addAdditionalModelVariables(Model model) {
        Order order = CartState.getCart();
        if (order != null && !(order instanceof NullOrderImpl)) {
            try {
                Map<String, String> formFields = authorizeNetCheckoutService.constructAuthorizeAndDebitFields(order);
                for (String key : formFields.keySet()) {
                    model.addAttribute(key, formFields.get(key));
                    LOG.debug("Checkout Form Field Parameter - " + key + " = " + formFields.get(key));
                }
            } catch (NoSuchAlgorithmException e) {
                LOG.error("Error Creating Authorize.net Checkout Form " + e);
            } catch (InvalidKeyException e) {
                LOG.error("Error Creating Authorize.net Checkout Form " + e);
            } catch (UnsupportedOperationException e) {
                LOG.error("Error Creating Authorize.net Checkout Form " + e);
            }
        }

        return ExtensionResultStatusType.HANDLED_CONTINUE;
    }

    /**
     * Not currently implemented for Authorize.Net
     * 
     * @return
     */
    @Override
    public ExtensionResultStatusType performAdditionalShippingAction() {
        return ExtensionResultStatusType.HANDLED_CONTINUE;
    }

}

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

package org.broadleafcommerce.vendor.authorizenet.web.controller;

import net.authorize.sim.Result;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadleafcommerce.common.payment.dto.PaymentRequestDTO;
import org.broadleafcommerce.common.payment.dto.PaymentResponseDTO;
import org.broadleafcommerce.common.payment.service.PaymentGatewayConfiguration;
import org.broadleafcommerce.common.payment.service.PaymentGatewayTransactionService;
import org.broadleafcommerce.common.payment.service.PaymentGatewayWebResponsePrintService;
import org.broadleafcommerce.common.payment.service.PaymentGatewayWebResponseService;
import org.broadleafcommerce.common.vendor.service.exception.PaymentException;
import org.broadleafcommerce.common.web.payment.controller.PaymentGatewayAbstractController;
import org.broadleafcommerce.payment.service.gateway.AuthorizeNetConfiguration;
import org.broadleafcommerce.vendor.authorizenet.service.payment.AuthorizeNetCheckoutService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Chad Harchar (charchar)
 * @author Elbert Bautista (elbertbautista)
 */
@Controller("blAuthorizeNetController")
@RequestMapping("/" + BroadleafAuthorizeNetController.GATEWAY_CONTEXT_KEY)
public class BroadleafAuthorizeNetController extends PaymentGatewayAbstractController {

    protected static final Log LOG = LogFactory.getLog(BroadleafAuthorizeNetController.class);
    protected static final String GATEWAY_CONTEXT_KEY = "authorizenet";

    @Resource(name = "blAuthorizeNetWebResponseService")
    protected PaymentGatewayWebResponseService paymentGatewayWebResponseService;

    @Resource(name = "blAuthorizeNetCheckoutService")
    protected AuthorizeNetCheckoutService authorizeNetCheckoutService;

    @Resource(name = "blAuthorizeNetConfiguration")
    protected AuthorizeNetConfiguration configuration;

    @Resource(name = "blPaymentGatewayWebResponsePrintService")
    protected PaymentGatewayWebResponsePrintService webResponsePrintService;
    
    @Resource(name = "blAuthorizeNetTransactionService")
    protected PaymentGatewayTransactionService transactionService;

    @Override
    public String getGatewayContextKey() {
        return GATEWAY_CONTEXT_KEY;
    }

    @Override
    public void handleProcessingException(Exception e, RedirectAttributes redirectAttributes) throws PaymentException {
        // Don't do anything here because there isn't enough information in the given parameters to actually
        // void the payment that we need to void the payment
        throw new PaymentException(e);
    }

    @Override
    public void handleUnsuccessfulTransaction(Model model, RedirectAttributes redirectAttributes,
            PaymentResponseDTO responseDTO) throws PaymentException {
        LOG.trace("A Processing Exception Occurred for " + GATEWAY_CONTEXT_KEY + ". Adding Error to Redirect Attributes.");
        redirectAttributes.addAttribute(PAYMENT_PROCESSING_ERROR, getProcessingErrorMessage());
    }

    @Override
    public PaymentGatewayWebResponseService getWebResponseService() {
        return paymentGatewayWebResponseService;
    }

    @Override
    public PaymentGatewayConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    @RequestMapping(value = "/error", method = RequestMethod.GET)
    public String errorEndpoint(Model model, HttpServletRequest request, RedirectAttributes redirectAttributes,
            Map<String, String> pathVars) throws PaymentException {
        redirectAttributes.addAttribute(PAYMENT_PROCESSING_ERROR, request.getParameter(PAYMENT_PROCESSING_ERROR));
        return getOrderReviewRedirect();
    }

    @Override
    @RequestMapping(value = "/return", method = RequestMethod.GET)
    public String returnEndpoint(Model model, HttpServletRequest request,
            final RedirectAttributes redirectAttributes,
            @PathVariable Map<String, String> pathVars) throws PaymentException {
        PaymentResponseDTO responseDTO = getWebResponseService().translateWebResponse(request);
        String orderNumber = lookupOrderNumberFromOrderId(responseDTO);
        return getConfirmationViewRedirect(orderNumber);
    }

    /**
     * <p>
     * Invoked by Authorize.net after taking the user's credit card. This should process the payment and add it to the order
     * as well as check out the order if configured.
     * 
     * <p>
     * At this point in the lifecyle of taking payment, the card has already been charged. If there is a problem checking
     * out the order, we need to communicate that explicitly up to Authorize.net via a VOID transaction. Otherwise, the user
     * will be left with a non-submitted Order but their card will still be charged.
     * 
     * @param request
     * @param response
     * @param model
     * @return
     */
    @RequestMapping(value = "/process", method = RequestMethod.POST, produces = "text/html")
    public @ResponseBody String process(HttpServletRequest request,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes,
            Model model) throws PaymentException {
        LOG.debug("Authorize URL request - " + request.getRequestURL().toString());
        LOG.debug("Authorize Request Parameter Map (params: [" + requestParamToString(request) + "])");
        Result result = Result.createResult(configuration.getLoginId(), configuration.getMd5Key(), request.getParameterMap());
        
        boolean error = false;
        String returnUrl = "";
        try {
            // Process the payment on the Broadleaf order side
            returnUrl = super.process(model, request, redirectAttributes);
        } catch (Exception e) {
            error = true;
        }
        
        // This is a hacky way to do this but has to be done because the handleProcessingException does not give enough
        // information to actually be useful in doing what I need. Ideally this logic would go in handleProcessingException
        if (error || getErrorViewRedirect().equals(returnUrl)) {
            PaymentResponseDTO responseDTO = getWebResponseService().translateWebResponse(request);
            PaymentRequestDTO voidRequest = responseToVoidRequest(responseDTO);
            transactionService.voidPayment(voidRequest);
            
            return authorizeNetCheckoutService.buildRelayResponse(configuration.getErrorUrl(), result);
        }

        return authorizeNetCheckoutService.buildRelayResponse(configuration.getConfirmUrl(), result);
    }
    
    /**
     * Builds a request with enough information to void the given response from the gateway
     * 
     * @param response
     * @return
     */
    protected PaymentRequestDTO responseToVoidRequest(PaymentResponseDTO response) {
        PaymentRequestDTO request = new PaymentRequestDTO()
            .transactionTotal(response.getAmount().toString());
        
        for (Entry<String, String> field : response.getResponseMap().entrySet()) {
            request.additionalField(field.getKey(), field.getValue());
        }
        
        return request;
    }

    protected String requestParamToString(HttpServletRequest request) {
        StringBuffer requestMap = new StringBuffer();
        for (String key : (Set<String>) request.getParameterMap().keySet()) {
            requestMap.append(key + ": " + request.getParameter(key) + ", ");
        }
        return requestMap.toString();
    }

}

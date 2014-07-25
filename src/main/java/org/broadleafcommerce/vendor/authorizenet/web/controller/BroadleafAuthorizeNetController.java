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

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.authorize.AuthNetField;
import net.authorize.sim.Result;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadleafcommerce.common.payment.PaymentType;
import org.broadleafcommerce.common.payment.dto.PaymentResponseDTO;
import org.broadleafcommerce.common.payment.service.PaymentGatewayConfiguration;
import org.broadleafcommerce.common.payment.service.PaymentGatewayWebResponsePrintService;
import org.broadleafcommerce.common.payment.service.PaymentGatewayWebResponseService;
import org.broadleafcommerce.common.vendor.service.exception.PaymentException;
import org.broadleafcommerce.common.web.payment.controller.PaymentGatewayAbstractController;
import org.broadleafcommerce.payment.service.gateway.AuthorizeNetConfiguration;
import org.broadleafcommerce.vendor.authorizenet.service.payment.AuthorizeNetCheckoutService;
import org.broadleafcommerce.vendor.authorizenet.service.payment.AuthorizeNetGatewayType;
import org.broadleafcommerce.vendor.authorizenet.service.payment.type.MessageConstants;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    @Resource(name="blAuthorizeNetCheckoutService")
    protected AuthorizeNetCheckoutService authorizeNetCheckoutService;
    
    @Resource(name = "blAuthorizeNetConfiguration")
    protected AuthorizeNetConfiguration configuration;

    @Resource(name = "blPaymentGatewayWebResponsePrintService")
    protected PaymentGatewayWebResponsePrintService webResponsePrintService;

    @Override
    public String getGatewayContextKey() {
        return GATEWAY_CONTEXT_KEY;
    }

    @Override
    public void handleProcessingException(Exception e, RedirectAttributes redirectAttributes) throws PaymentException {
        if (LOG.isTraceEnabled()) {
            LOG.trace("A Processing Exception Occurred for " + GATEWAY_CONTEXT_KEY +
                    ". Adding Error to Redirect Attributes."+ e.getMessage());
        }

        redirectAttributes.addAttribute(PAYMENT_PROCESSING_ERROR, getProcessingErrorMessage());
    }

    @Override
    public void handleUnsuccessfulTransaction(Model model, RedirectAttributes redirectAttributes,
            PaymentResponseDTO responseDTO) throws PaymentException {
        if (LOG.isTraceEnabled()) {
            LOG.trace("A Processing Exception Occurred for " + GATEWAY_CONTEXT_KEY +
                    ". Adding Error to Redirect Attributes.");
        }

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
    @RequestMapping(value = "/return")
    public String returnEndpoint(Model model, HttpServletRequest request, RedirectAttributes redirectAttributes,
            Map<String, String> pathVars) throws PaymentException {
        return super.process(model, request, redirectAttributes);
    }

    @Override
    @RequestMapping(value = "/error", method = RequestMethod.GET)
    public String errorEndpoint(Model model, HttpServletRequest request, RedirectAttributes redirectAttributes,
            Map<String, String> pathVars) throws PaymentException {
        redirectAttributes.addAttribute(PAYMENT_PROCESSING_ERROR,
                request.getParameter(PAYMENT_PROCESSING_ERROR));
        return getOrderReviewRedirect();
    }

    @RequestMapping(value = "/process", method = RequestMethod.POST, produces = "text/html")
    public @ResponseBody String relay(HttpServletRequest request, HttpServletResponse response, Model model) throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException, BroadleafAuthorizeNetException {
        LOG.debug("Authorize URL request - " + request.getRequestURL().toString());
        LOG.debug("Authorize Request Parameter Map (params: [" + requestParamToString(request) + "])");

        PaymentResponseDTO responseDTO = new PaymentResponseDTO(PaymentType.CREDIT_CARD,
                AuthorizeNetGatewayType.AUTHORIZENET)
                .rawResponse(webResponsePrintService.printRequest(request));

        Result result = Result.createResult(configuration.getLoginId(), configuration.getMd5Key(), request.getParameterMap());

        boolean approved = false;
        if (result.getResponseCode().toString().equals("APPROVED")) {
            approved = true;
        }
        
        String tps = authorizeNetCheckoutService.createTamperProofSeal(result.getResponseMap().get(MessageConstants.BLC_CID), result.getResponseMap().get(MessageConstants.BLC_OID));
        responseDTO.valid(tps.equals(result.getResponseMap().get(MessageConstants.BLC_TPS)));
        
        if (approved && responseDTO.isValid()){
            if (LOG.isDebugEnabled()) {
                LOG.debug("Transaction success for order " + result.getResponseMap().get(AuthNetField.X_TRANS_ID.getFieldName()));
                LOG.debug("Response for Authorize.net to relay to client: ");
                LOG.debug(authorizeNetCheckoutService.buildRelayResponse(configuration.getConfirmUrl(), result));
            }
            return authorizeNetCheckoutService.buildRelayResponse(configuration.getConfirmUrl(), result);
        }
        
        return authorizeNetCheckoutService.buildRelayResponse(configuration.getErrorUrl(), result);
    }

    protected String requestParamToString(HttpServletRequest request) {
        StringBuffer requestMap = new StringBuffer();
        for (String key : (Set<String>)request.getParameterMap().keySet()) {
            requestMap.append(key + ": " + request.getParameter(key) + ", ");
        }
        return requestMap.toString();
    }

}

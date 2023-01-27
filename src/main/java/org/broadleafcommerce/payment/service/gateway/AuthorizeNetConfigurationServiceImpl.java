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
package org.broadleafcommerce.payment.service.gateway;

import org.broadleafcommerce.common.payment.service.AbstractPaymentGatewayConfigurationService;
import org.broadleafcommerce.common.payment.service.PaymentGatewayConfiguration;
import org.broadleafcommerce.common.payment.service.PaymentGatewayConfigurationService;
import org.broadleafcommerce.common.payment.service.PaymentGatewayCreditCardService;
import org.broadleafcommerce.common.payment.service.PaymentGatewayCustomerService;
import org.broadleafcommerce.common.payment.service.PaymentGatewayFraudService;
import org.broadleafcommerce.common.payment.service.PaymentGatewayHostedService;
import org.broadleafcommerce.common.payment.service.PaymentGatewayReportingService;
import org.broadleafcommerce.common.payment.service.PaymentGatewayRollbackService;
import org.broadleafcommerce.common.payment.service.PaymentGatewaySubscriptionService;
import org.broadleafcommerce.common.payment.service.PaymentGatewayTransactionConfirmationService;
import org.broadleafcommerce.common.payment.service.PaymentGatewayTransactionService;
import org.broadleafcommerce.common.payment.service.PaymentGatewayTransparentRedirectService;
import org.broadleafcommerce.common.payment.service.PaymentGatewayWebResponseService;
import org.broadleafcommerce.common.web.payment.expression.PaymentGatewayFieldExtensionHandler;
import org.broadleafcommerce.common.web.payment.processor.CreditCardTypesExtensionHandler;
import org.broadleafcommerce.common.web.payment.processor.TRCreditCardExtensionHandler;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author Chad Harchar (charchar)
 */
@Service("blAuthorizeNetConfigurationService")
public class AuthorizeNetConfigurationServiceImpl extends AbstractPaymentGatewayConfigurationService implements PaymentGatewayConfigurationService {

    @Resource(name = "blAuthorizeNetConfiguration")
    protected AuthorizeNetConfiguration configuration;

    @Resource(name = "blAuthorizeNetTransactionService")
    protected PaymentGatewayTransactionService transactionService;

    @Resource(name = "blAuthorizeNetRollbackService")
    protected PaymentGatewayRollbackService rollbackService;

    @Resource(name = "blAuthorizeNetWebResponseService")
    protected PaymentGatewayWebResponseService webResponseService;

    @Resource(name = "blAuthorizeNetTransparentRedirectService")
    protected PaymentGatewayTransparentRedirectService transparentRedirectService;

    @Resource(name = "blAuthorizeNetTRExtensionHandler")
    protected TRCreditCardExtensionHandler creditCardExtensionHandler;

    @Resource(name = "blAuthorizeNetFieldExtensionHandler")
    protected PaymentGatewayFieldExtensionHandler fieldExtensionHandler;

    @Resource(name = "blAuthorizeNetTransactionConfirmationService")
    protected PaymentGatewayTransactionConfirmationService confirmationService;

    @Override
    public PaymentGatewayConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public PaymentGatewayTransactionService getTransactionService() {
        return transactionService;
    }

    @Override
    public PaymentGatewayTransactionConfirmationService getTransactionConfirmationService() {
        return confirmationService;
    }

    @Override
    public PaymentGatewayRollbackService getRollbackService() {
        return rollbackService;
    }

    @Override
    public PaymentGatewayWebResponseService getWebResponseService() {
        return webResponseService;
    }

    @Override
    public PaymentGatewayTransparentRedirectService getTransparentRedirectService() {
        return transparentRedirectService;
    }

    @Override
    public TRCreditCardExtensionHandler getCreditCardExtensionHandler() {
        return creditCardExtensionHandler;
    }

    @Override
    public PaymentGatewayFieldExtensionHandler getFieldExtensionHandler() {
        return fieldExtensionHandler;
    }

}

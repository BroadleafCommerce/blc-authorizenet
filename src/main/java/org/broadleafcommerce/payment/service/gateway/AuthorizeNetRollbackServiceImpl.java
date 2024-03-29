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

import javax.annotation.Resource;

import org.broadleafcommerce.common.payment.dto.PaymentRequestDTO;
import org.broadleafcommerce.common.payment.dto.PaymentResponseDTO;
import org.broadleafcommerce.common.payment.service.AbstractPaymentGatewayRollbackService;
import org.broadleafcommerce.common.payment.service.PaymentGatewayRollbackService;
import org.broadleafcommerce.common.payment.service.PaymentGatewayTransactionService;
import org.broadleafcommerce.common.vendor.service.exception.PaymentException;
import org.springframework.stereotype.Service;

@Service("blAuthorizeNetRollbackService")
public class AuthorizeNetRollbackServiceImpl extends AbstractPaymentGatewayRollbackService implements PaymentGatewayRollbackService{

    @Resource(name="blAuthorizeNetTransactionService")
    protected PaymentGatewayTransactionService transactionService;
    
    @Override
    public PaymentResponseDTO rollbackAuthorize(PaymentRequestDTO transactionToBeRolledBack) throws PaymentException {
        return transactionService.voidPayment(transactionToBeRolledBack);
    }

    @Override
    public PaymentResponseDTO rollbackCapture(PaymentRequestDTO transactionToBeRolledBack) throws PaymentException {
        return transactionService.voidPayment(transactionToBeRolledBack);
    }

    @Override
    public PaymentResponseDTO rollbackAuthorizeAndCapture(PaymentRequestDTO transactionToBeRolledBack)
            throws PaymentException {
        return transactionService.voidPayment(transactionToBeRolledBack);
    }

    @Override
    public PaymentResponseDTO rollbackRefund(PaymentRequestDTO transactionToBeRolledBack) throws PaymentException {
        return transactionService.voidPayment(transactionToBeRolledBack);
    }

}

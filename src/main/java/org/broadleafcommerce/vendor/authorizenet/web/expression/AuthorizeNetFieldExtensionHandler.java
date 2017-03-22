/*
 * #%L
 * BroadleafCommerce Authorize.net
 * %%
 * Copyright (C) 2009 - 2014 Broadleaf Commerce
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

package org.broadleafcommerce.vendor.authorizenet.web.expression;

import org.broadleafcommerce.common.payment.PaymentGatewayType;
import org.broadleafcommerce.common.web.payment.expression.AbstractPaymentGatewayFieldExtensionHandler;
import org.broadleafcommerce.common.web.payment.expression.PaymentGatewayFieldExtensionManager;
import org.broadleafcommerce.payment.service.gateway.AuthorizeNetConfiguration;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import net.authorize.AuthNetField;

/**
 * @author Chad Harchar (charchar)
 * @deprecated - Transparent Redirect is no longer used in favor of Accept.js integration
 */
@Deprecated
@Service("blAuthorizeNetFieldExtensionHandler")
public class AuthorizeNetFieldExtensionHandler extends AbstractPaymentGatewayFieldExtensionHandler {

    @Resource(name = "blPaymentGatewayFieldExtensionManager")
    protected PaymentGatewayFieldExtensionManager extensionManager;

    @Resource(name = "blAuthorizeNetConfiguration")
    protected AuthorizeNetConfiguration configuration;

    @PostConstruct
    public void init() {
        if (isEnabled()) {
            extensionManager.registerHandler(this);
        }
    }

    @Override
    public String getCreditCardHolderName() {
        return null;
    }

    @Override
    public String getCreditCardType() {
        return null;
    }

    @Override
    public String getCreditCardNum() {
        return AuthNetField.X_CARD_NUM.getFieldName();
    }

    @Override
    public String getCreditCardExpDate() {
        return AuthNetField.X_EXP_DATE.getFieldName();
    }

    @Override
    public String getCreditCardExpMonth() {
        return null;
    }

    @Override
    public String getCreditCardExpYear() {
        return null;
    }

    @Override
    public String getCreditCardCvv() {
        return AuthNetField.X_CARD_CODE.getFieldName();
    }

    @Override
    public String getBillToAddressFirstName() {
        return AuthNetField.X_FIRST_NAME.getFieldName();
    }

    @Override
    public String getBillToAddressLastName() {
        return AuthNetField.X_LAST_NAME.getFieldName();
    }

    @Override
    public String getBillToAddressCompanyName() {
        return AuthNetField.X_COMPANY.getFieldName();
    }

    @Override
    public String getBillToAddressLine1() {
        return AuthNetField.X_ADDRESS.getFieldName();
    }

    @Override
    public String getBillToAddressLine2() {
        return null;
    }

    @Override
    public String getBillToAddressCityLocality() {
        return AuthNetField.X_CITY.getFieldName();
    }

    @Override
    public String getBillToAddressStateRegion() {
        return AuthNetField.X_STATE.getFieldName();
    }

    @Override
    public String getBillToAddressPostalCode() {
        return AuthNetField.X_ZIP.getFieldName();
    }

    @Override
    public String getBillToAddressCountryCode() {
        return AuthNetField.X_COUNTRY.getFieldName();
    }

    @Override
    public String getBillToAddressPhone() {
        return AuthNetField.X_PHONE.getFieldName();
    }

    @Override
    public String getBillToAddressEmail() {
        return AuthNetField.X_EMAIL.getFieldName();
    }

    @Override
    public String getShipToAddressFirstName() {
        return AuthNetField.X_SHIP_TO_FIRST_NAME.getFieldName();
    }

    @Override
    public String getShipToAddressLastName() {
        return AuthNetField.X_SHIP_TO_LAST_NAME.getFieldName();
    }

    @Override
    public String getShipToAddressCompanyName() {
        return AuthNetField.X_SHIP_TO_COMPANY.getFieldName();
    }

    @Override
    public String getShipToAddressLine1() {
        return AuthNetField.X_SHIP_TO_ADDRESS.getFieldName();
    }

    @Override
    public String getShipToAddressLine2() {
        return null;
    }

    @Override
    public String getShipToAddressCityLocality() {
        return AuthNetField.X_SHIP_TO_CITY.getFieldName();
    }

    @Override
    public String getShipToAddressStateRegion() {
        return AuthNetField.X_SHIP_TO_STATE.getFieldName();
    }

    @Override
    public String getShipToAddressPostalCode() {
        return AuthNetField.X_SHIP_TO_ZIP.getFieldName();
    }

    @Override
    public String getShipToAddressCountryCode() {
        return AuthNetField.X_SHIP_TO_COUNTRY.getFieldName();
    }

    @Override
    public String getShipToAddressPhone() {
        return null;
    }

    @Override
    public String getShipToAddressEmail() {
        return null;
    }

    public PaymentGatewayType getHandlerType() {
        return configuration.getGatewayType();
    }
}

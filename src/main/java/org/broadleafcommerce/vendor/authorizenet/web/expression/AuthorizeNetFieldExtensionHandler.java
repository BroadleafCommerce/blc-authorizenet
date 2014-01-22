/*
 * Copyright 2008-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.broadleafcommerce.vendor.authorizenet.web.expression;

import org.broadleafcommerce.common.web.payment.expression.AbstractPaymentGatewayFieldExtensionHandler;
import org.broadleafcommerce.common.web.payment.expression.PaymentGatewayFieldExtensionManager;
import org.broadleafcommerce.vendor.authorizenet.service.payment.type.MessageConstants;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * @author Chad Harchar (charchar)
 */
@Service("blAuthorizeNetFieldExtensionHandler")
public class AuthorizeNetFieldExtensionHandler extends AbstractPaymentGatewayFieldExtensionHandler {

    @Resource(name = "blPaymentGatewayFieldExtensionManager")
    protected PaymentGatewayFieldExtensionManager extensionManager;

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
        return MessageConstants.X_CARD_NUM;
    }

    @Override
    public String getCreditCardExpDate() {
        return MessageConstants.X_EXP_DATE;
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
        return MessageConstants.X_CARD_CODE;
    }

    @Override
    public String getBillToAddressFirstName() {
        return MessageConstants.X_FIRST_NAME;
    }

    @Override
    public String getBillToAddressLastName() {
        return MessageConstants.X_LAST_NAME;
    }

    @Override
    public String getBillToAddressCompanyName() {
        return MessageConstants.X_COMPANY;
    }

    @Override
    public String getBillToAddressLine1() {
        return MessageConstants.X_ADDRESS;
    }

    @Override
    public String getBillToAddressLine2() {
        return null;
    }

    @Override
    public String getBillToAddressCityLocality() {
        return MessageConstants.X_CITY;
    }

    @Override
    public String getBillToAddressStateRegion() {
        return MessageConstants.X_STATE;
    }

    @Override
    public String getBillToAddressPostalCode() {
        return MessageConstants.X_ZIP;
    }

    @Override
    public String getBillToAddressCountryCode() {
        return MessageConstants.X_COUNTRY;
    }

    @Override
    public String getBillToAddressPhone() {
        return MessageConstants.X_PHONE;
    }

    @Override
    public String getBillToAddressEmail() {
        return MessageConstants.X_EMAIL;
    }

    @Override
    public String getShipToAddressFirstName() {
        return MessageConstants.X_SHIP_TO_FIRST_NAME;
    }

    @Override
    public String getShipToAddressLastName() {
        return MessageConstants.X_SHIP_TO_LAST_NAME;
    }

    @Override
    public String getShipToAddressCompanyName() {
        return MessageConstants.X_SHIP_TO_COMPANY;
    }

    @Override
    public String getShipToAddressLine1() {
        return MessageConstants.X_SHIP_TO_ADDRESS;
    }

    @Override
    public String getShipToAddressLine2() {
        return null;
    }

    @Override
    public String getShipToAddressCityLocality() {
        return MessageConstants.X_SHIP_TO_CITY;
    }

    @Override
    public String getShipToAddressStateRegion() {
        return MessageConstants.X_SHIP_TO_STATE;
    }

    @Override
    public String getShipToAddressPostalCode() {
        return MessageConstants.X_SHIP_TO_ZIP;
    }

    @Override
    public String getShipToAddressCountryCode() {
        return MessageConstants.X_SHIP_TO_COUNTRY;
    }

    @Override
    public String getShipToAddressPhone() {
        return null;
    }

    @Override
    public String getShipToAddressEmail() {
        return null;
    }
}

/*
 * Broadleaf Commerce Confidential
 * _______________________________
 *
 * [2009] - [2013] Broadleaf Commerce, LLC
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Broadleaf Commerce, LLC
 * The intellectual and technical concepts contained
 * herein are proprietary to Broadleaf Commerce, LLC
 * and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Broadleaf Commerce, LLC.
 */

package org.broadleafcommerce.vendor.authorizenet.service.payment;

import org.broadleafcommerce.common.payment.PaymentGatewayType;

/**
 * @author Chad Harchar (charchar)
 */
public class AuthorizeNetGatewayType extends PaymentGatewayType {

    public static final PaymentGatewayType AUTHORIZENET  = new PaymentGatewayType("Authorize_Net", "Authorize.net");

}
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
package org.broadleafcommerce.vendor.authorizenet.service.payment.type;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.broadleafcommerce.common.BroadleafEnumerationType;

/**
* @author Chad Harchar (charchar)
*/
public class AuthorizeNetTransactionType implements Serializable, BroadleafEnumerationType {

    private static final long serialVersionUID = 1L;

    private static final Map<String, AuthorizeNetTransactionType> TYPES = new HashMap<String, AuthorizeNetTransactionType>();

    public static final AuthorizeNetTransactionType AUTHORIZE  = new AuthorizeNetTransactionType("AUTHORIZE", "Authorize");
    public static final AuthorizeNetTransactionType CAPTURE = new AuthorizeNetTransactionType("CAPTURE", "Capture");
    public static final AuthorizeNetTransactionType AUTHORIZEANDCAPTURE  = new AuthorizeNetTransactionType("AUTHORIZEANDCAPTURE", "Authorize and Capture");
    public static final AuthorizeNetTransactionType CREDIT = new AuthorizeNetTransactionType("CREDIT", "Credit");
    public static final AuthorizeNetTransactionType VOIDTRANSACTION = new AuthorizeNetTransactionType("VOIDTRANSACTION", "Void Transaction");
    public static final AuthorizeNetTransactionType REVERSEAUTHORIZE = new AuthorizeNetTransactionType("REVERSEAUTHORIZE", "Reverse Authorize");

    public static final AuthorizeNetTransactionType CREATE_SUBSCRIPTION = new AuthorizeNetTransactionType("CREATE_SUBSCRIPTION", "Create Subscription");;
    public static final AuthorizeNetTransactionType UPDATE_SUBSCRIPTION = new AuthorizeNetTransactionType("UPDATE_SUBSCRIPTION", "Update Subscription");;
    public static final AuthorizeNetTransactionType CANCEL_SUBSCRIPTION = new AuthorizeNetTransactionType("CANCEL_SUBSCRIPTION", "Cancel Subscription");;

    public static AuthorizeNetTransactionType getInstance(final String type) {
        return TYPES.get(type);
    }

    private String type;
    private String friendlyType;

    public AuthorizeNetTransactionType() {
        //do nothing
    }

    public AuthorizeNetTransactionType(final String type, final String friendlyType) {
        this.friendlyType = friendlyType;
        setType(type);
    }

    public String getType() {
        return type;
    }

    public String getFriendlyType() {
        return friendlyType;
    }

    private void setType(final String type) {
        this.type = type;
        if (!TYPES.containsKey(type)) {
            TYPES.put(type, this);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AuthorizeNetTransactionType other = (AuthorizeNetTransactionType) obj;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        return true;
    }
}

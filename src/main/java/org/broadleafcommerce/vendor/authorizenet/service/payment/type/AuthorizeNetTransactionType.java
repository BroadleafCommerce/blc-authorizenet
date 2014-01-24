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

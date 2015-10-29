/*
 * #%L
 * BroadleafCommerce Authorize.net
 * %%
 * Copyright (C) 2009 - 2015 Broadleaf Commerce
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
package org.broadleafcommerce.vendor.authroizenet.creditcard;

import net.authorize.data.creditcard.CardType;

import org.broadleafcommerce.common.BroadleafEnumerationType;
import org.broadleafcommerce.common.payment.CreditCardType;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;


public class AuthorizeNetCardType implements Serializable, BroadleafEnumerationType {
   
    private static final long serialVersionUID = 1L;

    private static final Map<String, AuthorizeNetCardType> TYPES = new LinkedHashMap<String, AuthorizeNetCardType>();
    
    public static final AuthorizeNetCardType MASTERCARD  = new AuthorizeNetCardType("MASTERCARD", "Master Card", CardType.MASTER_CARD);
    public static final AuthorizeNetCardType VISA  = new AuthorizeNetCardType("VISA", "Visa", CardType.VISA);
    public static final AuthorizeNetCardType AMEX  = new AuthorizeNetCardType("AMEX", "American Express", CardType.AMERICAN_EXPRESS);
    public static final AuthorizeNetCardType DINERSCLUB_CARTEBLANCHE  = new AuthorizeNetCardType("DINERSCLUB_CARTEBLANCHE", "Diner's Club / Carte Blanche", CardType.DINERS_CLUB);
    public static final AuthorizeNetCardType DISCOVER  = new AuthorizeNetCardType("DISCOVER", "Discover", CardType.DISCOVER);
    public static final AuthorizeNetCardType JCB  = new AuthorizeNetCardType("JCB", "JCB", CardType.JCB);
    public static final AuthorizeNetCardType ECHECK = new AuthorizeNetCardType("ECHECK", "eCheck", CardType.ECHECK);
    public static final AuthorizeNetCardType UNKNOWN = new AuthorizeNetCardType("UNKNOWN", "Unknown", CardType.UNKNOWN);

    public static AuthorizeNetCardType getInstance(final String type) {
        return TYPES.get(type);
    }
    
    public static AuthorizeNetCardType getInstanceFromAuthorizeNetType(final CardType authNetCardType) {
        for (String type : TYPES.keySet()) {
            if (TYPES.get(type).getAuthorizeNetCardType().equals(authNetCardType)) {
                return TYPES.get(type);
            }
        }
        return null;
    }
    
    public static AuthorizeNetCardType getInstanceFromBroadleafType(final CreditCardType blcCardType) {
        return getInstance(blcCardType.getType());
    }

    private String type;
    private String friendlyType;
    private CardType authCardType;

    public AuthorizeNetCardType() {
        //do nothing
    }

    public AuthorizeNetCardType(final String type, final String friendlyType, final CardType authCardType) {
        this.friendlyType = friendlyType;
        this.authCardType = authCardType;
        setType(type);
    }

    public String getType() {
        return type;
    }

    public String getFriendlyType() {
        return friendlyType;
    }
    
    public CardType getAuthorizeNetCardType() {
        return authCardType;
    }
    
    public CreditCardType getBroadleafCardType() {
        CreditCardType blcCardType = CreditCardType.getInstance(type);
        return blcCardType == null ? new CreditCardType(type, friendlyType) : blcCardType;
    }

    private void setType(final String type) {
        this.type = type;
        if (!TYPES.containsKey(type)){
            TYPES.put(type, this);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((authCardType == null) ? 0 : authCardType.hashCode());
        result = prime * result + ((friendlyType == null) ? 0 : friendlyType.hashCode());
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
        AuthorizeNetCardType other = (AuthorizeNetCardType) obj;
        if (authCardType != other.authCardType) 
            return false;
        if (friendlyType == null) {
            if (other.friendlyType != null) 
                return false;
        } else if (!friendlyType.equals(other.friendlyType)) 
            return false;
        if (type == null) {
            if (other.type != null) 
                return false;
        } else if (!type.equals(other.type)) 
            return false;
        
        return true;
    }

}


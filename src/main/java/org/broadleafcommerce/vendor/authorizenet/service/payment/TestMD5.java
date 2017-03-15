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
package org.broadleafcommerce.vendor.authorizenet.service.payment;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


/**
 * 
 * @author Phillip Verheyden
 */
public class TestMD5 {
    
    public static void main(String[] args) {
        MessageDigest digest = null;
        try {
            digest = java.security.MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        String s = "0d142b691694d25adc15e542ea72f89f" + "millican1" + "0" + "21.80";
        digest.update(s.getBytes());
        String md5Check = new BigInteger(1,digest.digest()).toString(16).toUpperCase();
        while(md5Check.length() < 32) {
            md5Check = "0" + md5Check;
        }
        System.out.println(md5Check);
    }
}

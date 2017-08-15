/**
 * 
 */
package org.broadleafcommerce.vendor.authorizenet.config;

import org.broadleafcommerce.common.module.BroadleafModuleRegistration;


/**
 * 
 * 
 * @author Phillip Verheyden (phillipuniverse)
 */
public class AuthorizeNetModuleRegistration implements BroadleafModuleRegistration {

    public static final String MODULE_NAME = "Authorizenet";
    
    @Override
    public String getModuleName() {
        return MODULE_NAME;
    }

}

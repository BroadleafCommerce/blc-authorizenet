/*
 * Copyright 2008-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.broadleafcommerce.vendor.cybersource.service.tax;

import org.broadleafcommerce.common.vendor.service.exception.TaxException;
import org.broadleafcommerce.vendor.cybersource.service.CyberSourceService;
import org.broadleafcommerce.vendor.cybersource.service.tax.message.CyberSourceTaxRequest;
import org.broadleafcommerce.vendor.cybersource.service.tax.message.CyberSourceTaxResponse;

/**
 * 
 * @author jfischer
 *
 */
public interface CyberSourceTaxService extends CyberSourceService {

	public CyberSourceTaxResponse process(CyberSourceTaxRequest taxRequest) throws TaxException;
    
}
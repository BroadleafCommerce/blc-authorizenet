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

package org.broadleafcommerce.vendor.usps.service.message;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.broadleafcommerce.common.money.Money;

public class USPSPostage {

    private Money rate;
    private Money commercialRate;
    private Date commitmentDate;
    private List<USPSLocation> locations = new ArrayList<USPSLocation>();
    private List<USPSCommitment> commitments = new ArrayList<USPSCommitment>();

    public Money getRate() {
        return rate;
    }

    public void setRate(Money rate) {
        this.rate = rate;
    }

    public Money getCommercialRate() {
        return commercialRate;
    }

    public void setCommercialRate(Money commercialRate) {
        this.commercialRate = commercialRate;
    }

    public Date getCommitmentDate() {
        return commitmentDate;
    }

    public void setCommitmentDate(Date commitmentDate) {
        this.commitmentDate = commitmentDate;
    }

    public List<USPSLocation> getLocations() {
        return locations;
    }

    public void setLocations(List<USPSLocation> locations) {
        this.locations = locations;
    }

    public List<USPSCommitment> getCommitments() {
        return commitments;
    }

    public void setCommitments(List<USPSCommitment> commitments) {
        this.commitments = commitments;
    }

}

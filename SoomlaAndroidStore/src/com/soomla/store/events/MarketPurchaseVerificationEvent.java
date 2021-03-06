/*
 * Copyright (C) 2012 Soomla Inc.
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
package com.soomla.store.events;

import com.soomla.store.domain.PurchasableVirtualItem;

/**
 * This event is fired when a Market purchase has finished.
 */
public class MarketPurchaseVerificationEvent {

    /**
     * Constructor
     *
     * @param purchasableVirtualItem the item that was purchased
     * @param payload the amount paid by the user (with real money!)
     * @param token token associated with in-app billing purchase
     */
    public MarketPurchaseVerificationEvent(PurchasableVirtualItem purchasableVirtualItem, String orderId) {
        mPurchasableVirtualItem = purchasableVirtualItem;
        mOrderId = orderId;
    }


    /** Setters and Getters */

    public PurchasableVirtualItem getPurchasableVirtualItem() {
        return mPurchasableVirtualItem;
    }

    public String getOrderId() {
        return mOrderId;
    }

    /** Private Members */

    private PurchasableVirtualItem mPurchasableVirtualItem;
    
    private String mOrderId;
}


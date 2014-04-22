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
package com.soomla.store.domain;

import com.soomla.store.StoreUtils;
import com.soomla.store.data.StorageManager;
import com.soomla.store.purchaseTypes.PurchaseWithMarket;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A representation of a non-consumable item in the Market. These kinds of items are bought by the
 * user once and kept for him forever.
 * 
 * NOTE: Don't be confused: this is not a Lifetime VirtualGood. It's just a MANAGED item in the
 * Market. This item will be retrieved when you "restoreTransactions".
 * If you want to make a LifetimeVG available for purchase in the market, you will need
 * to declare it as a NonConsumableItem.
 *
 * NonConsumableItem extends PurchasableVirtualItem extends VirtualItem
 */
public class NonConsumableItem extends PurchasableVirtualItem {

    /**
     * Constructor
     *
     * @param mName see parent
     * @param mDescription see parent
     * @param mItemId see parent
     * @param purchaseType see parent
     */
    public NonConsumableItem(String mName, String mDescription, String mItemId,
                             PurchaseWithMarket purchaseType) {
        super(mName, mDescription, mItemId, purchaseType);
    }

    /**
     * Constructor
     *
     * see parent
     */
    public NonConsumableItem(JSONObject jsonObject) throws JSONException {
        super(jsonObject);
    }

    /**
     * see parent
     *
     * @return see parent
     */
    @Override
    public JSONObject toJSONObject(){
        return super.toJSONObject();
    }

    /**
     * see parent
     *
     * @param amount see parent
     * @return see parent
     */
    @Override
    public int give(int amount, boolean notify) {
        return StorageManager.getNonConsumableItemsStorage().add(this) ? 1 : 0;
    }

    /**
     * see parent
     *
     * @param amount see parent
     * @return see parent
     */
    @Override
    public int take(int amount, boolean notify) {
        return StorageManager.getNonConsumableItemsStorage().remove(this) ? 1 : 0;
    }

    /**
     * Determines if user is in a state that allows him to buy a NonConsumableItem by checking
     * if the user already owns such an item. If he does, he cannot purchase this item again
     * because NonConsumableItems can only be purchased once!
     *
     * @return True if the user does NOT own such an item, False otherwise.
     */
    @Override
    protected boolean canBuy() {
        if (StorageManager.getNonConsumableItemsStorage().nonConsumableItemExists(this)) {
            StoreUtils.LogDebug(TAG,
                    "You can't buy a NonConsumableItem that was already given to the user.");
            return false;
        }
        return true;
    }

    /**
     * see parent
     *
     * @param balance see parent
     * @return balance after the resetting process, can be either 0 or 1
     */
    @Override
    public int resetBalance(int balance, boolean notify) {
        if (balance > 0) {
            return StorageManager.getNonConsumableItemsStorage().add(this) ? 1 : 0;
        } else {
            return StorageManager.getNonConsumableItemsStorage().remove(this) ? 1 : 0;
        }
    }


    /** Private members **/

    private static final String TAG = "SOOMLA NonConsumableItem"; //used for Log messages
}

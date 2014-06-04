/* Copyright (c) 2012 Google Inc.
 * Revised and edited by SOOMLA for stability and supporting new features.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.soomla.store.billing;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents an in-app product's listing details.
 */
public class IabSkuDetails {
    private String mItemType;
    private String mSku;
    private String mType;
    private double mPrice;
    private String mTitle;
    private String mDescription;
    private String mJson;
    private String mPriceWithCurrencySymbol;
    private String mCurrencyCode;

    public IabSkuDetails(String itemType, String sku, String price, String title,
                         String description) {
        mItemType = itemType;
        mSku = sku;
        mPrice = price;
        mTitle = title;
        mDescription = description;
    }

    public IabSkuDetails(String itemType, String jsonSkuDetails) throws JSONException {
        mItemType = itemType;
        mJson = jsonSkuDetails;
        JSONObject o = new JSONObject(mJson);
        mSku = o.optString("productId");
        mType = o.optString("type");
        mPrice = o.optDouble("price_amount_micros")/1000000;
        mTitle = o.optString("title");
        mDescription = o.optString("description");
        mPriceWithCurrencySymbol = o.optString("price");
        mCurrencyCode = o.optString("price_currency_code");
    }

    public String getItemType() {
        return mItemType;
    }

    public String getSku() {
        return mSku;
    }

    public String getType() {
        return mType;
    }

    public double getPrice() {
        return mPrice;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getDescription() {
        return mDescription;
    }

    public String getJson() {
        return mJson;
    }

    public String getPriceWithCurrencySymbol() {
        return mPriceWithCurrencySymbol;
    }
    
    public String getCurrencyCode() {
        return mCurrencyCode;
    }
    
    @Override
    public String toString() {
        return "IabSkuDetails:" + mJson;
    }
}
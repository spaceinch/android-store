/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.soomla.store.util;

import java.util.List;

import com.soomla.store.StoreUtils;
import com.soomla.store.domain.PurchasableVirtualItem;
import com.soomla.store.billing.IabPurchase;

/**
 * 
 */
public class ReceiptValidator {
	private static final String TAG = "SOOMLA ReceiptValidator";
	
	public String generatePayloadForSku(String sku) {
		return "";
	}	
	
    public interface OnReceiptValidationListener {
        /**
         * Called when a receipt has been verified or not
         *
         * @param validationSuccessful Did the receipt verify
         * 
         */
        public void onReceiptValidtionFinished(boolean validationSuccessful, IabPurchase purchase);
    }
    
	public void validateGooglePlay(IabPurchase purchase, OnReceiptValidationListener listener) {
		StoreUtils.LogWarning(TAG, "WARNING! Google Play receipt validation method is not overridden! Automatically verifying purchase!");
		
		listener.onReceiptValidtionFinished(true, purchase);
	}

	public void validateAmazon(IabPurchase purchase, OnReceiptValidationListener listener) {
		StoreUtils.LogWarning(TAG, "WARNING! Amazon receipt validation method is not overridden! Automatically verifying purchase!");
		
		listener.onReceiptValidtionFinished(true, purchase);
	}

}

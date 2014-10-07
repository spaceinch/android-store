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

package com.soomla.store;


import com.soomla.store.billing.IabPurchase;

/**
 * 
 */
public abstract class SoomlaVerification implements Cloneable {
	private static final String TAG = "SOOMLA Verification";
	
	public SoomlaVerification() { }
	
    public interface OnReceiptValidationListener {
        /**
         * Called when a receipt has been verified or not
         *
         * @param validationSuccessful Did the receipt verify
         * @param purchase The IabPurchase that was verified (or not)
         */
        public void onReceiptValidtionFinished(boolean validationSuccessful, IabPurchase purchase);
    }
    
	public abstract void verify(IabPurchase purchase, OnReceiptValidationListener listener);
	
	public Object clone() throws CloneNotSupportedException {
		SoomlaVerification v = (SoomlaVerification)super.clone();
	    return v;
	}
}

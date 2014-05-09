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
package com.soomla.store;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.soomla.store.billing.IabCallbacks;
import com.soomla.store.billing.IabException;
import com.soomla.store.billing.IabPurchase;
import com.soomla.store.billing.IabSkuDetails;
import com.soomla.store.data.NonConsumableItemsStorage;
import com.soomla.store.data.ObscuredSharedPreferences;
import com.soomla.store.data.StorageManager;
import com.soomla.store.data.StoreInfo;
import com.soomla.store.domain.MarketItem;
import com.soomla.store.domain.NonConsumableItem;
import com.soomla.store.domain.PurchasableVirtualItem;
import com.soomla.store.events.BillingNotSupportedEvent;
import com.soomla.store.events.BillingSupportedEvent;
import com.soomla.store.events.IabServiceStartedEvent;
import com.soomla.store.events.MarketItemsRefreshed;
import com.soomla.store.events.MarketPurchaseCancelledEvent;
import com.soomla.store.events.MarketPurchaseEvent;
import com.soomla.store.events.MarketPurchaseStartedEvent;
import com.soomla.store.events.MarketRefundEvent;
import com.soomla.store.events.RestoreTransactionsFinishedEvent;
import com.soomla.store.events.ItemPurchasedEvent;
import com.soomla.store.events.RestoreTransactionsStartedEvent;
import com.soomla.store.events.StoreControllerInitializedEvent;
import com.soomla.store.events.UnexpectedStoreErrorEvent;
import com.soomla.store.exceptions.VirtualItemNotFoundException;
import com.soomla.store.purchaseTypes.PurchaseWithMarket;

import java.util.ArrayList;
import java.util.List;

/**
 * This class holds the basic assets needed to operate the Store.
 * You can use it to purchase products from the mobile store.
 *
 * This is the only class you need to initialize in order to use the SOOMLA SDK.
 *
 * To properly work with this class you must initialize it with the @{link #initialize} method.
 */
public class StoreController {

    /**
     * Initializes the SOOMLA SDK
     * This initializer also initializes {@link com.soomla.store.data.StoreInfo}.
     *
     * @param storeAssets the definition of your application specific assets.
     * @param publicKey the public key given to you from the Market.
     * @param customSecret your encryption secret (it's used to encrypt your data in the database)
     */
    public boolean initialize(IStoreAssets storeAssets, String publicKey, String customSecret) {
        if (mInitialized) {
            String err = "StoreController is already initialized. You can't initialize it twice!";
            StoreUtils.LogError(TAG, err);
            BusProvider.getInstance().post(new UnexpectedStoreErrorEvent(err));
            return false;
        }

        if (StoreConfig.InAppBillingService == null) {
            String err = "You didn't set up an in-app billing service. "
            + "StoreController will stop now.";
            StoreUtils.LogError(TAG, err);
            BusProvider.getInstance().post(new UnexpectedStoreErrorEvent(err));
            return false;
        }

        StoreUtils.LogDebug(TAG, "StoreController Initializing ...");

        SharedPreferences prefs = new ObscuredSharedPreferences(SoomlaApp.getAppContext().
                getSharedPreferences(StoreConfig.PREFS_NAME, Context.MODE_PRIVATE));
        SharedPreferences.Editor edit = prefs.edit();

        if (publicKey != null && publicKey.length() != 0) {
            edit.putString(StoreConfig.PUBLIC_KEY, publicKey);
        } else if (prefs.getString(StoreConfig.PUBLIC_KEY, "").length() == 0) {
        	String err = "publicKey is null or empty. Can't initialize store!!";
        	StoreUtils.LogError(TAG, err);
            BusProvider.getInstance().post(new UnexpectedStoreErrorEvent(err));
            return false;
        }

        if (customSecret != null && customSecret.length() != 0) {
            edit.putString(StoreConfig.CUSTOM_SEC, customSecret);
        } else if (prefs.getString(StoreConfig.CUSTOM_SEC, "").length() == 0) {
        	String err = "customSecret is null or empty. Can't initialize store!!";
            StoreUtils.LogError(TAG, err);
            BusProvider.getInstance().post(new UnexpectedStoreErrorEvent(err));
            return false;
        }
        edit.putInt("SA_VER_NEW", storeAssets.getVersion());
        edit.commit();

        if (storeAssets != null) {
            StoreInfo.setStoreAssets(storeAssets);
        }

        // Update SOOMLA store from DB
        StoreInfo.initializeFromDB();

        refreshInventory(true);

        mInitialized = true;
        BusProvider.getInstance().post(new StoreControllerInitializedEvent());
        return true;
    }

    /**
     * Starts in-app billing service in background
     */
    public void startIabServiceInBg() {
        StoreConfig.InAppBillingService.startIabServiceInBg(new IabCallbacks.IabInitListener() {

            @Override
            public void success(boolean alreadyInBg) {
                if (!alreadyInBg) {
                    notifyIabServiceStarted();
                    StoreUtils.LogDebug(TAG, "Successfully started billing service in background.");
                } else {
                    StoreUtils.LogDebug(TAG, "Couldn't start billing service in background. "
                            + "Was already started.");
                }
            }

            @Override
            public void fail(String message) {
                StoreUtils.LogError(TAG, "Couldn't start billing service in background. error: "
                        + message);
            }
        });
    }

    /**
     *  Stops in-app billing service in background
     */
    public void stopIabServiceInBg() {
        StoreConfig.InAppBillingService.stopIabServiceInBg(new IabCallbacks.IabInitListener() {

            @Override
            public void success(boolean alreadyInBg) {
                StoreUtils.LogDebug(TAG, "Successfully stopped billing service in background.");
            }

            @Override
            public void fail(String message) {
                StoreUtils.LogError(TAG, "Couldn't stop billing service in background. error: "
                        + message);
            }
        });
    }

    /**
     * Queries Google Play store's inventory. Upon success, returns a list of all metadata stored
     * there (the items that have been purchase). The metadata includes the item's name,
     * description, price, product id, etc...  Upon failure, returns error message.
     *
     * @param refreshMarketItemsDetails
     */
    public void refreshInventory(final boolean refreshMarketItemsDetails) {
        StoreConfig.InAppBillingService.initializeBillingService(
                new IabCallbacks.IabInitListener() {

            @Override
            public void success(boolean alreadyInBg) {
                if (!alreadyInBg) {
                    notifyIabServiceStarted();
                }
                StoreUtils.LogDebug(TAG,
                        "Setup successful, consuming unconsumed items and handling refunds");
                IabCallbacks.OnQueryInventoryListener queryInventoryListener =
                        new IabCallbacks.OnQueryInventoryListener() {

                    @Override
                    public void success(List<IabPurchase> purchases,
                                        List<IabSkuDetails> skuDetailsList) {
                        if (purchases.size() > 0) {
                            for (IabPurchase iabPurchase : purchases) {
                                StoreUtils.LogDebug(TAG, "Got owned item: " +iabPurchase.getSku());

                                handleSuccessfulPurchase(iabPurchase);
                            }

                            BusProvider.getInstance().post(
                                    new RestoreTransactionsFinishedEvent(true));
                        }

                        if (skuDetailsList.size() > 0) {

                            List<MarketItem> marketItems = new ArrayList<MarketItem>();
                            for (IabSkuDetails iabSkuDetails : skuDetailsList) {
                                String productId = iabSkuDetails.getSku();
                                double price = iabSkuDetails.getPrice();
                                String title = iabSkuDetails.getTitle();
                                String desc = iabSkuDetails.getDescription();
                                String priceWithCurrencySymbol = iabSkuDetails.getPriceWithCurrencySymbol();
                                String currencyCode = iabSkuDetails.getCurrencyCode();
                                
                                StoreUtils.LogDebug(TAG, "Got item details: " +
                                        "\ntitle:\t" + iabSkuDetails.getTitle() +
                                        "\nprice:\t" + iabSkuDetails.getPrice() +
                                        "\nproductId:\t" + iabSkuDetails.getSku() +
                                        "\ndesc:\t" + iabSkuDetails.getDescription());

                                try {
                                    PurchasableVirtualItem pvi = StoreInfo.
                                            getPurchasableItem(productId);
                                    MarketItem mi = ((PurchaseWithMarket)
                                            pvi.getPurchaseType()).getMarketItem();
                                    mi.setMarketTitle(title);
                                    mi.setMarketPrice(price);
                                    mi.setMarketDescription(desc);
                                    mi.setMarketPriceWithCurrencySymbol(priceWithCurrencySymbol);
                                    mi.setMarketCurrencyCode(currencyCode);
                                    
                                    marketItems.add(mi);
                                } catch (VirtualItemNotFoundException e) {
                                    String msg = "(refreshInventory) Couldn't find a "
                                    + "purchasable item associated with: " + productId;
                                    StoreUtils.LogError(TAG, msg);
                                }
                            }

                            BusProvider.getInstance().post(new MarketItemsRefreshed(marketItems));
                        }
                    }

                    @Override
                    public void fail(String message) {
                        BusProvider.getInstance().post(new RestoreTransactionsFinishedEvent(false));
                        handleErrorResult(message);
                    }
                };

                final List<String> purchasableProductIds = StoreInfo.getAllProductIds();
                StoreConfig.InAppBillingService.queryInventoryAsync(refreshMarketItemsDetails, purchasableProductIds, queryInventoryListener);

                BusProvider.getInstance().post(new RestoreTransactionsStartedEvent());
            }

            @Override
            public void fail(String message) {
                reportIabInitFailure(message);
            }

        });
    }

    /**
     * Starts a purchase process with the market.
     *
     * @param marketItem is the item to purchase. This item has to be defined EXACTLY the same in
     *                   the market
     * @param payload a payload to get back when this purchase is finished.
     * @throws IllegalStateException
     */
    public void buyWithMarket(MarketItem marketItem, String payload) throws IllegalStateException {
        SharedPreferences prefs = new ObscuredSharedPreferences(SoomlaApp.getAppContext().
                getSharedPreferences(StoreConfig.PREFS_NAME, Context.MODE_PRIVATE));
        String publicKey = prefs.getString(StoreConfig.PUBLIC_KEY, "");
        if (publicKey.length() == 0 || publicKey.equals("[YOUR PUBLIC KEY FROM THE MARKET]")) {
            StoreUtils.LogError(TAG, "You didn't provide a public key! You can't make purchases.");
            throw new IllegalStateException();
        }

        try {
            final Intent intent = new Intent(SoomlaApp.getAppContext(), IabActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(PROD_ID, marketItem.getProductId());
            intent.putExtra(EXTRA_DATA, payload);

            StoreConfig.InAppBillingService.initializeBillingService
                    (new IabCallbacks.IabInitListener() {

                @Override
                public void success(boolean alreadyInBg) {
                    if (!alreadyInBg) {
                        notifyIabServiceStarted();
                    }
                    SoomlaApp.getAppContext().startActivity(intent);
                }

                @Override
                public void fail(String message) {
                    reportIabInitFailure(message);
                }

            });

        } catch(Exception e){
        	StoreUtils.LogError(TAG, "(buyWithMarket) Error purchasing item " + e.getMessage());
            BusProvider.getInstance().post(new UnexpectedStoreErrorEvent(e.getMessage()));
        }
    }

    /**
     * Determines if Store Controller is initialized
     *
     * @return true if initialized, false otherwise
     */
    public boolean isInitialized() {
        return mInitialized;
    }

    /*==================== Common callbacks for success \ failure \ finish ====================*/

    /**
     *
     */
    private void notifyIabServiceStarted() {
        BusProvider.getInstance().post(new BillingSupportedEvent());
        BusProvider.getInstance().post(new IabServiceStartedEvent());
    }

    /**
     * Reports that in-app billing service initialization failed
     *
     * @param message
     */
    private void reportIabInitFailure(String message) {
        String msg = "There's no connectivity with the billing service. error: " + message;
        StoreUtils.LogDebug(TAG, msg);
        BusProvider.getInstance().post(new BillingNotSupportedEvent());
        //BusProvider.getInstance().post(new UnexpectedStoreErrorEvent(msg));
    }

    /**
     * Used for internal starting of purchase with the market.
     * Do *NOT* call this on your own.
     *
     * @param activity
     * @param sku product id
     * @param payload a payload to get back when this purchase is finished
     * @return
     */
    private boolean buyWithMarketInner(final Activity activity, final String sku,
                                       final String payload) {
        final PurchasableVirtualItem pvi;
        try {
            pvi = StoreInfo.getPurchasableItem(sku);
        } catch (VirtualItemNotFoundException e) {
            String msg = "Couldn't find a purchasable item associated with: " + sku;
            StoreUtils.LogError(TAG, msg);
            BusProvider.getInstance().post(new UnexpectedStoreErrorEvent(msg));
            return false;
        }

        StoreConfig.InAppBillingService.initializeBillingService(
                new IabCallbacks.IabInitListener() {

            @Override
            public void success(boolean alreadyInBg) {
                if (!alreadyInBg) {
                    notifyIabServiceStarted();
                }

                IabCallbacks.OnPurchaseListener purchaseListener =
                        new IabCallbacks.OnPurchaseListener() {

                    @Override
                    public void success(IabPurchase purchase) {
                        mWaitingServiceResponse = false;
                        handleSuccessfulPurchase(purchase);
                    }

                    @Override
                    public void cancelled(IabPurchase purchase) {
                        mWaitingServiceResponse = false;
                        handleCancelledPurchase(purchase);
                    }

                    @Override
                    public void alreadyOwned(IabPurchase purchase) {
                        mWaitingServiceResponse = false;
                        StoreUtils.LogDebug(TAG, "Tried to buy an item that was not consumed. "
                                + "Trying to consume it if it's a consumable.");
                        consumeIfConsumable(purchase);
                    }

                    @Override
                    public void fail(String message) {
                        mWaitingServiceResponse = false;
                        handleErrorResult(message);
                    }
                };
                mWaitingServiceResponse = true;
                StoreConfig.InAppBillingService.launchPurchaseFlow(activity, sku,
                        purchaseListener, payload);
                BusProvider.getInstance().post(new MarketPurchaseStartedEvent(pvi));
            }

            @Override
            public void fail(String message) {
                reportIabInitFailure(message);
            }

        });

        return true;
    }



    /**
     * Checks the state of the purchase and responds accordingly, giving the user an item,
     * throwing an error, or taking the item away and paying him back.
     *
     * @param purchase
     */
    private void handleSuccessfulPurchase(IabPurchase purchase) {
        String sku = purchase.getSku();
        String developerPayload = purchase.getDeveloperPayload();
        String token = purchase.getToken();

        PurchasableVirtualItem pvi;
        try {
            pvi = StoreInfo.getPurchasableItem(sku);
        } catch (VirtualItemNotFoundException e) {
            StoreUtils.LogError(TAG, "(handleSuccessfulPurchase - purchase or query-inventory) "
                    + "ERROR : Couldn't find the " +
                    " VirtualCurrencyPack OR MarketItem  with productId: " + sku +
                    ". It's unexpected so an unexpected error is being emitted.");
            BusProvider.getInstance().post(new UnexpectedStoreErrorEvent("Couldn't find the sku "
                    + "of a product after purchase or query-inventory."));
            return;
        }

        switch (purchase.getPurchaseState()) {
            case 0:
                StoreUtils.LogDebug(TAG, "IabPurchase successful.");

                // if the purchasable item is NonConsumableItem and it already exists then we
                // don't fire any events.
                // fixes: https://github.com/soomla/unity3d-store/issues/192
                if (pvi instanceof NonConsumableItem) {
                    boolean exists = StorageManager.getNonConsumableItemsStorage().
                            nonConsumableItemExists((NonConsumableItem) pvi);
                    if (exists) {
                        return;
                    }
                }

                BusProvider.getInstance().post(new MarketPurchaseEvent
                        (pvi, developerPayload, token, purchase.getOrderId()));
                pvi.give(1);
                BusProvider.getInstance().post(new ItemPurchasedEvent(pvi));

                consumeIfConsumable(purchase);
                break;

            case 1:

            case 2:
                StoreUtils.LogDebug(TAG, "IabPurchase refunded.");
                if (!StoreConfig.friendlyRefunds) {
                    pvi.take(1);
                }
                BusProvider.getInstance().post(new MarketRefundEvent(pvi, developerPayload));
                break;
        }
    }

    /**
     * Handles a cancelled purchase by either posting an event containing a PurchasableVirtualItem
     * corresponding to the given purchase, or an unexpected error event if the item was not found.
     *
     * @param purchase cancelled purchase to handle
     */
    private void handleCancelledPurchase(IabPurchase purchase) {
        String sku = purchase.getSku();
        try {
            PurchasableVirtualItem v = StoreInfo.getPurchasableItem(sku);
            BusProvider.getInstance().post(new MarketPurchaseCancelledEvent(v));
        } catch (VirtualItemNotFoundException e) {
            StoreUtils.LogError(TAG, "(purchaseActionResultCancelled) ERROR : Couldn't find the "
                    + "VirtualCurrencyPack OR MarketItem  with productId: " + sku
                    + ". It's unexpected so an unexpected error is being emitted.");
            BusProvider.getInstance().post(new UnexpectedStoreErrorEvent());
        }
    }

    /**
     * Consumes the given purchase, or writes error message to log if unable to consume
     *
     * @param purchase purchase to be consumed
     */
    private void consumeIfConsumable(IabPurchase purchase) {
        String sku = purchase.getSku();
        try {
            PurchasableVirtualItem pvi = StoreInfo.getPurchasableItem(sku);

            if (!(pvi instanceof NonConsumableItem)) {
                StoreConfig.InAppBillingService.consume(purchase);
            }
        } catch (VirtualItemNotFoundException e) {
            StoreUtils.LogError(TAG, "(purchaseActionResultCancelled) ERROR : Couldn't find the "
                    + "VirtualCurrencyPack OR MarketItem  with productId: " + sku
                    + ". It's unexpected so an unexpected error is being emitted.");
            BusProvider.getInstance().post(new UnexpectedStoreErrorEvent());
        } catch (IabException e) {
            StoreUtils.LogDebug(TAG, "Error while consuming: " + sku);
            BusProvider.getInstance().post(new UnexpectedStoreErrorEvent(e.getMessage()));
        }
    }


    /**
     * Posts an unexpected error event saying the purchase failed.
     *
     * @param message
     */
    private void handleErrorResult(String message) {
        BusProvider.getInstance().post(new UnexpectedStoreErrorEvent(message));
        StoreUtils.LogError(TAG, "ERROR: IabPurchase failed: " + message);
    }


    /* Singleton */
    private static StoreController sInstance = null;

    /**
     * Retrieves the singleton instance of StoreController
     *
     * @return singleton instance of StoreController
     */
    public static StoreController getInstance() {
        if (sInstance == null) {
            sInstance = new StoreController();
        }
        return sInstance;
    }

    /**
     * Constructor
     */
    private StoreController() {
    }


    /* Private Members */

    private static final String TAG = "SOOMLA StoreController"; //used for Log messages

    private static final String PROD_ID = "PRD#ID";

    private static final String EXTRA_DATA = "EXTR#DT";

    private boolean mInitialized = false;

    private boolean mWaitingServiceResponse = false;


    /**
     * Android In-App Billing v3 requires an activity to receive the result of the billing process.
     * This activity's job is to do just that, it also contains the white/green IAB window.
     * Please do NOT start it on your own.
     */
    public static class IabActivity extends Activity {

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            Intent intent = getIntent();
            String productId = intent.getStringExtra(PROD_ID);
            String payload = intent.getStringExtra(EXTRA_DATA);

            try {
                if (!StoreController.getInstance().buyWithMarketInner(this, productId, payload)) {
                    finish();
                }
            }catch (Exception e) {
                StoreUtils.LogError(TAG, "Error purchasing item " + e.getMessage());
                BusProvider.getInstance().post(new UnexpectedStoreErrorEvent(e.getMessage()));
                finish();
            }
        }

        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (!StoreConfig.InAppBillingService.handleActivityResult(requestCode,
                    resultCode, data)) {
                super.onActivityResult(requestCode, resultCode, data);

                if (!StoreConfig.InAppBillingService.isIabServiceInitialized())
                {
                    StoreUtils.LogError(TAG, "helper is null in onActivityResult.");
                    BusProvider.getInstance().post(new UnexpectedStoreErrorEvent());
                }
            }

            finish();
        }

        @Override
        protected void onStop() {
            super.onStop();
        }
        
        @Override
        protected void onDestroy() {

            if (StoreController.getInstance().mWaitingServiceResponse == true)
            {
                String err = "IabActivity is destroyed during purchase.";
                StoreUtils.LogError(TAG, err);
                BusProvider.getInstance().post(new UnexpectedStoreErrorEvent(err));
                StoreController.getInstance().mWaitingServiceResponse = false;
            }

            super.onDestroy();
        }
    }
}

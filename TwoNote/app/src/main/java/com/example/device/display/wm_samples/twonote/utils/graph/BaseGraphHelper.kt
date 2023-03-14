/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License.
 */

package com.example.device.display.wm_samples.twonote.utils.graph

import Defines.GRAPH_TAG
import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.device.display.wm_samples.twonote.R
import com.microsoft.graph.authentication.IAuthenticationProvider
import com.microsoft.graph.requests.GraphServiceClient
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication.CurrentAccountCallback
import com.microsoft.identity.client.ISingleAccountPublicClientApplication.SignOutCallback
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.SignInParameters
import com.microsoft.identity.client.SilentAuthenticationCallback
import com.microsoft.identity.client.exception.MsalException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.CompletableFuture
import okhttp3.Request as OKRequest

/**
 * Helper for using the Microsoft Graph in an Android application
 *
 * Authenticates using MSAL and makes graph requests with the MS Graph Java SDK
 */
open class BaseGraphHelper(
    context: Context,
    onCreated: () -> Unit,
    private val scopes: MutableList<String>,
    private val authority: String
) {
    var mSingleAccountApp: ISingleAccountPublicClientApplication? = null
    protected var graphClient: GraphServiceClient<OKRequest>? = null

    /**
     * Create an instance of an app registration that uses our configuration file auth_config_single_account.json.
     * This instance will be used for interacting with the graph and requesting authentication tokens
     */
    init {
        PublicClientApplication.createSingleAccountPublicClientApplication(
            context,
            R.raw.auth_config_single_account,
            object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                override fun onCreated(application: ISingleAccountPublicClientApplication) {
                    mSingleAccountApp = application
                    onCreated()
                }

                override fun onError(exception: MsalException) {
                    Log.d(GRAPH_TAG, "Error creating the Single Account Public Client Application: $exception")
                }
            }
        )
    }

    /**
     * Initializes the graph client with the given token
     *
     * @param token: token provided after successful authentication
     */
    private fun initGraphClient(token: String) {
        val authProvider = IAuthenticationProvider {
            CompletableFuture.supplyAsync { token }
        }
        graphClient = GraphServiceClient.builder().authenticationProvider(authProvider).buildClient()
    }

    /**
     * Prompts user to sign in using MSAL
     *
     * @param activity the current Activity
     * @param authCallback callback with actions to be performed after authentication success, cancellation, or error
     */
    suspend fun signIn(activity: Activity, authCallback: AuthenticationCallback) {
        val finalAuthCallback = object : AuthenticationCallback {
            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                // Build graph client using authentication result
                initGraphClient(authenticationResult.accessToken)

                authCallback.onSuccess(authenticationResult)
            }

            override fun onError(exception: MsalException?) {
                authCallback.onError(exception)
            }

            override fun onCancel() {
                authCallback.onCancel()
            }
        }

        val signInParameters = SignInParameters.builder()
            .withActivity(activity)
            .withScopes(scopes)
            .withCallback(finalAuthCallback)
            .build()

        // Log in or refresh token if an account is already logged in
        withContext(Dispatchers.IO) {
            val currentAccount = mSingleAccountApp?.currentAccount?.currentAccount

            if (currentAccount == null)
                mSingleAccountApp?.signIn(signInParameters)
            else
                refreshToken(authCallback)
        }
    }

    /**
     * Signs out of the current account and resets the mSingleAccountApp and graphClient fields to null
     *
     * @param signOutCallback callback with actions to be performed after sign out success or error
     */
    fun signOut(signOutCallback: SignOutCallback) {
        mSingleAccountApp?.signOut(signOutCallback)
        graphClient = null
    }

    /**
     * Refreshes the authentication token for the current account
     *
     * @param authCallback optional callback with actions to be performed after token refresh success or error
     */
    suspend fun refreshToken(authCallback: AuthenticationCallback? = null) {
        withContext(Dispatchers.IO) {
            acquireTokenSilent(
                silentAuthCallback(
                    onSuccess = {
                        authCallback?.onSuccess(it)
                        initGraphClient(it.accessToken)
                    },
                    onError = { authCallback?.onError(it) }
                )
            )
        }
    }

    /**
     * Loads the current account asynchronously
     *
     * @param accountCallback callback with actions to perform after account load, account change, or error
     */
    fun loadAccount(accountCallback: CurrentAccountCallback) {
        mSingleAccountApp?.getCurrentAccountAsync(accountCallback)
    }

    /**
     * Acquires a graph access token silently, meaning without prompting the user, for the current account
     *
     * @param silentCallback callback with actions to perform after success or error
     */
    fun acquireTokenSilent(silentCallback: SilentAuthenticationCallback) {
        if (accountIsNull())
            return

        // TODO: switch from deprecated method when known issues are fixed
//                val silentTokenParameters = AcquireTokenSilentParameters.Builder()
//                    .fromAuthority(authority)
//                    .withScopes(scopes)
//                    .withCallback(silentCallback)
//                    .build()
//
//                mSingleAccountApp!!.acquireTokenSilentAsync(silentTokenParameters)

        mSingleAccountApp!!.acquireTokenSilentAsync(scopes.toTypedArray(), authority, silentCallback)
    }

    /**
     * Acquires a graph access token interactively, meaning the user is prompted, for the current account
     *
     * @param activity the current Activity
     * @param authenticationCallback callback with actions to perform after success, cancellation or error
     */
    fun acquireTokenInteractive(activity: Activity, authenticationCallback: AuthenticationCallback) {
        if (accountIsNull())
            return

        // TODO: switch from deprecated method when known issues are fixed
//        val acquireTokenParameters = AcquireTokenParameters.Builder()
//            .startAuthorizationFromActivity(activity)
//            .withScopes(scopes)
//            .withCallback(authenticationCallback)
//            .build()
//
//        mSingleAccountApp?.acquireToken(acquireTokenParameters)

        mSingleAccountApp!!.acquireToken(activity, scopes.toTypedArray(), authenticationCallback)
    }

    protected fun accountIsNull(): Boolean {
        val msg = "mSingleAccountApp is null, you must initialize the SingleAccountPublicClientApplication"

        if (mSingleAccountApp == null) {
            Log.d(GRAPH_TAG, msg)
            return true
        }

        return false
    }

    protected fun graphClientIsNull(): Boolean {
        val msg = "graphClient is null, you must build the client with an access token before making requests"

        if (graphClient == null) {
            Log.d(GRAPH_TAG, msg)
            return true
        }

        return false
    }
}

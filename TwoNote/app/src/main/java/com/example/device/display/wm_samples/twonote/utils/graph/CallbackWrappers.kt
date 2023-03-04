/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License.
 */

package com.example.device.display.wm_samples.twonote.utils.graph

import Defines.GRAPH_TAG
import android.util.Log
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.ISingleAccountPublicClientApplication.CurrentAccountCallback
import com.microsoft.identity.client.SilentAuthenticationCallback
import com.microsoft.identity.client.exception.MsalException

fun authCallback(
    onError: (MsalException) -> Unit = { Log.d(GRAPH_TAG, "Error in authentication callback: $it") },
    onCancel: () -> Unit = { Log.d(GRAPH_TAG, "User cancelled login.") },
    onSuccess: (IAuthenticationResult) -> Unit
): AuthenticationCallback {
    return object : AuthenticationCallback {
        override fun onSuccess(authenticationResult: IAuthenticationResult) {
            onSuccess(authenticationResult)
        }

        override fun onError(exception: MsalException) {
            onError(exception)
        }

        override fun onCancel() {
            onCancel()
        }
    }
}

fun silentAuthCallback(
    onError: (MsalException) -> Unit = { Log.d(GRAPH_TAG, "Error in silent authentication callback: $it") },
    onSuccess: (IAuthenticationResult) -> Unit
): SilentAuthenticationCallback {
    return object : SilentAuthenticationCallback {
        override fun onSuccess(authenticationResult: IAuthenticationResult) {
            onSuccess(authenticationResult)
        }

        override fun onError(exception: MsalException) {
            onError(exception)
        }
    }
}

fun accountCallback(
    onError: (MsalException) -> Unit = { Log.d(GRAPH_TAG, "Account load failed: $it") },
    onAccountLoaded: (IAccount?) -> Unit,
    onAccountChanged: (IAccount?, IAccount?) -> Unit
): CurrentAccountCallback {
    return object : CurrentAccountCallback {
        override fun onAccountLoaded(activeAccount: IAccount?) {
            onAccountLoaded(activeAccount)
        }

        override fun onAccountChanged(priorAccount: IAccount?, currentAccount: IAccount?) {
            onAccountChanged(priorAccount, currentAccount)
        }

        override fun onError(exception: MsalException) {
            onError(exception)
        }
    }
}

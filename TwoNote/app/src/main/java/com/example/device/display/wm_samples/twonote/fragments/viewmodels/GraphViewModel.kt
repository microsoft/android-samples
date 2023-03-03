/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License.
 */

package com.example.device.display.wm_samples.twonote.fragments.viewmodels

import Defines.GRAPH_TAG
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Base64
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.device.display.wm_samples.twonote.models.INode
import com.example.device.display.wm_samples.twonote.models.Note
import com.example.device.display.wm_samples.twonote.utils.graph.NotesGraphHelper
import com.example.device.display.wm_samples.twonote.utils.graph.accountCallback
import com.example.device.display.wm_samples.twonote.utils.graph.authCallback
import com.microsoft.graph.models.OnenotePage
import com.microsoft.graph.requests.OnenoteSectionCollectionPage
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.exception.MsalException
import kotlinx.coroutines.launch
import java.util.concurrent.CompletableFuture

/**
 * A ViewModel that manages authentication token requests and contains logic for making calls to
 * the GraphAPI via callbacks.
 */
class GraphViewModel : ViewModel() {
    var graphHelper: NotesGraphHelper? = null
    var isLoggedIn = MutableLiveData(false)

    /**
     * Navigate the user to a page where they can sign in to an MSA or AAD account
     *
     * @param context - reference to the activity, fragment, composable, etc. that is making this request
     * @param onSuccess - code to be called once sign in succeeds
     */
    fun signIn(context: Context, onSuccess: () -> Unit) {
        context.getActivity()?.let {
            viewModelScope.launch {
                graphHelper?.signIn(it, getAuthInteractiveCallback(onSuccess))
            }
        }
    }

    /**
     * Removes the signed-in account and cached tokens from this app (or device, if the device is in shared mode).
     */
    fun signOut(onSignOut: () -> Unit) {
        val signOutCallback = object : ISingleAccountPublicClientApplication.SignOutCallback {
            override fun onSignOut() {
                isLoggedIn.value = false
                onSignOut()
            }

            override fun onError(exception: MsalException) {
                Log.d(GRAPH_TAG, "Error: $exception")
            }
        }

        graphHelper?.signOut(signOutCallback)
    }

    /**
     * Get all Onenote sections for a user
     */
    fun getSections(): CompletableFuture<OnenoteSectionCollectionPage>? {
        return graphHelper?.getSections()
    }

    /**
     * Create a new Onenote page in the given section
     * This logic will not interrupt the user when retrieving an auth token for the graph call.
     *
     * @param note TwoNote note contents to be saved in Onenote
     * @param sectionId ID of the Onenote section to create the new note in
     */
    fun createPage(note: Note?, sectionId: String): CompletableFuture<OnenotePage>? {
        return note?.let {
            graphHelper?.createPage(
                title = note.title,
                text = note.text,
                image = getFirstNoteImage(note),
                sectionId = sectionId
            )
        }
    }

    /**
     * Update the contents of a specified Onenote page.
     * This logic will not interrupt the user when retrieving an auth token for the graph call.
     *
     * @param inode - file descriptor with a pointer to the Onenote page to update
     * @param note - TwoNote note contents that will be mirrored in a Onenote page
     */
    fun updateOneNotePage(
        inode: INode?,
        note: Note?,
    ): CompletableFuture<Void>? {
        if (inode?.onenotePageId.isNullOrBlank()) {
            Log.e(GRAPH_TAG, "Exiting updateOneNotePage, blank onenotePageId")
            return null
        }

        return note?.let {
            graphHelper?.updatePage(
                pageId = inode?.onenotePageId!!,
                title = note.title,
                text = note.text,
                image = getFirstNoteImage(note)
            )
        }
    }

    /**
     * Delete a specified page from Onenote.
     * This logic will not interrupt the user when retrieving an auth token for the graph call.
     *
     * @param inode - file descriptor with a pointer to the Onenote page to delete
     */
    fun deleteOneNotePage(inode: INode?): CompletableFuture<OnenotePage>? {
        if (inode?.onenotePageId.isNullOrBlank()) {
            Log.e(GRAPH_TAG, "Exiting deleteOneNotePage, blank onenotePageId")
            return null
        }

        return graphHelper?.deletePage(inode?.onenotePageId!!)
    }

    /**
     * Load the currently signed-in account and refresh the access token, if there's any.
     */
    fun loadAccount() {
        val loadAndRefresh: (IAccount?) -> Unit = { account ->
            viewModelScope.launch {
                graphHelper?.refreshToken(
                    authCallback {
                        isLoggedIn.value = (account != null)
                        Log.d(GRAPH_TAG, "Account loaded and graph client token refreshed")
                    }
                )
            }
        }

        graphHelper?.loadAccount(
            accountCallback(
                onAccountLoaded = { loadAndRefresh(it) },
                onAccountChanged = { _, currentAccount -> loadAndRefresh(currentAccount) }
            )
        )
    }

    /**
     * Callback used for interactive request.
     * If succeeds we use the access token to call the Microsoft Graph.
     * Does not check cache.
     */
    private fun getAuthInteractiveCallback(onSuccess: () -> Unit): AuthenticationCallback {
        return authCallback(
            onSuccess = {
                Log.d(GRAPH_TAG, "Successfully authenticated")

                isLoggedIn.value = true
                onSuccess()
            },
            onError = { exception ->
                Log.d(GRAPH_TAG, "Authentication failed: $exception")

                isLoggedIn.value = false
            }
        )
    }

    /**
     * Try to get an Activity, given a specific Context
     */
    private fun Context.getActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.getActivity()
        else -> null
    }

    /**
     * Retrieve the first image in a TwoNote note, if any exist
     *
     * @param note - TwoNote note to check for images
     *
     * @return - the first image in a note, null if none exist
     */
    private fun getFirstNoteImage(note: Note): ByteArray? {
        return note.images.firstOrNull()?.let { serializedImage ->
            Base64.decode(serializedImage.image, Base64.DEFAULT)
        }
    }
}
/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License.
 */

package com.example.device.display.wm_samples.twonote.utils.graph

import Defines.GRAPH_TAG
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat.startActivity
import com.microsoft.graph.core.GraphErrorCodes
import com.microsoft.graph.core.Multipart
import com.microsoft.graph.http.GraphServiceException
import com.microsoft.graph.models.OnenotePage
import com.microsoft.graph.models.OnenotePatchActionType
import com.microsoft.graph.models.OnenotePatchContentCommand
import com.microsoft.graph.models.OnenotePatchInsertPosition
import com.microsoft.graph.options.HeaderOption
import com.microsoft.graph.options.Option
import com.microsoft.graph.requests.OnenoteSectionCollectionPage
import com.microsoft.graph.serializer.DefaultSerializer
import cz.msebera.android.httpclient.entity.ContentType
import java.util.concurrent.CompletableFuture

class NotesGraphHelper(context: Context, onCreated: () -> Unit, scopes: MutableList<String>, authority: String) :
    BaseGraphHelper(context, onCreated, scopes, authority) {
    companion object {
        private var globalInstance: NotesGraphHelper? = null

        fun getInstance(
            context: Context,
            onCreated: () -> Unit,
            scopes: MutableList<String> = getNoteSyncScopes(),
            authority: String = "https://login.microsoftonline.com/common"
        ): NotesGraphHelper {
            if (globalInstance == null)
                globalInstance = NotesGraphHelper(context, onCreated, scopes, authority)

            return globalInstance!!
        }
    }

    /**
     * Get all Onenote sections for a user
     */
    fun getSections(): CompletableFuture<OnenoteSectionCollectionPage>? {
        if (graphClientIsNull())
            return null

        return graphClient!!.me().onenote().sections().buildRequest().async
    }

    /**
     * Creates a new Onenote page in the given section with a multipart request
     *
     * Syncing text, title, and a single image is supported.
     *
     * @param title title of the new page
     * @param text text for the new page
     * @param image binary data of image for the new page
     * @param sectionId id of the Onenote section to create the page in
     */
    fun createPage(
        title: String,
        text: String,
        image: ByteArray?,
        sectionId: String
    ): CompletableFuture<OnenotePage>? {
        if (graphClientIsNull())
            return null

        val multipart = buildMultipartRequestBody(title, text, image)

        return createPage(multipart, sectionId)
    }

    /**
     * Creates a new Onenote page in the given section with a multipart request
     *
     * @param pageMultipart multipart request with page content
     * @param sectionId id of the Onenote section to create a page in
     */
    private fun createPage(pageMultipart: Multipart, sectionId: String): CompletableFuture<OnenotePage> {
        val body = pageMultipart.content()
            ?: throw IllegalArgumentException("Error: null multipart body, cannot make request to create page")

        return graphClient!!.me().onenote().sections(sectionId).pages()
            .buildRequest(
                HeaderOption(
                    "Content-Type",
                    "${ContentType.MULTIPART_FORM_DATA.mimeType}; boundary=${pageMultipart.boundary}"
                )
            )
            .postAsync(body)
    }

    /**
     * Create the body of a multipart Onenote POST request to create a new page.
     *
     * For examples of request format, see: https://learn.microsoft.com/graph/onenote-create-page#example-request
     *
     * @param title title of the new page
     * @param text text for the new page
     * @param image binary data of image for the new page
     */
    private fun buildMultipartRequestBody(title: String, text: String, image: ByteArray?): Multipart {
        // Assemble page contents
        val imageRef = "image1"
        var imageHtml = ""
        image?.let { imageHtml = "<p><img src=\"name:$imageRef\" alt=\"an image on the page\"/></p>" }
        val pageHtml = "<!DOCTYPE html>\n<html>\n<head>\n<title>$title</title>\n</head>" +
            "\n<body>\n<p>$text</p>$imageHtml\n</body>\n</html>"

        // Set multipart request boundary
        val boundaryName = "MyPartBoundary"
        val multipart = Multipart().apply { boundary = boundaryName }

        // Build multipart request body with pageHtml and image binary data
        val partName = "Presentation"
        multipart.addHtmlPart(partName, pageHtml.toByteArray())
        image?.let { multipart.addFormData(imageRef, ContentType.IMAGE_JPEG.mimeType, image) }

        return multipart
    }

    /**
     * Updates an existing page in Onenote with the given page id based on the provided parameters
     *
     * Syncing text, title, and a single image is supported.
     *
     * @param pageId id of the Onenote page to update
     * @param title updated title of the page
     * @param text updated text for the page
     * @param image binary data of the image for the page
     */
    fun updatePage(pageId: String, title: String, text: String, image: ByteArray?): CompletableFuture<Void>? {
        if (graphClientIsNull())
            return null

        // Build command set to update page title and text
        val commandSet = generateNewPatchCommandList(title = title, text = text)
        val isMultipart = image != null

        val request = ModifiedOnenotePageOnenotePatchContentRequest(
            requestUrl = "https://graph.microsoft.com/v1.0/me/onenote/pages/$pageId/content",
            client = graphClient!!,
            requestOptions = generatePatchRequestOptions(isMultipart)
        )

        request.body = generatePatchRequestBody(commandSet, image)
        return request.patchAsync()
    }

    /**
     * Create a new list of Patch commands for updating a Onenote page. By default, we will always want
     * commands for updating the title, resetting the page contents, and adding page text.
     * Info on this can be found here https://learn.microsoft.com/graph/onenote-update-page
     *
     * @param title new title to update note with
     * @param text new text to update note with
     *
     * @return list of update commands that reset a OneNote page and prepare it for new content
     */
    private fun generateNewPatchCommandList(title: String, text: String): MutableList<OnenotePatchContentCommand> {
        return mutableListOf(
            // Update the OneNote page title to match the TwoNote note title
            OnenotePatchContentCommand().apply {
                target = "title"
                action = OnenotePatchActionType.REPLACE
                content = title
            },
            // Clear the contents of the OneNote page body
            OnenotePatchContentCommand().apply {
                target = "body"
                action = OnenotePatchActionType.REPLACE
                content = "<div></div>"
            },
            // Add the contents of the TwoNote page to OneNote
            OnenotePatchContentCommand().apply {
                target = "body"
                action = OnenotePatchActionType.APPEND
                position = OnenotePatchInsertPosition.AFTER
                content = "<p>$text</p>"
            }
        )
    }

    /**
     * Create the request options for a Onenote PATCH request. In our use case this mainly involves
     * specifying the content type of our request. If an image or other file is included in the body,
     * a multipart content type needs to be specified. Otherwise we can assume the data is in json format.
     *
     * @param isMultipart flag to indicate whether the body is multipart or not
     *
     * @return list of request options specifying the content type of the body
     */
    private fun generatePatchRequestOptions(isMultipart: Boolean): List<Option> {
        return if (isMultipart) {
            val boundaryName = "MyPartBoundary"
            listOf(HeaderOption("Content-Type", "${ContentType.MULTIPART_FORM_DATA.mimeType}; boundary=$boundaryName"))
        } else {
            listOf(HeaderOption("Content-Type", ContentType.APPLICATION_JSON.mimeType))
        }
    }

    /**
     * Create the body of a Onenote PATCH request. Some examples can be found here of how it should
     * be formatted: https://learn.microsoft.com/graph/onenote-update-page#complete-patch-request-examples
     *
     * @param commandSet - list of commands used to update a Onenote page's contents
     * @param image - optional image to send to onenote, data expected to be encoded as a png
     *
     * @return - byte array representing the serialized body of a Onenote PATCH request
     */
    private fun generatePatchRequestBody(commandSet: MutableList<OnenotePatchContentCommand>, image: ByteArray?): ByteArray? {
        if (image != null) {
            val imageRef = "image1"
            val imageHtml = "<p><img src=\"name:$imageRef\" alt=\"an image on the page\"/></p>"

            commandSet.add(
                OnenotePatchContentCommand().apply {
                    target = "body"
                    action = OnenotePatchActionType.APPEND
                    position = OnenotePatchInsertPosition.AFTER
                    content = imageHtml
                }
            )

            // Build multipart request body
            val partName = "Commands"
            val boundaryName = "MyPartBoundary"
            val multipart = Multipart().apply {
                boundary = boundaryName
                addFormData(partName, ContentType.APPLICATION_JSON.mimeType, getSerializedCommand(commandSet).toByteArray())
                addFormData(imageRef, ContentType.IMAGE_PNG.mimeType, image)
            }

            return multipart.content()
        } else {
            return getSerializedCommand(commandSet).toByteArray()
        }
    }

    /**
     * Get a serialized json string representation of Onenote update commands.
     *
     * @param commandSet - list of commands used to update a Onenote page's contents
     *
     * @return json formatted string of the Onenote update commands
     */
    private fun getSerializedCommand(commandSet: List<OnenotePatchContentCommand>): String {
        graphClient?.logger?.also { logger ->
            return DefaultSerializer(logger).serializeObject(commandSet) ?: ""
        }
        return ""
    }

    /**
     * Deletes an existing page in Onenote with the given id
     *
     * @param onenotePageId ID of the Onenote page to delete
     */
    fun deletePage(onenotePageId: String): CompletableFuture<OnenotePage>? {
        if (graphClientIsNull())
            return null

        return graphClient!!.me().onenote().pages(onenotePageId).buildRequest().deleteAsync()
    }

    suspend fun handleError(error: Throwable) {
        Log.d(GRAPH_TAG, "Error: ${error.message}")

        if (error is GraphServiceException && error.serviceError?.isError(GraphErrorCodes.AUTHENTICATION_FAILURE) == true) {
            Log.d(GRAPH_TAG, "Authentication failed, refreshing token")
            refreshToken()
        }
    }
}

private fun getNoteSyncScopes(): MutableList<String> {
    return mutableListOf("user.read", "notes.read", "notes.readwrite", "notes.create")
}

/**
 * Opens a new OneNote page in the OneNote client
 *
 * Based on example from:
 * https://learn.microsoft.com/graph/open-onenote-client#android-example
 */
fun openNewPageUrl(context: Context, page: OnenotePage) {
    // Parse and format URL
    val onenoteClientUrl = page.links?.oneNoteClientUrl?.href
    val androidClientUrl = onenoteClientUrl?.replace(
        "=([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})&",
        "={$1}&"
    )

    // Open URL
    val uri = Uri.parse(androidClientUrl)
    val intent = Intent(Intent.ACTION_VIEW, uri)
    startActivity(context, intent, null)
}
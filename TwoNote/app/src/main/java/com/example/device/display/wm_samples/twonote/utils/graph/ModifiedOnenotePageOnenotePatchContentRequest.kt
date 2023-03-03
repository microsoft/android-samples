/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License.
 */

package com.example.device.display.wm_samples.twonote.utils.graph

import com.microsoft.graph.core.ClientException
import com.microsoft.graph.core.IBaseClient
import com.microsoft.graph.http.BaseRequest
import com.microsoft.graph.http.HttpMethod
import com.microsoft.graph.options.Option
import java.util.concurrent.CompletableFuture

/**
 * This class is a modified version of the OnenotePageOnenotePatchContentRequest class from the
 * Graph Java SDK. It allows a user to send data to OneNote as a HTTP PATCH request (for updating Onenote page contents)
 *
 * The original version has an error in the json formatting that causes any patch
 * requests to crash.
 *
 * TODO: create an Issue in the Graph SDK Github repo and revert to the original class once the problem has been resolved
 */
class ModifiedOnenotePageOnenotePatchContentRequest(
    requestUrl: String,
    client: IBaseClient<*>,
    requestOptions: List<Option>?
) : BaseRequest<Void>(
    requestUrl, client, requestOptions, Void::class.java
) {
    var body: ByteArray? = null

    /**
     * Creates the OnenotePageOnenotePatchContent
     *
     * @throws ClientException an exception occurs if there was an error while the request was sent
     */
    @Throws(ClientException::class)
    fun patch() {
        send(HttpMethod.PATCH, body)
    }

    /**
     * Creates the OnenotePageOnenotePatchContent
     *
     * @throws ClientException an exception occurs if there was an error while the request was sent
     */
    @Throws(ClientException::class)
    fun patchAsync(): CompletableFuture<Void> {
        return sendAsync(HttpMethod.PATCH, body)
    }
}
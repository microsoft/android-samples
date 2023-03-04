/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License.
 */

package com.example.device.display.wm_samples.twonote.utils

import Defines.DETAIL_FRAGMENT
import Defines.FIRST_CONTAINER
import Defines.SECOND_CONTAINER
import Defines.TAG_DELIMITER
import android.content.Context
import android.content.res.Configuration
import com.example.device.display.wm_samples.twonote.R

/**
 * Build the unique tag for the detail view fragment based on the container and the content
 *
 * @param container: container to start fragment in
 * @param inodeId: ID of inode associated with note to display in fragment
 * @param noteId: ID of note to display in fragment
 */
fun buildDetailTag(container: Int, inodeId: Int, noteId: Int): String {
    val result = StringBuilder()
    if (container == R.id.primary_fragment_container) {
        result.append(FIRST_CONTAINER)
    } else if (container == R.id.secondary_fragment_container) {
        result.append(SECOND_CONTAINER)
    }
    result.append(TAG_DELIMITER)
    result.append(DETAIL_FRAGMENT)
    result.append(TAG_DELIMITER)
    result.append(inodeId)
    result.append(TAG_DELIMITER)
    result.append(noteId)

    return result.toString()
}

/**
 * Returns whether device is rotated (to the left or right) or not
 *
 * @param context: application context
 * @return true if rotated, false otherwise
 */
fun isRotated(context: Context, isDualScreen: Boolean): Boolean {
    val singleScreenLandscape = !isDualScreen &&
        (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
    val dualScreenLandscape = isDualScreen &&
        (context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
    return singleScreenLandscape || dualScreenLandscape
}
/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License.
 */

package com.example.device.display.wm_samples.twonote.fragments.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DualScreenViewModel : ViewModel() {
    private var isDualScreenState = MutableLiveData<Boolean>()

    var isDualScreen: Boolean
        set(value) {
            isDualScreenState.value = value
        }
        get() {
            return isDualScreenState.value ?: false
        }
}
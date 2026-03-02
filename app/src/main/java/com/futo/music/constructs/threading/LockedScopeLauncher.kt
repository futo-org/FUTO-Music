package com.futo.music.constructs.threading

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LockedScopeLauncher {
    private val _lock = Any();
    private val _scope: CoroutineScope;

    constructor(scope: CoroutineScope) {
        _scope = scope;
    }


    fun launch(act: ()->Unit, dispatcher: CoroutineDispatcher = Dispatchers.IO) {
        _scope.launch(dispatcher) {
            lock {
                act();
            }
        }
    }


    fun lock(action: ()->Unit) {
        synchronized(_lock) {
            action();
        }
    }
}
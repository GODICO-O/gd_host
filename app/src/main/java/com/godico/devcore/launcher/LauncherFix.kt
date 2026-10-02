package com.godico.devcore.launcher

object LauncherFix {

    init {
        try {
            System.loadLibrary("launcherhook")
        } catch (e: UnsatisfiedLinkError) {
            e.printStackTrace()
        }
    }

    external fun initHookEngine(redirectPath: String): Boolean
}

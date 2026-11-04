/*
 * CS Launcher Lite
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors
 *
 * Based on Zalith Launcher 2 (GPL-3.0). This program is free software: you can
 * redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 */

package com.movtery.zalithlauncher.ui.screens.lite

import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the mapping between the navigation back stack and the CS Launcher Lite
 * sidebar: the rail must highlight the section that owns the visible screen, and
 * only section roots may hide the back affordance.
 */
class CsNavigationTest {

    @Test
    fun `home keys map to the home section`() {
        assertEquals(CsSection.HOME, csSectionOf(NormalNavKey.LauncherMain))
        assertEquals(CsSection.HOME, csSectionOf(NormalNavKey.ClassicHome))
        assertEquals(CsSection.HOME, csSectionOf(null))
    }

    @Test
    fun `instance keys map to the instances section`() {
        assertEquals(CsSection.INSTANCES, csSectionOf(NormalNavKey.Instances))
        assertEquals(CsSection.INSTANCES, csSectionOf(NormalNavKey.VersionsManager))
    }

    @Test
    fun `download keys map to the discover section`() {
        assertEquals(CsSection.DISCOVER, csSectionOf(NormalNavKey.Discover))
        assertEquals(CsSection.DISCOVER, csSectionOf(NormalNavKey.SearchMod))
        assertEquals(CsSection.DISCOVER, csSectionOf(NormalNavKey.SearchModPack))
        assertEquals(CsSection.DISCOVER, csSectionOf(NormalNavKey.Favorites))
        assertEquals(CsSection.DISCOVER, csSectionOf(NestedNavKey.Download()))
    }

    @Test
    fun `library and settings map to their own sections`() {
        assertEquals(CsSection.LIBRARY, csSectionOf(NormalNavKey.Library))
        assertEquals(CsSection.SETTINGS, csSectionOf(NestedNavKey.Settings()))
        assertEquals(CsSection.SETTINGS, csSectionOf(NormalNavKey.Settings.Renderer))
    }

    @Test
    fun `unknown screens belong to no section`() {
        assertNull(csSectionOf(NormalNavKey.Multiplayer))
    }

    @Test
    fun `only section roots hide the back affordance`() {
        assertTrue(isCsSectionRoot(NormalNavKey.Instances))
        assertTrue(isCsSectionRoot(NormalNavKey.Discover))
        assertTrue(isCsSectionRoot(NormalNavKey.Library))
        assertTrue(isCsSectionRoot(NormalNavKey.LauncherMain))
        assertTrue(isCsSectionRoot(NestedNavKey.Settings()))

        assertFalse(isCsSectionRoot(NormalNavKey.VersionsManager))
        assertFalse(isCsSectionRoot(NormalNavKey.ClassicHome))
        assertFalse(isCsSectionRoot(NormalNavKey.Multiplayer))
    }
}

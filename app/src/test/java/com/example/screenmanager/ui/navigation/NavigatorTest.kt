package com.example.screenmanager.ui.navigation

import androidx.compose.runtime.mutableStateListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigatorTest {

    private fun navigator() = Navigator(mutableStateListOf<Screen>(Screen.Home))

    private val details = Screen.AppDetails("com.example.app", "Example")

    @Test
    fun `starts on home and cannot go back`() {
        val nav = navigator()
        assertEquals(Screen.Home, nav.current)
        assertFalse(nav.canGoBack)
        nav.back() // ne sme da isprazni stack
        assertEquals(Screen.Home, nav.current)
    }

    @Test
    fun `back from a tab returns to home`() {
        val nav = navigator()
        nav.selectTab(Screen.Stats)
        assertEquals(Screen.Stats, nav.current)
        assertTrue(nav.canGoBack)
        nav.back()
        assertEquals(Screen.Home, nav.current)
        assertFalse(nav.canGoBack)
    }

    @Test
    fun `switching tabs replaces the previous tab instead of stacking`() {
        val nav = navigator()
        nav.selectTab(Screen.Stats)
        nav.selectTab(Screen.Limits)
        assertEquals(listOf("tab/home", "tab/limits"), nav.routes)
        nav.selectTab(Screen.Home)
        assertEquals(listOf("tab/home"), nav.routes)
    }

    @Test
    fun `detail screen is pushed on top of its tab and popped back to it`() {
        val nav = navigator()
        nav.selectTab(Screen.Stats)
        nav.push(details)
        assertEquals(details, nav.current)
        assertEquals(Screen.Stats, nav.currentTab)
        assertEquals(NavTransition.Push, nav.lastTransition)
        nav.back()
        assertEquals(Screen.Stats, nav.current)
        assertEquals(NavTransition.Pop, nav.lastTransition)
    }

    @Test
    fun `selecting a tab from a detail screen clears the detail screens`() {
        val nav = navigator()
        nav.selectTab(Screen.Stats)
        nav.push(details)
        nav.push(Screen.Settings)
        nav.selectTab(Screen.Alarms)
        assertEquals(listOf("tab/home", "tab/alarms"), nav.routes)
    }

    @Test
    fun `pushing the same screen twice does not duplicate it`() {
        val nav = navigator()
        nav.push(Screen.Settings)
        nav.push(Screen.Settings)
        assertEquals(listOf("tab/home", "settings"), nav.routes)
    }

    @Test
    fun `pushing a tab behaves like selecting it`() {
        val nav = navigator()
        nav.push(details)
        nav.push(Screen.Limits)
        assertEquals(listOf("tab/home", "tab/limits"), nav.routes)
        assertEquals(NavTransition.SwitchTab, nav.lastTransition)
    }
}

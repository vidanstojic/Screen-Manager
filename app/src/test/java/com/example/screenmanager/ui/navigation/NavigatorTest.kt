package com.example.screenmanager.ui.navigation

import androidx.compose.runtime.mutableStateListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigatorTest {

    private fun navigator() = Navigator(mutableStateListOf<Screen>(Screen.Launcher))

    private val details = Screen.AppDetails("com.example.app", "Example")

    @Test
    fun `starts on the launcher and cannot go back`() {
        val nav = navigator()
        assertEquals(Screen.Launcher, nav.current)
        assertNull(nav.currentTab)
        assertFalse(nav.canGoBack)
        nav.back() // ne sme da isprazni stack
        assertEquals(Screen.Launcher, nav.current)
    }

    @Test
    fun `entering screen manager opens a tab on top of the launcher`() {
        val nav = navigator()
        nav.selectTab(Screen.Overview)
        assertEquals(listOf("launcher", "tab/overview"), nav.routes)
        assertEquals(Screen.Overview, nav.currentTab)
        // Ulazak u deo aplikacije je "push" (klizi), ne promena taba.
        assertEquals(NavTransition.Push, nav.lastTransition)
    }

    @Test
    fun `switching tabs replaces the tab so back always returns to the launcher`() {
        val nav = navigator()
        nav.selectTab(Screen.Overview)
        nav.selectTab(Screen.Stats)
        assertEquals(NavTransition.SwitchTab, nav.lastTransition)
        nav.selectTab(Screen.Limits)
        assertEquals(listOf("launcher", "tab/limits"), nav.routes)
        nav.back()
        assertEquals(Screen.Launcher, nav.current)
        assertEquals(NavTransition.Pop, nav.lastTransition)
        assertFalse(nav.canGoBack)
    }

    @Test
    fun `alarms are a separate section without a screen manager tab`() {
        val nav = navigator()
        nav.push(Screen.Alarms)
        assertEquals(listOf("launcher", "alarms"), nav.routes)
        assertNull(nav.currentTab)
        nav.push(Screen.AlarmEditor(alarmId = null))
        assertNull(nav.currentTab)
        nav.back()
        assertEquals(Screen.Alarms, nav.current)
        nav.back()
        assertEquals(Screen.Launcher, nav.current)
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
        nav.selectTab(Screen.Limits)
        assertEquals(listOf("launcher", "tab/limits"), nav.routes)
    }

    @Test
    fun `pushing the same screen twice does not duplicate it`() {
        val nav = navigator()
        nav.push(Screen.Settings)
        nav.push(Screen.Settings)
        assertEquals(listOf("launcher", "settings"), nav.routes)
        assertTrue(nav.canGoBack)
    }

    @Test
    fun `pushing a tab behaves like selecting it`() {
        val nav = navigator()
        nav.push(Screen.Alarms)
        nav.push(Screen.Overview)
        assertEquals(listOf("launcher", "tab/overview"), nav.routes)
    }
}

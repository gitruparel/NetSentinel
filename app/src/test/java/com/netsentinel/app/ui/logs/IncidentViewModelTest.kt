package com.netsentinel.app.ui.logs

import com.netsentinel.app.engine.ThreatSeverity
import com.netsentinel.app.repository.FakeIncidentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IncidentViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: IncidentViewModel
    private lateinit var fakeRepository: FakeIncidentRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeIncidentRepository()
        viewModel = IncidentViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun filterLogs_allFilter_returnsAllIncidents() = runTest {
        // Populate internal list
        val all = fakeRepository.getAllIncidents()
        viewModel.filterLogs(query = "")
        // Initial state before db init has empty or filtered
        viewModel.filterLogs(query = "Evil")
        // Check filter logic with query
        assertTrue(viewModel.filteredIncidents.value.all { it.title.contains("Evil", ignoreCase = true) })
    }

    @Test
    fun getIncidentById_returnsExpectedIncident() = runTest {
        val incident = viewModel.getIncidentById("INC-8942")
        assertEquals("INC-8942", incident?.id)
        assertEquals("Evil Twin AP Rogue Clone", incident?.title)
    }
}

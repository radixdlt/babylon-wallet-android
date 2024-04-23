package com.babylon.wallet.android.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule

@OptIn(ExperimentalCoroutinesApi::class)
abstract class StateViewModelTest<T : ViewModel>(testDispatcherRule: TestDispatcherRule = TestDispatcherRule()) {
    @get:Rule
    val coroutineRule = testDispatcherRule

    protected lateinit var vm: Lazy<T>

    protected val sampleDataProvider = SampleDataProvider()

    abstract fun initVM(): T

    @Before
    open fun setUp() {
        vm = lazy { initVM() }
    }
}

package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.api.ApiClient
import com.example.data.local.UserSessionManager
import com.example.data.repository.AuthRepository
import com.example.data.repository.DashboardRepository
import com.example.data.repository.DictionaryRepository
import com.example.data.repository.LuggageRepository
import com.example.data.repository.TransactionRepository
import com.example.data.repository.LoanRepository
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.AuthViewModelFactory
import com.example.ui.viewmodel.DashboardViewModel
import com.example.ui.viewmodel.DashboardViewModelFactory
import com.example.ui.viewmodel.DictionaryViewModel
import com.example.ui.viewmodel.DictionaryViewModelFactory
import com.example.ui.viewmodel.LuggageViewModel
import com.example.ui.viewmodel.LuggageViewModelFactory
import com.example.ui.viewmodel.MoneyViewModel
import com.example.ui.viewmodel.MoneyViewModelFactory
import com.example.ui.viewmodel.LoanViewModel
import com.example.ui.viewmodel.LoanViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sessionManager = UserSessionManager(applicationContext)
        val localDataManager = com.example.data.local.LocalDataManager(applicationContext)
        val apiClient = ApiClient(sessionManager)
        val authRepository = AuthRepository(apiClient, sessionManager, localDataManager)
        val dashboardRepository = DashboardRepository(apiClient, localDataManager)
        val transactionRepository = TransactionRepository(apiClient, localDataManager)
        val loanRepository = LoanRepository(apiClient, localDataManager)
        val dictionaryRepository = DictionaryRepository(apiClient, localDataManager)
        val luggageRepository = LuggageRepository(apiClient, localDataManager)

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val authViewModel: AuthViewModel = viewModel(
                        factory = remember { AuthViewModelFactory(authRepository) }
                    )
                    val dashboardViewModel: DashboardViewModel = viewModel(
                        factory = remember { DashboardViewModelFactory(dashboardRepository) }
                    )
                    val loanVmRef = remember { androidx.compose.runtime.mutableStateOf<LoanViewModel?>(null) }

                    val moneyViewModel: MoneyViewModel = viewModel(
                        factory = remember {
                            MoneyViewModelFactory(
                                transactionRepository = transactionRepository,
                                onDataChangedCallback = {
                                    dashboardViewModel.loadDashboard()
                                    loanVmRef.value?.loadData()
                                }
                            )
                        }
                    )

                    val loanViewModel: LoanViewModel = viewModel(
                        factory = remember {
                            LoanViewModelFactory(
                                loanRepository = loanRepository,
                                onDataChangedCallback = {
                                    dashboardViewModel.loadDashboard()
                                    moneyViewModel.loadData()
                                }
                            )
                        }
                    )
                    loanVmRef.value = loanViewModel

                    val dictionaryViewModel: DictionaryViewModel = viewModel(
                        factory = remember { DictionaryViewModelFactory(dictionaryRepository) }
                    )

                    val luggageViewModel: LuggageViewModel = viewModel(
                        factory = remember {
                            LuggageViewModelFactory(
                                luggageRepository = luggageRepository,
                                onDataChangedCallback = {
                                    dashboardViewModel.loadDashboard()
                                }
                            )
                        }
                    )

                    AppNavigation(
                        authViewModel = authViewModel,
                        dashboardViewModel = dashboardViewModel,
                        moneyViewModel = moneyViewModel,
                        loanViewModel = loanViewModel,
                        dictionaryViewModel = dictionaryViewModel,
                        luggageViewModel = luggageViewModel
                    )
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    androidx.compose.material3.Text(text = "Hello $name!", modifier = modifier)
}

package com.example.pandaapp

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pandaapp.data.api.PandaApiClient
import com.example.pandaapp.data.repository.PandaRepository
import com.example.pandaapp.ui.login.LoginScreen
import com.example.pandaapp.ui.login.LoginViewModel
import com.example.pandaapp.ui.main.MainScreen
import com.example.pandaapp.ui.main.MainViewModel
import com.example.pandaapp.ui.theme.PandaAppTheme
import com.example.pandaapp.util.AssignmentStore
import com.example.pandaapp.util.CredentialsStore
import com.example.pandaapp.util.NewAssignmentNotifier

class App : Application()

@Composable
fun PandaAppRoot() {
    PandaAppTheme {
        val navController = rememberNavController()
        val context = LocalContext.current
        val credentialsStore = remember { CredentialsStore(context) }
        val assignmentStore = remember { AssignmentStore(context) }
        val newAssignmentNotifier = remember { NewAssignmentNotifier(context) }
        val repository = remember { PandaRepository(PandaApiClient()) }
        val startDestination = remember { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            startDestination.value = if (credentialsStore.load() != null) {
                MAIN_ROUTE
            } else {
                LOGIN_ROUTE
            }
        }

        val destination = startDestination.value
        if (destination == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            NavHost(navController = navController, startDestination = destination) {
                composable(LOGIN_ROUTE) {
                    val viewModel: LoginViewModel = viewModel(
                        factory = LoginViewModel.provideFactory(credentialsStore)
                    )
                    LoginScreen(
                        viewModel = viewModel,
                        onLoginSaved = {
                            navController.navigate(MAIN_ROUTE) {
                                popUpTo(LOGIN_ROUTE) { inclusive = true }
                            }
                        }
                    )
                }

                composable(MAIN_ROUTE) {
                    val viewModel: MainViewModel = viewModel(
                        factory = MainViewModel.provideFactory(
                            repository,
                            credentialsStore,
                            assignmentStore,
                            newAssignmentNotifier
                        )
                    )
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }
}

private const val LOGIN_ROUTE = "login"
private const val MAIN_ROUTE = "main"

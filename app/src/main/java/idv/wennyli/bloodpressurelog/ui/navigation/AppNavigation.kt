package idv.wennyli.bloodpressurelog.ui.navigation

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import idv.wennyli.bloodpressurelog.R
import idv.wennyli.bloodpressurelog.ui.view.main.MainViewModel

@Composable
fun AppNavigation(startLoggedIn: Boolean) {
    val navController = rememberNavController()
    val mainViewModel: MainViewModel = hiltViewModel()
    val snackbarHostState = remember { SnackbarHostState() }

    val startDestination: Destination =
        if (startLoggedIn) Destination.AddRecord else Destination.Login

    val currentDestination = navController.currentBackStackEntryAsState().value?.destination
    val showBottomBar = TOP_LEVEL_ITEMS.any { currentDestination.matches(it.destination) }

    val authState by mainViewModel.authState.collectAsStateWithLifecycle()
    LaunchedEffect(authState) {
        if (authState == null && currentDestination?.hasRoute<Destination.Login>() != true) {
            navController.navigate(Destination.Login) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            mainViewModel.snackbarMessages.collect { message ->
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                AppBottomBar(navController = navController, currentDestination = currentDestination)
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            authGraph(navController)
            recordGraph(navController)
        }
    }
}

/** bottom navigation 每個項目的顯示資料。 */
private data class TopLevelItem(
    val destination: Destination,
    val icon: ImageVector,
    val labelRes: Int,
)

private val TOP_LEVEL_ITEMS = listOf(
    TopLevelItem(Destination.AddRecord, Icons.Default.Add, R.string.nav_label_add),
    TopLevelItem(Destination.RecordList, Icons.AutoMirrored.Filled.List, R.string.nav_label_records),
    TopLevelItem(Destination.Trends, Icons.Default.DateRange, R.string.nav_label_trends),
)

@Composable
private fun AppBottomBar(
    navController: NavController,
    currentDestination: NavDestination?,
) {
    NavigationBar {
        TOP_LEVEL_ITEMS.forEach { item ->
            NavigationBarItem(
                selected = currentDestination.matches(item.destination),
                onClick = {
                    navController.navigate(item.destination) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(item.icon, contentDescription = stringResource(item.labelRes)) },
                label = { Text(stringResource(item.labelRes)) },
            )
        }
    }
}

/** 目前目的地是否為指定的 top-level 目的地。 */
private fun NavDestination?.matches(target: Destination): Boolean =
    this != null && hasRoute(target::class)

package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.os.Build
import android.Manifest
import com.example.ui.components.AppBottomNavigationBar
import com.example.ui.screens.*
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppNavHost(viewModel = viewModel)
            }
        }
    }
}

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Otp : Screen("otp")
    object Home : Screen("home")
    object Crm : Screen("crm")
    object Tasks : Screen("tasks")
    object Attendance : Screen("attendance")
    object Work : Screen("work")
    object Leads : Screen("leads")
    object Chat : Screen("chat")
    object Profile : Screen("profile")

    object Projects : Screen("projects")
    object ProjectDetail : Screen("project_detail/{projectId}") {
        fun createRoute(id: Long) = "project_detail/$id"
    }
    object LeadDetail : Screen("lead_detail/{leadId}") {
        fun createRoute(id: Long) = "lead_detail/$id"
    }
    object FollowUps : Screen("follow_ups")
    object CallTracker : Screen("call_tracker")
    object ChatRoom : Screen("chat_room/{channelId}/{channelTitle}") {
        fun createRoute(id: String, title: String) = "chat_room/$id/$title"
    }
    object Notifications : Screen("notifications")
    object Holidays : Screen("holidays")
    object Invoices : Screen("invoices")
    object Manager : Screen("manager")
    object Settings : Screen("settings")
    object HelpSupport : Screen("help_support")
}

private fun getScreenOrder(route: String?): Int {
    return when {
        route == Screen.Home.route -> 0
        route == Screen.Crm.route || route == Screen.Leads.route -> 1
        route == Screen.Tasks.route -> 2
        route == Screen.Attendance.route -> 3
        route == Screen.Profile.route -> 4
        route == Screen.Work.route || route == Screen.Projects.route -> 5
        route == Screen.Chat.route -> 6
        else -> 10
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabEnterTransition(): EnterTransition {
    val initialOrder = getScreenOrder(initialState.destination.route)
    val targetOrder = getScreenOrder(targetState.destination.route)
    val direction = if (targetOrder >= initialOrder) {
        AnimatedContentTransitionScope.SlideDirection.Start
    } else {
        AnimatedContentTransitionScope.SlideDirection.End
    }
    return slideIntoContainer(
        towards = direction,
        animationSpec = tween(280, easing = FastOutSlowInEasing)
    ) + fadeIn(animationSpec = tween(220))
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabExitTransition(): ExitTransition {
    val initialOrder = getScreenOrder(initialState.destination.route)
    val targetOrder = getScreenOrder(targetState.destination.route)
    val direction = if (targetOrder >= initialOrder) {
        AnimatedContentTransitionScope.SlideDirection.Start
    } else {
        AnimatedContentTransitionScope.SlideDirection.End
    }
    return slideOutOfContainer(
        towards = direction,
        animationSpec = tween(260, easing = FastOutSlowInEasing)
    ) + fadeOut(animationSpec = tween(200))
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.detailEnterTransition(): EnterTransition {
    return slideIntoContainer(
        AnimatedContentTransitionScope.SlideDirection.Start,
        animationSpec = tween(340, easing = FastOutSlowInEasing)
    ) + fadeIn(animationSpec = tween(300))
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.detailExitTransition(): ExitTransition {
    return slideOutOfContainer(
        AnimatedContentTransitionScope.SlideDirection.Start,
        animationSpec = tween(280, easing = FastOutSlowInEasing)
    ) + fadeOut(animationSpec = tween(260))
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.detailPopEnterTransition(): EnterTransition {
    return slideIntoContainer(
        AnimatedContentTransitionScope.SlideDirection.End,
        animationSpec = tween(320, easing = FastOutSlowInEasing)
    ) + fadeIn(animationSpec = tween(300))
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.detailPopExitTransition(): ExitTransition {
    return slideOutOfContainer(
        AnimatedContentTransitionScope.SlideDirection.End,
        animationSpec = tween(280, easing = FastOutSlowInEasing)
    ) + fadeOut(animationSpec = tween(260))
}

@Composable
fun MainAppNavHost(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = LocalContext.current

    // Request notification permission for Android 13+ (Tiramisu / API 33+)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(permission)
            }
        }
    }

    // Handle deep links when user taps a background notification alert
    val activity = context as? ComponentActivity
    LaunchedEffect(activity?.intent) {
        activity?.intent?.let { intent ->
            val destination = intent.getStringExtra("destination")
            if (destination == "chat") {
                val channelId = intent.getStringExtra("channelId") ?: "company_chat"
                val channelTitle = intent.getStringExtra("channelTitle") ?: "Team Chat"
                viewModel.selectChatChannel(channelId)
                navController.navigate(Screen.ChatRoom.createRoute(channelId, channelTitle))
            } else if (destination == "tasks") {
                navController.navigate(Screen.Tasks.route)
            }
        }
    }

    val bottomNavRoutes = listOf(
        Screen.Home.route,
        Screen.Crm.route,
        Screen.Leads.route,
        Screen.Tasks.route,
        Screen.Attendance.route,
        Screen.Profile.route,
        Screen.Work.route,
        Screen.Chat.route
    )

    val showBottomBar = currentRoute in bottomNavRoutes

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = SurfaceBg,
        bottomBar = {
            if (showBottomBar) {
                AppBottomNavigationBar(navController = navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Splash Screen
            composable(
                route = Screen.Splash.route,
                enterTransition = { fadeIn(tween(400)) },
                exitTransition = { fadeOut(tween(350)) + scaleOut(targetScale = 1.05f, animationSpec = tween(350)) }
            ) {
                SplashScreen(
                    onTimeout = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            // Login Screen
            composable(
                route = Screen.Login.route,
                enterTransition = {
                    fadeIn(tween(350)) + slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Up,
                        tween(350, easing = FastOutSlowInEasing)
                    )
                },
                exitTransition = {
                    fadeOut(tween(280)) + slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Down,
                        tween(280, easing = FastOutSlowInEasing)
                    )
                }
            ) {
                LoginScreen(
                    onLoginSuccess = { isAdmin ->
                        val targetRoute = if (isAdmin) Screen.Manager.route else Screen.Home.route
                        navController.navigate(targetRoute) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToOtp = {
                        navController.navigate(Screen.Otp.route)
                    }
                )
            }

            // OTP Screen
            composable(
                route = Screen.Otp.route,
                enterTransition = { detailEnterTransition() },
                exitTransition = { detailExitTransition() },
                popEnterTransition = { detailPopEnterTransition() },
                popExitTransition = { detailPopExitTransition() }
            ) {
                OtpVerificationScreen(
                    onVerifySuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // 1. Employee Dashboard (Home)
            composable(
                route = Screen.Home.route,
                enterTransition = { tabEnterTransition() },
                exitTransition = { tabExitTransition() },
                popEnterTransition = { tabEnterTransition() },
                popExitTransition = { tabExitTransition() }
            ) {
                EmployeeDashboardScreen(
                    viewModel = viewModel,
                    onNavigateToAttendance = { navController.navigate(Screen.Attendance.route) },
                    onNavigateToProjects = { navController.navigate(Screen.Projects.route) },
                    onNavigateToTasks = { navController.navigate(Screen.Tasks.route) },
                    onNavigateToLeads = { navController.navigate(Screen.Crm.route) },
                    onNavigateToCalls = { navController.navigate(Screen.CallTracker.route) },
                    onNavigateToHolidays = { navController.navigate(Screen.Holidays.route) },
                    onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                    onNavigateToManager = { navController.navigate(Screen.Manager.route) },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                    onNavigateToInvoices = { navController.navigate(Screen.Invoices.route) },
                    onNavigateToChat = { navController.navigate(Screen.Chat.route) }
                )
            }

            // 2. CRM Views (Screen.Crm & Screen.Leads)
            composable(
                route = Screen.Crm.route,
                enterTransition = { tabEnterTransition() },
                exitTransition = { tabExitTransition() },
                popEnterTransition = { tabEnterTransition() },
                popExitTransition = { tabExitTransition() }
            ) {
                LeadsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onLeadClick = { leadId ->
                        navController.navigate(Screen.LeadDetail.createRoute(leadId))
                    },
                    onNavigateToTasks = {
                        navController.navigate(Screen.Tasks.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToAttendance = {
                        navController.navigate(Screen.Attendance.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
                )
            }

            composable(
                route = Screen.Leads.route,
                enterTransition = { tabEnterTransition() },
                exitTransition = { tabExitTransition() },
                popEnterTransition = { tabEnterTransition() },
                popExitTransition = { tabExitTransition() }
            ) {
                LeadsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onLeadClick = { leadId ->
                        navController.navigate(Screen.LeadDetail.createRoute(leadId))
                    },
                    onNavigateToTasks = {
                        navController.navigate(Screen.Tasks.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToAttendance = {
                        navController.navigate(Screen.Attendance.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
                )
            }

            // 3. Tasks View
            composable(
                route = Screen.Tasks.route,
                enterTransition = { tabEnterTransition() },
                exitTransition = { tabExitTransition() },
                popEnterTransition = { tabEnterTransition() },
                popExitTransition = { tabExitTransition() }
            ) {
                TasksScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToCrm = {
                        navController.navigate(Screen.Crm.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToAttendance = {
                        navController.navigate(Screen.Attendance.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
                )
            }

            // 4. Attendance View
            composable(
                route = Screen.Attendance.route,
                enterTransition = { tabEnterTransition() },
                exitTransition = { tabExitTransition() },
                popEnterTransition = { tabEnterTransition() },
                popExitTransition = { tabExitTransition() }
            ) {
                AttendanceScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToCrm = {
                        navController.navigate(Screen.Crm.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToTasks = {
                        navController.navigate(Screen.Tasks.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                    onNavigateToChat = { navController.navigate(Screen.Chat.route) }
                )
            }

            // 5. Work (Projects Hub)
            composable(
                route = Screen.Work.route,
                enterTransition = { tabEnterTransition() },
                exitTransition = { tabExitTransition() },
                popEnterTransition = { tabEnterTransition() },
                popExitTransition = { tabExitTransition() }
            ) {
                ProjectsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onProjectClick = { projectId ->
                        navController.navigate(Screen.ProjectDetail.createRoute(projectId))
                    }
                )
            }

            // 6. Team Chat Hub
            composable(
                route = Screen.Chat.route,
                enterTransition = { tabEnterTransition() },
                exitTransition = { tabExitTransition() },
                popEnterTransition = { tabEnterTransition() },
                popExitTransition = { tabExitTransition() }
            ) {
                TeamChatListScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onOpenChannel = { id, title ->
                        navController.navigate(Screen.ChatRoom.createRoute(id, title))
                    },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
                )
            }

            // 7. Profile & Settings
            composable(
                route = Screen.Profile.route,
                enterTransition = { tabEnterTransition() },
                exitTransition = { tabExitTransition() },
                popEnterTransition = { tabEnterTransition() },
                popExitTransition = { tabExitTransition() }
            ) {
                ProfileScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToHelp = { navController.navigate(Screen.HelpSupport.route) }
                )
            }

            composable(
                route = Screen.Settings.route,
                enterTransition = { detailEnterTransition() },
                exitTransition = { detailExitTransition() },
                popEnterTransition = { detailPopEnterTransition() },
                popExitTransition = { detailPopExitTransition() }
            ) {
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.HelpSupport.route,
                enterTransition = { detailEnterTransition() },
                exitTransition = { detailExitTransition() },
                popEnterTransition = { detailPopEnterTransition() },
                popExitTransition = { detailPopExitTransition() }
            ) {
                HelpSupportScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            // Sub-screens with detail slide & fade animations
            composable(
                route = Screen.Projects.route,
                enterTransition = { detailEnterTransition() },
                exitTransition = { detailExitTransition() },
                popEnterTransition = { detailPopEnterTransition() },
                popExitTransition = { detailPopExitTransition() }
            ) {
                ProjectsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onProjectClick = { projectId ->
                        navController.navigate(Screen.ProjectDetail.createRoute(projectId))
                    }
                )
            }

            composable(
                route = Screen.ProjectDetail.route,
                enterTransition = { detailEnterTransition() },
                exitTransition = { detailExitTransition() },
                popEnterTransition = { detailPopEnterTransition() },
                popExitTransition = { detailPopExitTransition() }
            ) { backStackEntry ->
                val idStr = backStackEntry.arguments?.getString("projectId") ?: "1"
                val id = idStr.toLongOrNull() ?: 1L
                ProjectDetailScreen(
                    projectId = id,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onViewTasks = { navController.navigate(Screen.Tasks.route) }
                )
            }

            composable(
                route = Screen.LeadDetail.route,
                enterTransition = { detailEnterTransition() },
                exitTransition = { detailExitTransition() },
                popEnterTransition = { detailPopEnterTransition() },
                popExitTransition = { detailPopExitTransition() }
            ) { backStackEntry ->
                val idStr = backStackEntry.arguments?.getString("leadId") ?: "1"
                val id = idStr.toLongOrNull() ?: 1L
                LeadDetailScreen(
                    leadId = id,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.FollowUps.route,
                enterTransition = { detailEnterTransition() },
                exitTransition = { detailExitTransition() },
                popEnterTransition = { detailPopEnterTransition() },
                popExitTransition = { detailPopExitTransition() }
            ) {
                FollowUpsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.CallTracker.route,
                enterTransition = { detailEnterTransition() },
                exitTransition = { detailExitTransition() },
                popEnterTransition = { detailPopEnterTransition() },
                popExitTransition = { detailPopExitTransition() }
            ) {
                CallTrackerScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.ChatRoom.route,
                enterTransition = { detailEnterTransition() },
                exitTransition = { detailExitTransition() },
                popEnterTransition = { detailPopEnterTransition() },
                popExitTransition = { detailPopExitTransition() }
            ) { backStackEntry ->
                val channelId = backStackEntry.arguments?.getString("channelId") ?: "dev_team"
                val channelTitle = backStackEntry.arguments?.getString("channelTitle") ?: "Development Team"
                ChatRoomScreen(
                    channelId = channelId,
                    channelTitle = channelTitle,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.Notifications.route,
                enterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Down,
                        animationSpec = tween(320, easing = FastOutSlowInEasing)
                    ) + fadeIn(tween(280))
                },
                exitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Up,
                        animationSpec = tween(280, easing = FastOutSlowInEasing)
                    ) + fadeOut(tween(260))
                },
                popEnterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Down,
                        animationSpec = tween(320, easing = FastOutSlowInEasing)
                    ) + fadeIn(tween(280))
                },
                popExitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Up,
                        animationSpec = tween(280, easing = FastOutSlowInEasing)
                    ) + fadeOut(tween(260))
                }
            ) {
                NotificationsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.Holidays.route,
                enterTransition = { detailEnterTransition() },
                exitTransition = { detailExitTransition() },
                popEnterTransition = { detailPopEnterTransition() },
                popExitTransition = { detailPopExitTransition() }
            ) {
                HolidaysLeaveScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.Invoices.route,
                enterTransition = { detailEnterTransition() },
                exitTransition = { detailExitTransition() },
                popEnterTransition = { detailPopEnterTransition() },
                popExitTransition = { detailPopExitTransition() }
            ) {
                InvoiceQuotationScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToChat = { navController.navigate(Screen.Chat.route) }
                )
            }

            composable(
                route = Screen.Manager.route,
                enterTransition = { detailEnterTransition() },
                exitTransition = { detailExitTransition() },
                popEnterTransition = { detailPopEnterTransition() },
                popExitTransition = { detailPopExitTransition() }
            ) {
                ManagerDashboardScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToProjects = { navController.navigate(Screen.Projects.route) },
                    onNavigateToLeads = { navController.navigate(Screen.Crm.route) },
                    onNavigateToChat = { navController.navigate(Screen.Chat.route) }
                )
            }
        }
    }
}

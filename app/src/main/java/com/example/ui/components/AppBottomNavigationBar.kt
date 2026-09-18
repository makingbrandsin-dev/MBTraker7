package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.R
import com.example.Screen

/**
 * Navigation item specification for the persistent bottom bar.
 */
data class BottomNavItem(
    val route: String,
    val title: String,
    val drawableRes: Int,
    val activeColor: Color,
    val activeBgColor: Color,
    val testTag: String,
    val isRouteMatching: (String?) -> Boolean = { it == route }
)

/**
 * Persistent Bottom Navigation Bar component with colorful, modern UI icons
 * and animated pill containers for Home, CRM, Tasks, Attendance, and Profile.
 */
@Composable
fun AppBottomNavigationBar(
    currentRoute: String?,
    onNavigateToRoute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = remember {
        listOf(
            BottomNavItem(
                route = Screen.Home.route,
                title = "Home",
                drawableRes = R.drawable.ic_nav_home,
                activeColor = Color(0xFF2563EB), // Electric Royal Blue
                activeBgColor = Color(0xFFEFF6FF),
                testTag = "bottom_nav_home",
                isRouteMatching = { it == Screen.Home.route }
            ),
            BottomNavItem(
                route = Screen.Crm.route,
                title = "CRM",
                drawableRes = R.drawable.ic_nav_crm,
                activeColor = Color(0xFF00838F), // Cyan / Teal matching CRM badge
                activeBgColor = Color(0xFFE0F7FA),
                testTag = "bottom_nav_crm",
                isRouteMatching = { it == Screen.Crm.route || it == Screen.Leads.route }
            ),
            BottomNavItem(
                route = Screen.Tasks.route,
                title = "Tasks",
                drawableRes = R.drawable.ic_nav_tasks,
                activeColor = Color(0xFF059669), // Vibrant Emerald matching check.png
                activeBgColor = Color(0xFFDCFCE7),
                testTag = "bottom_nav_tasks",
                isRouteMatching = { it == Screen.Tasks.route }
            ),
            BottomNavItem(
                route = Screen.Attendance.route,
                title = "Attendance",
                drawableRes = R.drawable.ic_nav_attendance,
                activeColor = Color(0xFF4F46E5), // Indigo matching biometric screen
                activeBgColor = Color(0xFFEEF2FF),
                testTag = "bottom_nav_attendance",
                isRouteMatching = { it == Screen.Attendance.route }
            ),
            BottomNavItem(
                route = Screen.Chat.route,
                title = "Team Chat",
                drawableRes = R.drawable.ic_nav_chat,
                activeColor = Color(0xFF7C3AED), // Vibrant Purple
                activeBgColor = Color(0xFFEDE9FE),
                testTag = "bottom_nav_chat",
                isRouteMatching = { it == Screen.Chat.route || it?.startsWith("chat_room/") == true }
            )
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 14.dp, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
        color = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = item.isRouteMatching(currentRoute)

                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected) item.activeBgColor else Color.Transparent,
                    animationSpec = tween(200),
                    label = "bgColor"
                )

                Box(
                    modifier = Modifier
                        .testTag(item.testTag)
                        .clip(RoundedCornerShape(16.dp))
                        .background(backgroundColor)
                        .then(
                            if (isSelected) {
                                Modifier.border(
                                    width = 1.dp,
                                    color = item.activeColor.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                            } else {
                                Modifier
                            }
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = item.activeColor.copy(alpha = 0.2f))
                        ) {
                            if (!isSelected) {
                                onNavigateToRoute(item.route)
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Custom Icon: Always shows normally at full opacity, not faded
                        Image(
                            painter = painterResource(id = item.drawableRes),
                            contentDescription = item.title,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = item.title,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) item.activeColor else Color(0xFF334155) // Normal clear color, not faded
                        )
                    }
                }
            }
        }
    }
}

/**
 * Overloaded convenience composable taking [NavHostController] directly
 * for persistent lifecycle and backstack navigation.
 */
@Composable
fun AppBottomNavigationBar(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    AppBottomNavigationBar(
        currentRoute = currentRoute,
        onNavigateToRoute = { route ->
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        },
        modifier = modifier
    )
}

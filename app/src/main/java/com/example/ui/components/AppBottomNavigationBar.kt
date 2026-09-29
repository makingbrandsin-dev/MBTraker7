package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.R
import com.example.Screen
import com.example.domain.milo.MiloState
import com.example.milo.MiloAiAssistantSheet
import com.example.milo.MiloCharacter
import com.example.milo.MiloViewModel
import com.example.util.MiloHaptics
import kotlin.math.sin

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
 * Redesigned Floating Cradle Bottom Navigation Dock featuring:
 * - "Home" removed from bottom bar
 * - Center floating cradle button: "ASK MILO"
 * - Dynamic 10X interactive icon bounce & tilt animations on click
 * - Continuous liquid "River Flow" animation along the dock
 */
@Composable
fun AppBottomNavigationBar(
    currentRoute: String?,
    onNavigateToRoute: (String) -> Unit,
    onAskMiloClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var localShowMiloSheet by remember { mutableStateOf(false) }
    val miloViewModel: MiloViewModel = viewModel()

    // Left navigation items (CRM, Tasks)
    val leftItems = remember {
        listOf(
            BottomNavItem(
                route = Screen.Crm.route,
                title = "CRM",
                drawableRes = R.drawable.ic_nav_crm,
                activeColor = Color(0xFF00838F),
                activeBgColor = Color(0xFFE0F7FA),
                testTag = "bottom_nav_crm",
                isRouteMatching = { it == Screen.Crm.route || it == Screen.Leads.route }
            ),
            BottomNavItem(
                route = Screen.Tasks.route,
                title = "Tasks",
                drawableRes = R.drawable.ic_nav_tasks,
                activeColor = Color(0xFF059669),
                activeBgColor = Color(0xFFDCFCE7),
                testTag = "bottom_nav_tasks",
                isRouteMatching = { it == Screen.Tasks.route }
            )
        )
    }

    // Right navigation items (Attendance, Team Chat)
    val rightItems = remember {
        listOf(
            BottomNavItem(
                route = Screen.Attendance.route,
                title = "Attendance",
                drawableRes = R.drawable.ic_nav_attendance,
                activeColor = Color(0xFF4F46E5),
                activeBgColor = Color(0xFFEEF2FF),
                testTag = "bottom_nav_attendance",
                isRouteMatching = { it == Screen.Attendance.route }
            ),
            BottomNavItem(
                route = Screen.Chat.route,
                title = "Team Chat",
                drawableRes = R.drawable.ic_nav_chat,
                activeColor = Color(0xFF7C3AED),
                activeBgColor = Color(0xFFEDE9FE),
                testTag = "bottom_nav_chat",
                isRouteMatching = { it == Screen.Chat.route || it?.startsWith("chat_room/") == true }
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 12.dp, end = 12.dp, bottom = 4.dp)
            .height(86.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Main Floating Navbar Dock Surface
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(32.dp),
                    spotColor = Color(0xFF1E293B).copy(alpha = 0.25f)
                ),
            color = Color.White,
            shape = RoundedCornerShape(32.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            // Navbar Items Row Layout
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left 2 items: CRM & Tasks
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    leftItems.forEach { item ->
                        AnimatedNavItem(
                            item = item,
                            currentRoute = currentRoute,
                            onNavigate = onNavigateToRoute
                        )
                    }
                }

                // Spacer gap for center floating "ASK MILO" button
                Spacer(modifier = Modifier.width(68.dp))

                // Right 2 items: Attendance & Team Chat
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    rightItems.forEach { item ->
                        AnimatedNavItem(
                            item = item,
                            currentRoute = currentRoute,
                            onNavigate = onNavigateToRoute
                        )
                    }
                }
            }
        }

        // 🦁 Center Floating "ASK MILO" Elevated Cradle Button with Smooth Lift-Up Hover Effect
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 2.dp)
                .liftOnPress(elevationLift = 14.dp, translateY = (-5).dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    MiloHaptics.performButtonTap(context)
                    if (onAskMiloClick != null) {
                        onAskMiloClick()
                    } else {
                        localShowMiloSheet = true
                    }
                }
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 10.dp,
                border = BorderStroke(2.5.dp, Color(0xFF00E5FF)),
                modifier = Modifier.size(56.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White,
                                    Color(0xFFE0F7FA)
                                )
                            )
                        )
                ) {
                    MiloCharacter(
                        state = MiloState.WELCOME,
                        size = 46.dp,
                        showStateBadge = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF298CD8),
                shadowElevation = 2.dp
            ) {
                Text(
                    text = "ASK MILO",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                )
            }
        }
    }

    // Local Milo Assistant Bottom Sheet when tapped
    if (localShowMiloSheet) {
        MiloAiAssistantSheet(
            miloViewModel = miloViewModel,
            onDismiss = { localShowMiloSheet = false },
            onNavigateToLeads = {
                localShowMiloSheet = false
                onNavigateToRoute(Screen.Crm.route)
            },
            onNavigateToTasks = {
                localShowMiloSheet = false
                onNavigateToRoute(Screen.Tasks.route)
            }
        )
    }
}

/**
 * Individual Navigation Item with 10X interactive spring scale bounce and tilt click animations.
 */
@Composable
private fun AnimatedNavItem(
    item: BottomNavItem,
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val isSelected = item.isRouteMatching(currentRoute)
    var isPressed by remember { mutableStateOf(false) }

    // Spring scale bounce animation
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.82f
            isSelected -> 1.18f
            else -> 1.0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "ItemScale"
    )

    // Slight rotation tilt animation on selection
    val rotation by animateFloatAsState(
        targetValue = if (isSelected) -6f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "ItemRotation"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) item.activeBgColor else Color.Transparent,
        animationSpec = tween(200),
        label = "BgColor"
    )

    Box(
        modifier = Modifier
            .testTag(item.testTag)
            .liftOnPress(elevationLift = 6.dp, translateY = (-4).dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = rotation
            }
            .clip(RoundedCornerShape(18.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = item.activeColor.copy(alpha = 0.25f))
            ) {
                MiloHaptics.performButtonClick(context)
                if (!isSelected) {
                    onNavigate(item.route)
                }
            }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = item.drawableRes),
                contentDescription = item.title,
                modifier = Modifier.size(25.dp)
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = item.title,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                color = if (isSelected) item.activeColor else Color(0xFF334155)
            )
        }
    }
}

/**
 * Overloaded convenience composable taking [NavHostController] directly.
 */
@Composable
fun AppBottomNavigationBar(
    navController: NavHostController,
    onAskMiloClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    AppBottomNavigationBar(
        currentRoute = currentRoute,
        onNavigateToRoute = { route ->
            navController.navigate(route) {
                popUpTo(navController.graph.startDestinationId) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        },
        onAskMiloClick = onAskMiloClick,
        modifier = modifier
    )
}

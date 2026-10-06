package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AttendanceRecord
import com.example.data.model.Department
import com.example.data.model.EmployeeEntity
import com.example.ui.screens.MainViewModel
import com.example.ui.theme.*
import com.example.util.MiloHaptics
import java.text.SimpleDateFormat
import java.util.*

/**
 * 📊 D3-Inspired Employee Attendance Weekly Heatmap
 * Visualizes employees' attendance patterns throughout the week on a GitHub-style activity grid.
 * Features a high-contrast multi-shade green color gradient intensity scale, interactive filters,
 * detailed M3 click tooltips, legend scales, and weekly analytic summaries.
 */
@Composable
fun AttendanceHeatmapWidget(
    viewModel: MainViewModel,
    onNavigateToAttendance: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val employees by viewModel.employees.collectAsState()
    val attendanceList by viewModel.allAttendance.collectAsState(initial = emptyList())

    var selectedDepartment by remember { mutableStateOf<Department?>(null) }
    var selectedWeekOffset by remember { mutableIntStateOf(0) } // 0 = This Week, -1 = Last Week, -2 = 2 Weeks Ago

    // Active hovered or clicked cell coordinates (employeeId, dayOfWeekIndex 0..6)
    var activeCellEmployeeId by remember { mutableStateOf<Long?>(null) }
    var activeCellDayIndex by remember { mutableIntStateOf(-1) }

    // Filter employees based on selected department
    val filteredEmployees = remember(employees, selectedDepartment) {
        if (selectedDepartment == null) employees
        else employees.filter { it.department == selectedDepartment }
    }

    // Get date range for the selected week offset
    val selectedWeekDateRange = remember(selectedWeekOffset) {
        val calendar = Calendar.getInstance()
        // Adjust to start of the week (Monday)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysToSub = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
        calendar.add(Calendar.DAY_OF_YEAR, -daysToSub)
        calendar.add(Calendar.WEEK_OF_YEAR, selectedWeekOffset)
        
        // Start of week (Monday)
        val startCalendar = calendar.clone() as Calendar
        startCalendar.set(Calendar.HOUR_OF_DAY, 0)
        startCalendar.set(Calendar.MINUTE, 0)
        startCalendar.set(Calendar.SECOND, 0)
        
        // End of week (Sunday)
        val endCalendar = calendar.clone() as Calendar
        endCalendar.add(Calendar.DAY_OF_YEAR, 6)
        endCalendar.set(Calendar.HOUR_OF_DAY, 23)
        endCalendar.set(Calendar.MINUTE, 59)
        endCalendar.set(Calendar.SECOND, 59)

        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displayFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
        
        Triple(
            format.format(startCalendar.time),
            format.format(endCalendar.time),
            "${displayFormat.format(startCalendar.time)} - ${displayFormat.format(endCalendar.time)}"
        )
    }

    // Map Monday to Sunday dates of the selected week for matching cell records
    val weekDates = remember(selectedWeekOffset) {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysToSub = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
        calendar.add(Calendar.DAY_OF_YEAR, -daysToSub)
        calendar.add(Calendar.WEEK_OF_YEAR, selectedWeekOffset)

        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        List(7) { i ->
            val dayCal = calendar.clone() as Calendar
            dayCal.add(Calendar.DAY_OF_YEAR, i)
            format.format(dayCal.time)
        }
    }

    // Filter attendance records to those belonging to the selected week and matching employees
    val matchedRecords = remember(attendanceList, weekDates, filteredEmployees) {
        val weekDatesSet = weekDates.toSet()
        val empNamesSet = filteredEmployees.map { it.name }.toSet()
        attendanceList.filter { record ->
            record.date in weekDatesSet && record.employeeName in empNamesSet
        }
    }

    // Build grid structure: Map Employee ID to an array of 7 elements containing AttendanceRecord?
    val heatmapGrid = remember(filteredEmployees, weekDates, matchedRecords) {
        filteredEmployees.associate { emp ->
            emp.id to List(7) { dayIdx ->
                val targetDate = weekDates[dayIdx]
                // Find most complete record for this employee and date
                matchedRecords.find { it.employeeName == emp.name && it.date == targetDate }
            }
        }
    }

    // 📈 Analytics Summary Calculations
    val analyticsSummary = remember(heatmapGrid, filteredEmployees) {
        if (filteredEmployees.isEmpty()) null
        else {
            var totalHours = 0.0
            var presentCellCount = 0
            val dayCounters = IntArray(7) { 0 }
            val empHoursMap = mutableMapOf<String, Double>()

            heatmapGrid.forEach { (empId, recordsList) ->
                val empName = filteredEmployees.find { it.id == empId }?.name ?: ""
                recordsList.forEachIndexed { dayIdx, record ->
                    if (record != null) {
                        presentCellCount++
                        dayCounters[dayIdx]++
                        val hours = record.durationMinutes / 60.0
                        totalHours += hours
                        empHoursMap[empName] = (empHoursMap[empName] ?: 0.0) + hours
                    }
                }
            }

            val daysOfWeekNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
            val bestDayIdx = dayCounters.indices.maxByOrNull { dayCounters[it] } ?: 0
            val bestDayName = daysOfWeekNames[bestDayIdx]

            val bestEmployee = empHoursMap.maxByOrNull { it.value }

            val totalPossibleCells = filteredEmployees.size * 7
            val attendancePercentage = if (totalPossibleCells > 0) (presentCellCount.toFloat() / totalPossibleCells * 100).toInt() else 0

            AttendanceHeatmapStats(
                attendanceRate = attendancePercentage,
                bestAttendanceDay = bestDayName,
                bestDayCount = dayCounters[bestDayIdx],
                mostProductiveEmployee = bestEmployee?.key ?: "N/A",
                mostProductiveHours = bestEmployee?.value ?: 0.0,
                averageDailyHours = if (presentCellCount > 0) totalHours / presentCellCount else 0.0
            )
        }
    }

    // Identify details of currently active cell
    val activeCellDetails = remember(activeCellEmployeeId, activeCellDayIndex, heatmapGrid, filteredEmployees, weekDates) {
        val empId = activeCellEmployeeId
        val dayIdx = activeCellDayIndex
        if (empId != null && dayIdx in 0..6) {
            val emp = filteredEmployees.find { it.id == empId }
            val record = heatmapGrid[empId]?.get(dayIdx)
            val dateStr = weekDates[dayIdx]
            
            val displayDateFormat = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault())
            val dateFormatted = try {
                val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
                displayDateFormat.format(parsed!!)
            } catch (e: Exception) {
                dateStr
            }

            ActiveCellInfo(
                employeeName = emp?.name ?: "Unknown Employee",
                designation = emp?.designation ?: "Team Member",
                department = emp?.department?.name ?: "Engineering",
                date = dateFormatted,
                hasRecord = record != null,
                record = record
            )
        } else {
            null
        }
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("attendance_heatmap_widget")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 🏷️ Section Header: Intensity Heatmap Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF0284C7), Color(0xFF0369A1))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridOn,
                            contentDescription = "Weekly Heatmap",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Weekly Attendance Heatmap",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = Color(0xFF0F172A)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFE0F2FE),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = "D3 ENGINE ACTIVE",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0369A1),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = selectedWeekDateRange.third,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // 🗓️ Week Offset Controller (Next / Prev Week)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = {
                            MiloHaptics.performButtonTap(context)
                            selectedWeekOffset--
                            activeCellEmployeeId = null // Clear selected cell
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            Icons.Default.ChevronLeft,
                            contentDescription = "Previous Week",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    Text(
                        text = when (selectedWeekOffset) {
                            0 -> "This Week"
                            -1 -> "Last Week"
                            else -> "${-selectedWeekOffset}w Ago"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        modifier = Modifier.widthIn(min = 55.dp),
                        textAlign = TextAlign.Center
                    )

                    IconButton(
                        onClick = {
                            if (selectedWeekOffset < 0) {
                                MiloHaptics.performButtonTap(context)
                                selectedWeekOffset++
                                activeCellEmployeeId = null // Clear selected cell
                            }
                        },
                        enabled = selectedWeekOffset < 0,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "Next Week",
                            tint = if (selectedWeekOffset < 0) Color(0xFF64748B) else Color(0xFFCBD5E1),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 🏷️ Horizontal Filter for Departments
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedDepartment == null,
                        onClick = {
                            MiloHaptics.performButtonTap(context)
                            selectedDepartment = null
                            activeCellEmployeeId = null
                        },
                        label = { Text("All", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0F172A),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFFF1F5F9),
                            labelColor = Color(0xFF475569)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(28.dp)
                    )
                }
                items(Department.values()) { dept ->
                    FilterChip(
                        selected = selectedDepartment == dept,
                        onClick = {
                            MiloHaptics.performButtonTap(context)
                            selectedDepartment = dept
                            activeCellEmployeeId = null
                        },
                        label = { Text(dept.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0F172A),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFFF1F5F9),
                            labelColor = Color(0xFF475569)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (filteredEmployees.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No employees registered in this department", color = Color(0xFF94A3B8), fontSize = 12.sp)
                }
            } else {
                // 📊 THE D3 INTENSITY GRID
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    // Header row: Column headers (Days of week: M T W T F S S)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Empty spacer matching the name column width
                        Box(modifier = Modifier.width(85.dp))
                        
                        val daysLabels = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            daysLabels.forEach { label ->
                                Text(
                                    text = label,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF64748B),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Grid body rows (one per employee)
                    filteredEmployees.forEach { emp ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Employee Name Column
                            Text(
                                text = emp.name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.width(85.dp)
                            )

                            // 7 squares for 7 days
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                val empRecords = heatmapGrid[emp.id] ?: List(7) { null }
                                for (dayIdx in 0..6) {
                                    val record = empRecords[dayIdx]
                                    val durationHours = if (record != null) record.durationMinutes / 60.0 else 0.0

                                    // Intensity scale colors (D3 style GitHub green shades)
                                    val cellColor = when {
                                        record == null -> Color(0xFFE2E8F0) // Slate Gray (Absent/Off)
                                        durationHours < 4.0 -> Color(0xFFD1FAE5) // Light mint green (Short day / half-day)
                                        durationHours < 7.0 -> Color(0xFF6EE7B7) // Sage green (Standard low)
                                        durationHours < 9.0 -> Color(0xFF10B981) // Vibrant emerald green (Full Standard shift)
                                        else -> Color(0xFF047857) // Deep teal green (Overtime / high output)
                                    }

                                    val isSelectedCell = activeCellEmployeeId == emp.id && activeCellDayIndex == dayIdx

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(2.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(cellColor)
                                            .border(
                                                width = if (isSelectedCell) 2.dp else 0.dp,
                                                color = if (isSelectedCell) Color(0xFF0F172A) else Color.Transparent,
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .clickable {
                                                MiloHaptics.performButtonTap(context)
                                                if (isSelectedCell) {
                                                    activeCellEmployeeId = null
                                                    activeCellDayIndex = -1
                                                } else {
                                                    activeCellEmployeeId = emp.id
                                                    activeCellDayIndex = dayIdx
                                                }
                                            }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 🏷️ HEATMAP COLOR LEGEND
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Less", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        val legendShades = listOf(
                            Color(0xFFE2E8F0) to "0h",
                            Color(0xFFD1FAE5) to "<4h",
                            Color(0xFF6EE7B7) to "4-7h",
                            Color(0xFF10B981) to "7-9h",
                            Color(0xFF047857) to "9h+"
                        )

                        legendShades.forEach { (color, rangeLabel) ->
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(color)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text("More", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    }
                }
            }

            // 💬 DYNAMIC INTERACTIVE DETAILS TOOLTIP DISPLAY CARD
            AnimatedVisibility(
                visible = activeCellDetails != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                activeCellDetails?.let { details ->
                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBAE6FD)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = details.employeeName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF0369A1)
                                    )
                                    Text(
                                        text = "${details.designation} • ${details.department}",
                                        fontSize = 10.sp,
                                        color = Color(0xFF0284C7)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        activeCellEmployeeId = null
                                        activeCellDayIndex = -1
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Close Tooltip",
                                        tint = Color(0xFF0284C7),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Divider(color = Color(0xFFE0F2FE), modifier = Modifier.padding(vertical = 8.dp))

                            Text(
                                text = details.date,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                color = Color(0xFF1E293B)
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            if (details.hasRecord && details.record != null) {
                                val rec = details.record
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Check In", fontSize = 9.sp, color = Color(0xFF64748B))
                                        Text(rec.checkInTime, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                    }
                                    Column {
                                        Text("Check Out", fontSize = 9.sp, color = Color(0xFF64748B))
                                        Text(rec.checkOutTime ?: "--:--", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                    }
                                    Column {
                                        Text("Working Hours", fontSize = 9.sp, color = Color(0xFF64748B))
                                        val hrs = rec.durationMinutes / 60
                                        val mins = rec.durationMinutes % 60
                                        Text("${hrs}h ${mins}m", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                    }
                                    Column {
                                        Text("Status", fontSize = 9.sp, color = Color(0xFF64748B))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = when (rec.status) {
                                                "Present" -> Color(0xFFDCFCE7)
                                                "Late" -> Color(0xFFFEF3C7)
                                                else -> Color(0xFFF1F5F9)
                                            }
                                        ) {
                                            Text(
                                                text = rec.status,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = when (rec.status) {
                                                    "Present" -> Color(0xFF16A34A)
                                                    "Late" -> Color(0xFFD97706)
                                                    else -> Color(0xFF475569)
                                                },
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Absent or no punch logs found for this day.",
                                        fontSize = 11.sp,
                                        color = Color(0xFFEF4444),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 📈 KEY ANALYTICS SUMMARY GRID
            analyticsSummary?.let { stats ->
                Divider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(bottom = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // KPI Card 1: Attendance Rate
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Week Coverage", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${stats.attendanceRate}%",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Total possible logged", fontSize = 8.sp, color = Color(0xFF94A3B8))
                        }
                    }

                    // KPI Card 2: Peak Attendance Day
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Peak Day", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stats.bestAttendanceDay.substring(0, 3).uppercase(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF10B981)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("${stats.bestDayCount} active punch-ins", fontSize = 8.sp, color = Color(0xFF94A3B8))
                        }
                    }

                    // KPI Card 3: Top Performer
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Top Performer", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stats.mostProductiveEmployee.split(" ").firstOrNull() ?: "N/A",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0284C7),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(String.format("%.1f hrs logged", stats.mostProductiveHours), fontSize = 8.sp, color = Color(0xFF94A3B8))
                        }
                    }
                }
            }
        }
    }
}

// Stats models for Heatmap data aggregation
data class AttendanceHeatmapStats(
    val attendanceRate: Int,
    val bestAttendanceDay: String,
    val bestDayCount: Int,
    val mostProductiveEmployee: String,
    val mostProductiveHours: Double,
    val averageDailyHours: Double
)

data class ActiveCellInfo(
    val employeeName: String,
    val designation: String,
    val department: String,
    val date: String,
    val hasRecord: Boolean,
    val record: AttendanceRecord?
)

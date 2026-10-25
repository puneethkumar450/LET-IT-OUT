package com.letitout.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class AgeStyle(val value: String, val title: String, val detail: String)

private val ageStyles = listOf(
    AgeStyle("young", "Young adult", "18–24 style"),
    AgeStyle("adult", "Adult", "Easygoing"),
    AgeStyle("mature", "Mature", "Grounded")
)

/**
 * A visual choice for adults. No emotion or age is inferred from the user's messages or profile.
 * The parent owns the saved choice and applies it after [onSelect].
 */
@Composable
internal fun CompanionPickerScreen(
    selectedGender: String,
    selectedAge: String,
    onSelect: (String, String) -> Unit,
    onBack: () -> Unit
) {
    var gender by rememberSaveable(selectedGender) {
        mutableStateOf(selectedGender.takeIf { it == "woman" || it == "man" } ?: "woman")
    }
    var age by rememberSaveable(selectedAge) {
        mutableStateOf(selectedAge.takeIf { it in ageStyles.map(AgeStyle::value) } ?: "adult")
    }
    BackHandler(onBack = onBack)

    Column(Modifier.fillMaxSize().background(AppColors.Background).systemBarsPadding()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 16.dp)) {
            PageHeader("Your companion", onBack)
            Spacer(Modifier.height(24.dp))
            Text(
                "Choose who feels right to talk to.",
                style = DisplayStyle,
                fontSize = 30.sp,
                lineHeight = 35.sp,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Find a look that feels comfortable. You can change it anytime.",
                color = AppColors.Muted,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
            Spacer(Modifier.height(24.dp))

            val backdrop = companionBackdrop(gender, age)
            Box(
                Modifier.fillMaxWidth().height(264.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(backdrop),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.size(216.dp).clip(CircleShape)
                        .background(backdrop.copy(alpha = .8f)),
                    contentAlignment = Alignment.Center
                ) {
                    CompanionPortrait(
                        gender = gender,
                        age = age,
                        expression = "calm",
                        modifier = Modifier.fillMaxSize(),
                        contentDescription = "${ageLabel(age)} ${gender} companion portrait"
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "${ageLabel(age)} ${gender} · chosen by you",
                modifier = Modifier.fillMaxWidth(),
                color = AppColors.Muted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(26.dp))

            Text("Choose a look", color = AppColors.Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GenderChoice("Woman", gender == "woman", Modifier.weight(1f)) { gender = "woman" }
                GenderChoice("Man", gender == "man", Modifier.weight(1f)) { gender = "man" }
            }
            Spacer(Modifier.height(24.dp))
            Text("Age style", color = AppColors.Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ageStyles.forEach { style ->
                    AgeChoice(style, age == style.value, Modifier.weight(1f)) { age = style.value }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Portrait styles are for adults 18+. Your choice controls the look of the companion.",
                color = AppColors.Muted,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }
        Box(Modifier.fillMaxWidth().background(AppColors.Background).padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 16.dp)) {
            PillButton("Use this companion", { onSelect(gender, age) })
        }
    }
}

@Composable
private fun GenderChoice(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Surface(
        modifier = modifier.heightIn(min = 58.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        shape = shape,
        color = if (selected) AppColors.Lavender else AppColors.Surface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) AppColors.Primary else AppColors.Divider)
    ) {
        Box(Modifier.padding(horizontal = 12.dp, vertical = 14.dp), contentAlignment = Alignment.Center) {
            Text(label, color = AppColors.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AgeChoice(style: AgeStyle, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Surface(
        modifier = modifier.heightIn(min = 77.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        shape = shape,
        color = if (selected) AppColors.Lavender else AppColors.Surface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) AppColors.Primary else AppColors.Divider)
    ) {
        Column(
            Modifier.padding(horizontal = 5.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(style.title, color = AppColors.Ink, fontSize = 12.sp, lineHeight = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(3.dp))
            Text(style.detail, color = AppColors.Muted, fontSize = 11.sp, lineHeight = 14.sp, textAlign = TextAlign.Center)
        }
    }
}

private fun ageLabel(age: String): String = when (age) {
    "young" -> "Young adult"
    "mature" -> "Mature"
    else -> "Adult"
}

private fun companionBackdrop(gender: String, age: String): Color = when ("${gender}-${age}") {
    "woman-young" -> Color(0xFFE6C7B3)
    "woman-adult" -> Color(0xFFEDAA96)
    "woman-mature" -> Color(0xFFC9AB92)
    "man-young" -> Color(0xFFEAD0BF)
    "man-adult" -> Color(0xFFBBC4C2)
    "man-mature" -> Color(0xFF164638)
    else -> AppColors.Peach
}

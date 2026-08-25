package id.rona.app.ui.log

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Healing
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.rona.app.domain.model.Energy
import id.rona.app.domain.model.FlowLevel
import id.rona.app.domain.model.Mood
import id.rona.app.domain.model.Severity
import id.rona.app.domain.model.SymptomType
import id.rona.app.domain.model.ThemeMode
import id.rona.app.ui.components.RonaJournalTextField
import id.rona.app.ui.components.RonaPrimaryButton
import id.rona.app.ui.components.RonaSecondaryButton
import id.rona.app.ui.components.RonaSection
import id.rona.app.ui.components.RonaSelectableChip
import id.rona.app.ui.components.RonaTextAction
import id.rona.app.ui.nlp.NoteAnalysisResultSheet
import id.rona.app.ui.theme.LocalRonaColors
import id.rona.app.ui.theme.RonaMotion
import id.rona.app.ui.theme.RonaPillShape
import id.rona.app.ui.theme.RonaTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Guided Adaptive Question-Card Sheet for daily check-in.
 * Guides the user step-by-step through:
 * Initial Choice -> Flow -> Symptoms -> Severity (Conditional) -> Energy & Mood -> Note -> Summary.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuidedCheckInSheet(
    date: LocalDate? = null,
    onDismiss: () -> Unit,
    onOpenFullEditor: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LogEditorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalRonaColors.current

    // Always (re)load on first composition so reopening after a prior save starts
    // clean instead of resuming stale answers or skipping the Initial card.
    LaunchedEffect(Unit) {
        viewModel.load(date ?: uiState.date ?: java.time.LocalDate.now())
    }

    // A successful save is transient: acknowledge, reset the session, and close.
    LaunchedEffect(uiState.saveState) {
        if (uiState.saveState == SaveState.Saved) {
            onDismiss()
            viewModel.acknowledgeSaved()
        }
    }

    val effectiveDate = date ?: uiState.date
    val dateFormatted = effectiveDate.format(
        DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("id", "ID"))
    )

    val currentStep = uiState.currentStep
    val progress = when (currentStep) {
        DailyCheckInStep.Initial -> 0.15f
        DailyCheckInStep.PeriodStartConfirm -> 0.22f
        DailyCheckInStep.QuickEnergy -> 0.22f
        DailyCheckInStep.Flow -> 0.35f
        DailyCheckInStep.Symptoms -> 0.5f
        DailyCheckInStep.Severity -> 0.6f
        DailyCheckInStep.Energy -> 0.75f
        DailyCheckInStep.Note -> 0.9f
        DailyCheckInStep.Summary -> 1.0f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = RonaMotion.appleSpring(),
        label = "checkInProgress",
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.pageCanvas),
    ) {
        // ————— Sheet Header —————
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (currentStep != DailyCheckInStep.Initial) {
                    IconButton(
                        onClick = viewModel::previousStep,
                        modifier = Modifier.size(38.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Kembali ke langkah sebelumnya",
                            tint = colors.inkPrimary,
                        )
                    }
                } else {
                    Spacer(Modifier.size(38.dp))
                }

                Text(
                    text = dateFormatted,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.inkSecondary,
                    ),
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(38.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Tutup",
                        tint = colors.inkSecondary,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Animated Progress Indicator
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape),
                color = colors.cyclePrimary,
                trackColor = colors.surfaceSoft,
            )
        }

        // ————— Card Body (Animated Transition) —————
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.cyclePrimary)
                }
            } else {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        val isForward = targetState.stepOrdinal() >= initialState.stepOrdinal()
                        if (isForward) {
                            (slideInHorizontally(
                                initialOffsetX = { (it * 0.22f).roundToInt() },
                                animationSpec = RonaMotion.appleSpring(),
                            ) + fadeIn(
                                animationSpec = tween(260, easing = RonaMotion.AppleEaseOut),
                            )).togetherWith(
                                slideOutHorizontally(
                                    targetOffsetX = { (-it * 0.22f).roundToInt() },
                                    animationSpec = RonaMotion.appleSpring(),
                                ) + fadeOut(
                                    animationSpec = tween(180, easing = RonaMotion.AppleEaseIn),
                                )
                            )
                        } else {
                            (slideInHorizontally(
                                initialOffsetX = { (-it * 0.22f).roundToInt() },
                                animationSpec = RonaMotion.appleSpring(),
                            ) + fadeIn(
                                animationSpec = tween(260, easing = RonaMotion.AppleEaseOut),
                            )).togetherWith(
                                slideOutHorizontally(
                                    targetOffsetX = { (it * 0.22f).roundToInt() },
                                    animationSpec = RonaMotion.appleSpring(),
                                ) + fadeOut(
                                    animationSpec = tween(180, easing = RonaMotion.AppleEaseIn),
                                )
                            )
                        }
                    },
                    label = "guidedStepTransition",
                ) { step ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                    ) {
                        when (step) {
                            DailyCheckInStep.Initial -> CardInitialChoice(
                                onSelectChoice = viewModel::selectInitialChoice,
                                onOpenFullEditor = onOpenFullEditor,
                            )
                            DailyCheckInStep.QuickEnergy -> CardQuickEnergy(
                                selectedEnergy = uiState.energy,
                                onSelectEnergy = viewModel::selectEnergy,
                                onNext = { viewModel.navigateToStep(DailyCheckInStep.Summary) },
                                onSkip = { viewModel.navigateToStep(DailyCheckInStep.Summary) },
                                onOpenFullEditor = onOpenFullEditor,
                            )
                            DailyCheckInStep.PeriodStartConfirm -> CardPeriodStartConfirm(
                                isConfirming = uiState.isStartingPeriod,
                                onConfirm = viewModel::confirmPeriodStart,
                                onCancel = viewModel::cancelPeriodStart,
                            )
                            DailyCheckInStep.Flow -> CardFlowSelection(
                                selectedFlow = uiState.flow,
                                onSelectFlow = viewModel::selectFlow,
                                onNext = viewModel::nextStep,
                                onSkip = viewModel::skipStep,
                                onOpenFullEditor = onOpenFullEditor,
                            )
                            DailyCheckInStep.Symptoms -> CardSymptomSelection(
                                selectedSymptoms = uiState.selectedSymptoms.keys,
                                onToggleSymptom = viewModel::toggleSymptom,
                                onNext = viewModel::nextStep,
                                onSkip = viewModel::skipStep,
                                onOpenFullEditor = onOpenFullEditor,
                            )
                            DailyCheckInStep.Severity -> CardSeveritySelection(
                                selectedSymptoms = uiState.selectedSymptoms,
                                onSetSeverity = viewModel::setSymptomSeverity,
                                onNext = viewModel::nextStep,
                                onOpenFullEditor = onOpenFullEditor,
                            )
                            DailyCheckInStep.Energy -> CardEnergySelection(
                                selectedEnergy = uiState.energy,
                                selectedMood = uiState.mood,
                                onSelectEnergy = viewModel::selectEnergy,
                                onSelectMood = viewModel::selectMood,
                                onNext = viewModel::nextStep,
                                onSkip = viewModel::skipStep,
                                onOpenFullEditor = onOpenFullEditor,
                            )
                            DailyCheckInStep.Note -> CardNoteInput(
                                note = uiState.note,
                                onNoteChange = viewModel::setNote,
                                isAnalyzing = uiState.isAnalyzing,
                                onRunAnalysis = viewModel::runAnalysis,
                                onNext = viewModel::nextStep,
                                onSkip = viewModel::skipStep,
                                onOpenFullEditor = onOpenFullEditor,
                            )
                            DailyCheckInStep.Summary -> CardCheckInSummary(
                                uiState = uiState,
                                onSave = viewModel::save,
                                onOpenFullEditor = onOpenFullEditor,
                                onEditStep = { viewModel.navigateToStep(it) },
                                isSaving = uiState.isSaving,
                                errorMessage = uiState.error,
                            )
                        }
                    }
                }
            }
        }
    }

    // ————— NLP Analysis Result Bottom Sheet —————
    if (uiState.showAnalysisResult && uiState.analysisResult != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissAnalysisResult,
            sheetState = sheetState,
        ) {
            NoteAnalysisResultSheet(
                result = uiState.analysisResult!!,
                onApply = viewModel::applySuggestions,
                onDismiss = viewModel::dismissAnalysisResult,
                onAcknowledgeSafety = viewModel::acknowledgeSafetyAlerts,
            )
        }
    }
}

// ───────────────────────── Modular Card Composables ─────────────────────────

/**
 * Step 1: Initial Choice Card
 */
@Composable
fun CardInitialChoice(
    onSelectChoice: (InitialChoiceOption) -> Unit,
    onOpenFullEditor: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalRonaColors.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Bagaimana keadaanmu hari ini?",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                lineHeight = 32.sp,
                color = colors.inkPrimary,
            ),
        )
        Text(
            text = "Pilih entri cepat atau catat keluhan spesifikmu.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp,
                color = colors.inkSecondary,
            ),
        )

        Spacer(Modifier.height(4.dp))

        // 1. Fast path: "Baik-baik saja"
        InitialOptionCard(
            title = "Baik-baik saja",
            subtitle = "Simpan check-in singkat hari ini tanpa mengisi data kesehatan.",
            icon = Icons.Rounded.Favorite,
            badgeColor = colors.sageContainer,
            iconTint = colors.sageAccent,
            onClick = { onSelectChoice(InitialChoiceOption.FEELING_GOOD) },
        )

        // 2. "Ada menstruasi / pendarahan"
        InitialOptionCard(
            title = "Ada menstruasi / pendarahan",
            subtitle = "Catat aliran menstruasi atau bercak (spotting).",
            icon = Icons.Rounded.WaterDrop,
            badgeColor = colors.cycleContainer,
            iconTint = colors.cyclePrimary,
            onClick = { onSelectChoice(InitialChoiceOption.PERIOD_FLOW) },
        )

        // 3. "Ada gejala fisik atau keluhan"
        InitialOptionCard(
            title = "Ada gejala atau keluhan",
            subtitle = "Kram perut, sakit kepala, kembung, mual, dll.",
            icon = Icons.Rounded.Healing,
            badgeColor = colors.plumContainer,
            iconTint = colors.plumAccent,
            onClick = { onSelectChoice(InitialChoiceOption.SYMPTOMS) },
        )

        // 4. "Catat suasana hati & energi"
        InitialOptionCard(
            title = "Catat suasana hati & energi",
            subtitle = "Tingkat energi dan suasana hati hari ini.",
            icon = Icons.Rounded.EditNote,
            badgeColor = colors.amberContainer,
            iconTint = colors.amberAccent,
            onClick = { onSelectChoice(InitialChoiceOption.ENERGY_MOOD) },
        )

        Spacer(Modifier.height(12.dp))

        // Option to open full editor
        TextButton(
            onClick = onOpenFullEditor,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text(
                text = "Buka formulir editor lengkap",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Medium,
                    color = colors.cyclePrimary,
                ),
            )
        }
    }
}

/**
 * Step 1b: Optional energy capture on the short "Baik-baik saja" path.
 * Energy stays null unless the user explicitly selects it — no defaults are
 * fabricated. "Lewati" lands straight on Summary with no wellness value.
 */
@Composable
fun CardQuickEnergy(
    selectedEnergy: Energy?,
    onSelectEnergy: (Energy?) -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onOpenFullEditor: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalRonaColors.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            text = "Senang mendengarnya.",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = colors.inkPrimary,
            ),
        )
        Text(
            text = "Mau mencatat energimu?",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp,
                color = colors.inkSecondary,
            ),
        )

        Energy.entries.forEach { energy ->
            RonaSelectableChip(
                label = energyLabel(energy),
                selected = selectedEnergy == energy,
                onClick = { onSelectEnergy(if (selectedEnergy == energy) null else energy) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(20.dp))

        CardActionFooter(
            onSkip = onSkip,
            onNext = onNext,
            onOpenFullEditor = onOpenFullEditor,
            nextText = "Lanjut ke Ringkasan",
        )
    }
}

/**
 * Step 1c: Period-start confirmation. Only shown when the user indicates period
 * flow but no PeriodRecord covers the target day. The PeriodRecord is created
 * only after an explicit confirmation — declining does nothing.
 */
@Composable
fun CardPeriodStartConfirm(
    isConfirming: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalRonaColors.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            text = "Apakah hari ini hari pertama periode?",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = colors.inkPrimary,
            ),
        )
        Text(
            text = "Kami belum menemukan catatan periode yang sedang berlangsung untuk tanggal ini. " +
                "Konfirmasi untuk memulai periode baru.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp,
                color = colors.inkSecondary,
            ),
        )

        Spacer(Modifier.height(24.dp))

        RonaPrimaryButton(
            text = if (isConfirming) "Memulai…" else "Ya, mulai periode hari ini",
            onClick = onConfirm,
            enabled = !isConfirming,
            modifier = Modifier.fillMaxWidth(),
        )

        RonaSecondaryButton(
            text = "Kembali",
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun InitialOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badgeColor: Color,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalRonaColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = RonaMotion.appleBouncySpring(),
        label = "initialOptionScale",
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = MaterialTheme.shapes.medium,
        color = colors.surfaceSoft,
        border = BorderStroke(1.dp, colors.dividerSubtle),
        modifier = modifier
            .fillMaxWidth()
            .scale(pressScale),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = badgeColor,
                modifier = Modifier.size(46.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.inkPrimary,
                    ),
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = colors.inkSecondary,
                    ),
                )
            }
        }
    }
}

/**
 * Step 2: Flow Selection Card
 */
@Composable
fun CardFlowSelection(
    selectedFlow: FlowLevel?,
    onSelectFlow: (FlowLevel?) -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onOpenFullEditor: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalRonaColors.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            text = "Aliran Menstruasi",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = colors.inkPrimary,
            ),
        )
        Text(
            text = "Pilih tingkat aliran menstruasi atau lewati jika tidak sedang haid.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp,
                color = colors.inkSecondary,
            ),
        )

        Spacer(Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FlowOptionItem(
                label = "Spotting (Bercak)",
                description = "Hanya sedikit bercak atau flek",
                selected = selectedFlow == FlowLevel.SPOTTING,
                onClick = { onSelectFlow(if (selectedFlow == FlowLevel.SPOTTING) null else FlowLevel.SPOTTING) },
            )
            FlowOptionItem(
                label = "Ringan",
                description = "Aliran tipis atau sedikit",
                selected = selectedFlow == FlowLevel.LIGHT,
                onClick = { onSelectFlow(if (selectedFlow == FlowLevel.LIGHT) null else FlowLevel.LIGHT) },
            )
            FlowOptionItem(
                label = "Sedang",
                description = "Aliran teratur / normal harian",
                selected = selectedFlow == FlowLevel.MEDIUM,
                onClick = { onSelectFlow(if (selectedFlow == FlowLevel.MEDIUM) null else FlowLevel.MEDIUM) },
            )
            FlowOptionItem(
                label = "Banyak",
                description = "Aliran deras / perlu ganti pembalut lebih sering",
                selected = selectedFlow == FlowLevel.HEAVY,
                onClick = { onSelectFlow(if (selectedFlow == FlowLevel.HEAVY) null else FlowLevel.HEAVY) },
            )
        }

        Spacer(Modifier.height(20.dp))

        CardActionFooter(
            onSkip = onSkip,
            onNext = onNext,
            onOpenFullEditor = onOpenFullEditor,
        )
    }
}

@Composable
private fun FlowOptionItem(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalRonaColors.current
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) colors.cycleContainer else colors.surfaceSoft,
        border = BorderStroke(
            1.dp,
            if (selected) colors.cyclePrimary else colors.dividerSubtle,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) colors.onCycleContainer else colors.inkPrimary,
                    ),
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        color = colors.inkSecondary,
                    ),
                )
            }

            if (selected) {
                Surface(
                    shape = CircleShape,
                    color = colors.cyclePrimary,
                    modifier = Modifier.size(26.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = colors.onCyclePrimary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Step 3: Symptom Selection Card
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CardSymptomSelection(
    selectedSymptoms: Set<SymptomType>,
    onToggleSymptom: (SymptomType) -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onOpenFullEditor: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalRonaColors.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            text = "Gejala & Keluhan",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = colors.inkPrimary,
            ),
        )
        Text(
            text = "Pilih gejala yang kamu rasakan (bisa pilih beberapa sekaligus).",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp,
                color = colors.inkSecondary,
            ),
        )

        Spacer(Modifier.height(4.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            SymptomType.entries.forEach { symptom ->
                RonaSelectableChip(
                    label = symptomLabel(symptom),
                    selected = selectedSymptoms.contains(symptom),
                    onClick = { onToggleSymptom(symptom) },
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        CardActionFooter(
            onSkip = onSkip,
            onNext = onNext,
            onOpenFullEditor = onOpenFullEditor,
            nextText = if (selectedSymptoms.isNotEmpty()) "Tentukan Tingkat Keparahan" else "Lanjut",
        )
    }
}

/**
 * Step 4: Symptom Severity Selection Card (Conditional)
 */
@Composable
fun CardSeveritySelection(
    selectedSymptoms: Map<SymptomType, Severity>,
    onSetSeverity: (SymptomType, Severity) -> Unit,
    onNext: () -> Unit,
    onOpenFullEditor: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalRonaColors.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            text = "Intensitas Gejala",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = colors.inkPrimary,
            ),
        )
        Text(
            text = "Tentukan seberapa berat gejala yang kamu rasakan.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp,
                color = colors.inkSecondary,
            ),
        )

        Spacer(Modifier.height(4.dp))

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            selectedSymptoms.forEach { (symptom, severity) ->
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = colors.surfaceSoft,
                    border = BorderStroke(1.dp, colors.dividerSubtle),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = symptomLabel(symptom),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = colors.inkPrimary,
                            ),
                        )

                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            Severity.entries.forEachIndexed { index, sev ->
                                SegmentedButton(
                                    selected = severity == sev,
                                    onClick = { onSetSeverity(symptom, sev) },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = Severity.entries.size,
                                    ),
                                ) {
                                    Text(
                                        when (sev) {
                                            Severity.MILD -> "Ringan"
                                            Severity.MODERATE -> "Sedang"
                                            Severity.SEVERE -> "Berat"
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        CardActionFooter(
            onSkip = null,
            onNext = onNext,
            onOpenFullEditor = onOpenFullEditor,
        )
    }
}

/**
 * Step 5: Energy & Mood Selection Card
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CardEnergySelection(
    selectedEnergy: Energy?,
    selectedMood: Mood?,
    onSelectEnergy: (Energy?) -> Unit,
    onSelectMood: (Mood?) -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onOpenFullEditor: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalRonaColors.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = "Energi & Suasana Hati",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = colors.inkPrimary,
            ),
        )

        // ——— Energi ———
        RonaSection(title = "Tingkat Energi", supporting = "Seberapa bertenaga tubuhmu hari ini?") {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Energy.entries.forEach { energy ->
                    RonaSelectableChip(
                        label = energyLabel(energy),
                        selected = selectedEnergy == energy,
                        onClick = { onSelectEnergy(if (selectedEnergy == energy) null else energy) },
                    )
                }
            }
        }

        // ——— Mood ———
        RonaSection(title = "Suasana Hati (Mood)", supporting = "Bagaimana perasaan atau suasana hatimu?") {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Mood.entries.forEach { mood ->
                    RonaSelectableChip(
                        label = moodLabel(mood),
                        selected = selectedMood == mood,
                        onClick = { onSelectMood(if (selectedMood == mood) null else mood) },
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        CardActionFooter(
            onSkip = onSkip,
            onNext = onNext,
            onOpenFullEditor = onOpenFullEditor,
        )
    }
}

/**
 * Step 6: Note Input Card
 */
@Composable
fun CardNoteInput(
    note: String,
    onNoteChange: (String) -> Unit,
    isAnalyzing: Boolean,
    onRunAnalysis: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onOpenFullEditor: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalRonaColors.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            text = "Catatan Pribadi",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = colors.inkPrimary,
            ),
        )
        Text(
            text = "Tuliskan apa saja yang kamu rasakan atau alami hari ini. Catatan disimpan privat & terenkripsi.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp,
                color = colors.inkSecondary,
            ),
        )

        RonaJournalTextField(
            value = note,
            onValueChange = onNoteChange,
            placeholder = "Misalnya: Merasa sedikit kram setelah berolahraga, minum teh hangat...",
            minLines = 4,
        )

        // NLP Smart Analysis Option
        OutlinedButton(
            onClick = onRunAnalysis,
            enabled = note.isNotBlank() && !isAnalyzing,
            shape = RonaPillShape,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isAnalyzing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = colors.cyclePrimary,
                )
                Spacer(Modifier.width(8.dp))
                Text("Menganalisis catatan…")
            } else {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = colors.cyclePrimary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("Analisis catatan dengan AI lokal", color = colors.cyclePrimary)
            }
        }

        Spacer(Modifier.height(16.dp))

        CardActionFooter(
            onSkip = onSkip,
            onNext = onNext,
            onOpenFullEditor = onOpenFullEditor,
            nextText = "Lanjut ke Ringkasan",
        )
    }
}

/**
 * Step 7: Summary & Save Card
 */
@Composable
fun CardCheckInSummary(
    uiState: LogEditorUiState,
    onSave: () -> Unit,
    onOpenFullEditor: () -> Unit,
    onEditStep: (DailyCheckInStep) -> Unit,
    isSaving: Boolean,
    errorMessage: String?,
    modifier: Modifier = Modifier,
) {
    val colors = LocalRonaColors.current
    val bareCheckIn = uiState.flow == null &&
        uiState.selectedSymptoms.isEmpty() &&
        uiState.mood == null &&
        uiState.energy == null &&
        uiState.note.isBlank()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Ringkasan Hari Ini",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = colors.inkPrimary,
            ),
        )
        Text(
            text = "Catatanmu sudah siap disimpan ke dalam riwayat siklus.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp,
                color = colors.inkSecondary,
            ),
        )

        Surface(
            shape = MaterialTheme.shapes.medium,
            color = colors.surfaceSoft,
            border = BorderStroke(1.dp, colors.dividerSubtle),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Bare "Baik-baik saja" check-in: surface the short-entry label only.
                if (bareCheckIn) {
                    SummaryRow(
                        label = "Status",
                        value = "Check-in singkat tercatat",
                    )
                }

                // Aliran
                SummaryRow(
                    label = "Aliran Menstruasi",
                    value = when (uiState.flow) {
                        FlowLevel.SPOTTING -> "Spotting (Bercak)"
                        FlowLevel.LIGHT -> "Ringan"
                        FlowLevel.MEDIUM -> "Sedang"
                        FlowLevel.HEAVY -> "Banyak"
                        null -> "Tidak dicatat"
                    },
                    onEdit = { onEditStep(DailyCheckInStep.Flow) },
                )

                // Gejala
                SummaryRow(
                    label = "Gejala Tercatat",
                    value = if (uiState.selectedSymptoms.isEmpty()) {
                        "Tidak dicatat"
                    } else {
                        uiState.selectedSymptoms.entries.joinToString(", ") { (sym, sev) ->
                            "${symptomLabel(sym)} (${when (sev) {
                                Severity.MILD -> "Ringan"
                                Severity.MODERATE -> "Sedang"
                                Severity.SEVERE -> "Berat"
                            }})"
                        }
                    },
                    onEdit = if (uiState.selectedSymptoms.isNotEmpty()) {
                        { onEditStep(DailyCheckInStep.Symptoms) }
                    } else {
                        null
                    },
                )

                // Energi & Mood
                val energyMood = buildString {
                    if (uiState.energy != null) append(energyLabel(uiState.energy))
                    if (uiState.energy != null && uiState.mood != null) append(" · ")
                    if (uiState.mood != null) append("Mood: ${moodLabel(uiState.mood)}")
                    if (uiState.energy == null && uiState.mood == null) append("Tidak dicatat")
                }
                SummaryRow(
                    label = "Energi & Mood",
                    value = energyMood,
                    onEdit = if (uiState.energy != null || uiState.mood != null) {
                        { onEditStep(DailyCheckInStep.Energy) }
                    } else {
                        null
                    },
                )

                // Catatan
                if (uiState.note.isNotBlank()) {
                    SummaryRow(
                        label = "Catatan",
                        value = uiState.note,
                        onEdit = { onEditStep(DailyCheckInStep.Note) },
                    )
                }
            }
        }

        if (errorMessage != null) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(12.dp))

        RonaPrimaryButton(
            text = if (isSaving) "Menyimpan…" else "Simpan Catatan",
            onClick = onSave,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth(),
        )

        RonaSecondaryButton(
            text = "Tambah detail di formulir lengkap",
            onClick = onOpenFullEditor,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    onEdit: (() -> Unit)? = null,
) {
    val colors = LocalRonaColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.inkTertiary,
                ),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.inkPrimary,
                ),
            )
        }
        if (onEdit != null) {
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Edit",
                    tint = colors.inkSecondary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

/**
 * Standard footer navigation buttons for question cards.
 */
@Composable
private fun CardActionFooter(
    onSkip: (() -> Unit)?,
    onNext: () -> Unit,
    onOpenFullEditor: () -> Unit,
    nextText: String = "Lanjut",
    modifier: Modifier = Modifier,
) {
    val colors = LocalRonaColors.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (onSkip != null) {
                RonaSecondaryButton(
                    text = "Lewati",
                    onClick = onSkip,
                    modifier = Modifier.weight(1f),
                )
            }
            RonaPrimaryButton(
                text = nextText,
                onClick = onNext,
                modifier = Modifier.weight(if (onSkip != null) 1f else 2f),
            )
        }

        TextButton(
            onClick = onOpenFullEditor,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text(
                text = "Tambah detail",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = colors.inkSecondary,
                ),
            )
        }
    }
}

private fun DailyCheckInStep.stepOrdinal(): Int = when (this) {
    DailyCheckInStep.Initial -> 0
    DailyCheckInStep.QuickEnergy -> 1
    DailyCheckInStep.PeriodStartConfirm -> 2
    DailyCheckInStep.Flow -> 3
    DailyCheckInStep.Symptoms -> 4
    DailyCheckInStep.Severity -> 5
    DailyCheckInStep.Energy -> 6
    DailyCheckInStep.Note -> 7
    DailyCheckInStep.Summary -> 8
}

// ───────────────────────── Previews ─────────────────────────

@Preview(name = "CardInitialChoice — Light", showBackground = true)
@Composable
private fun CardInitialChoicePreview() {
    RonaTheme {
        CardInitialChoice(onSelectChoice = {}, onOpenFullEditor = {})
    }
}

@Preview(name = "CardInitialChoice — Dark", showBackground = true)
@Composable
private fun CardInitialChoiceDarkPreview() {
    RonaTheme(themeMode = ThemeMode.DARK) {
        CardInitialChoice(onSelectChoice = {}, onOpenFullEditor = {})
    }
}

@Preview(name = "CardCheckInSummary — Light", showBackground = true)
@Composable
private fun CardCheckInSummaryPreview() {
    RonaTheme {
        CardCheckInSummary(
            uiState = LogEditorUiState(
                flow = FlowLevel.MEDIUM,
                selectedSymptoms = mapOf(SymptomType.KRAM to Severity.MODERATE),
                energy = Energy.HIGH,
                mood = Mood.GOOD,
                note = "Hari ini merasa cukup baik.",
            ),
            onSave = {},
            onOpenFullEditor = {},
            onEditStep = {},
            isSaving = false,
            errorMessage = null,
        )
    }
}

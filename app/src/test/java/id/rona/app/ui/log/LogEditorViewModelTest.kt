package id.rona.app.ui.log

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import id.rona.app.data.db.dao.NlpSuggestionDao
import id.rona.app.data.db.dao.PeriodRecordDao
import id.rona.app.data.db.dao.DailyLogDao
import id.rona.app.data.db.dao.SymptomLogDao
import id.rona.app.data.db.entity.NlpSuggestionEntity
import id.rona.app.data.db.entity.PeriodRecordEntity
import id.rona.app.data.db.entity.SymptomLogEntity
import id.rona.app.data.db.entity.DailyLogEntity
import id.rona.app.data.repository.DailyLogRepository
import id.rona.app.data.repository.PeriodRecordRepository
import id.rona.app.domain.model.Energy
import id.rona.app.domain.model.FlowLevel
import id.rona.app.domain.model.Mood
import id.rona.app.domain.model.NlpStatus
import id.rona.app.domain.model.Severity
import id.rona.app.domain.model.SymptomType
import id.rona.app.domain.nlp.ConfidenceLevel
import id.rona.app.domain.nlp.LocalNoteAnalyzer
import id.rona.app.domain.nlp.NoteAnalysisInput
import id.rona.app.domain.nlp.NoteAnalysisResult
import id.rona.app.domain.nlp.SymptomSuggestion
import id.rona.app.ui.home.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class LogEditorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakePeriodRecordDao: FakePeriodRecordDao
    private lateinit var periodRecordRepository: PeriodRecordRepository
    private lateinit var fakeNlpSuggestionDao: FakeNlpSuggestionDao
    private lateinit var fakeAnalyzer: FakeLocalNoteAnalyzer
    private lateinit var dailyLogRepository: DailyLogRepository
    private lateinit var fakeDailyLogDao: FakeDailyLogDao
    private lateinit var fakeSymptomLogDao: FakeSymptomLogDao
    private lateinit var viewModel: LogEditorViewModel

    @Before
    fun setUp() {
        fakePeriodRecordDao = FakePeriodRecordDao()
        periodRecordRepository = PeriodRecordRepository(
            transactionRunner = object : id.rona.app.data.db.TransactionRunner {
                override suspend fun <T> invoke(block: suspend () -> T): T = block()
            },
            periodRecordDao = fakePeriodRecordDao,
            cyclePredictionDao = FakeCyclePredictionDao(),
        )
        fakeNlpSuggestionDao = FakeNlpSuggestionDao()
        fakeAnalyzer = FakeLocalNoteAnalyzer()
        fakeDailyLogDao = FakeDailyLogDao()
        fakeSymptomLogDao = FakeSymptomLogDao()
        dailyLogRepository = DailyLogRepository(
            transactionRunner = object : id.rona.app.data.db.TransactionRunner {
                override suspend fun <T> invoke(block: suspend () -> T): T = block()
            },
            dailyLogDao = fakeDailyLogDao,
            symptomLogDao = fakeSymptomLogDao,
        )

        viewModel = LogEditorViewModel(
            repository = dailyLogRepository,
            localNoteAnalyzer = fakeAnalyzer,
            periodRecordDao = fakePeriodRecordDao,
            periodRecordRepository = periodRecordRepository,
            nlpSuggestionDao = fakeNlpSuggestionDao,
        )
    }

    // 1. Initial state starts on Initial step with clean empty values
    @Test
    fun `initial state has Initial step and empty values`() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.currentStep).isEqualTo(DailyCheckInStep.Initial)
            assertThat(state.stepHistory).containsExactly(DailyCheckInStep.Initial)
            assertThat(state.flow).isNull()
            assertThat(state.selectedSymptoms).isEmpty()
            assertThat(state.mood).isNull()
            assertThat(state.energy).isNull()
            assertThat(state.note).isEmpty()
            assertThat(state.saved).isFalse()
        }
    }

    // 2. Fast "Baik-baik saja" path — no fabricated values, optional energy capture first
    @Test
    fun `selectFeelingOk routes to QuickEnergy without fabricating mood or energy`() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.selectInitialChoice(InitialChoiceOption.FEELING_GOOD)

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.currentStep).isEqualTo(DailyCheckInStep.QuickEnergy)
            assertThat(state.stepHistory).containsExactly(
                DailyCheckInStep.Initial,
                DailyCheckInStep.QuickEnergy,
            ).inOrder()
            assertThat(state.mood).isNull()
            assertThat(state.energy).isNull()
            assertThat(state.selectedSymptoms).isEmpty()
            assertThat(state.flow).isNull()
        }
    }

    // 2b. Skipping QuickEnergy lands on Summary still with no fabricated values
    @Test
    fun `skipping QuickEnergy goes straight to Summary with no fabricated values`() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.selectInitialChoice(InitialChoiceOption.FEELING_GOOD)
        viewModel.skipStep()

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.currentStep).isEqualTo(DailyCheckInStep.Summary)
            assertThat(state.mood).isNull()
            assertThat(state.energy).isNull()
            assertThat(state.selectedSymptoms).isEmpty()
            assertThat(state.flow).isNull()
        }
    }

    // 2c. Selecting energy on the quick path persists only the chosen energy
    @Test
    fun `quick path with selected energy keeps only the explicitly chosen energy`() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.selectInitialChoice(InitialChoiceOption.FEELING_GOOD)
        viewModel.selectEnergy(Energy.LOW)
        viewModel.navigateToStep(DailyCheckInStep.Summary)

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.currentStep).isEqualTo(DailyCheckInStep.Summary)
            assertThat(state.mood).isNull()
            assertThat(state.energy).isEqualTo(Energy.LOW)
        }
    }

    // 3. Routing from Initial card to specific sections
    @Test
    fun `selectInitialChoice routes to correct starting cards`() = runTest(mainDispatcherRule.testDispatcher) {
        // PERIOD_FLOW -> PeriodStartConfirm (no ongoing period in this fresh session)
        viewModel.selectInitialChoice(InitialChoiceOption.PERIOD_FLOW)
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(DailyCheckInStep.PeriodStartConfirm)

        // Reset and test SYMPTOMS -> Symptoms
        viewModel.resetStep()
        viewModel.selectInitialChoice(InitialChoiceOption.SYMPTOMS)
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(DailyCheckInStep.Symptoms)

        // Reset and test ENERGY_MOOD -> Energy
        viewModel.resetStep()
        viewModel.selectInitialChoice(InitialChoiceOption.ENERGY_MOOD)
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(DailyCheckInStep.Energy)
    }

    // 4. Step-by-step progression with symptoms navigates to Severity
    @Test
    fun `step progression follows Flow to Symptoms to Severity when symptoms present`() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.navigateToStep(DailyCheckInStep.Flow)
        viewModel.selectFlow(FlowLevel.LIGHT)
        viewModel.nextStep() // from Flow -> Symptoms

        assertThat(viewModel.uiState.value.currentStep).isEqualTo(DailyCheckInStep.Symptoms)

        viewModel.toggleSymptom(SymptomType.KRAM, Severity.MILD)
        viewModel.nextStep() // from Symptoms (with symptoms) -> Severity

        assertThat(viewModel.uiState.value.currentStep).isEqualTo(DailyCheckInStep.Severity)

        viewModel.setSymptomSeverity(SymptomType.KRAM, Severity.MODERATE)
        assertThat(viewModel.uiState.value.selectedSymptoms[SymptomType.KRAM]).isEqualTo(Severity.MODERATE)

        viewModel.nextStep() // from Severity -> Energy
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(DailyCheckInStep.Energy)

        viewModel.selectEnergy(Energy.LOW)
        viewModel.selectMood(Mood.LOW)
        viewModel.nextStep() // from Energy -> Note
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(DailyCheckInStep.Note)

        viewModel.setNote("Merasa lelah hari ini")
        viewModel.nextStep() // from Note -> Summary
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(DailyCheckInStep.Summary)
    }

    // 5. Conditional Step: Skips Severity when no symptoms are selected
    @Test
    fun `step progression skips Severity when no symptoms are selected`() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.navigateToStep(DailyCheckInStep.Symptoms)
        assertThat(viewModel.uiState.value.selectedSymptoms).isEmpty()

        viewModel.nextStep() // Should jump straight to Energy, skipping Severity
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(DailyCheckInStep.Energy)
    }

    // 6. Skipping steps
    @Test
    fun `skipStep on Symptoms clears symptoms and advances to Energy`() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.navigateToStep(DailyCheckInStep.Symptoms)
        viewModel.toggleSymptom(SymptomType.HEADACHE, Severity.SEVERE)
        assertThat(viewModel.uiState.value.selectedSymptoms).isNotEmpty()

        viewModel.skipStep()
        assertThat(viewModel.uiState.value.selectedSymptoms).isEmpty()
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(DailyCheckInStep.Energy)
    }

    // 7. Back navigation retains previously selected answers
    @Test
    fun `previousStep pops history and retains previously selected values`() = runTest(mainDispatcherRule.testDispatcher) {
        // Seed an ongoing period so PERIOD_FLOW routes straight to Flow (as before Phase A).
        val today = LocalDate.of(2026, 8, 23)
        fakePeriodRecordDao.ongoing = PeriodRecordEntity(
            id = 1L,
            startEpochDay = today.toEpochDay(),
            endEpochDay = null,
            createdAt = 0L,
            updatedAt = 0L,
        )
        viewModel.load(1L, today)

        viewModel.selectInitialChoice(InitialChoiceOption.PERIOD_FLOW)
        viewModel.selectFlow(FlowLevel.MEDIUM)
        viewModel.nextStep() // to Symptoms

        viewModel.toggleSymptom(SymptomType.NAUSEA, Severity.MILD)
        viewModel.nextStep() // to Severity

        viewModel.setSymptomSeverity(SymptomType.NAUSEA, Severity.SEVERE)
        viewModel.nextStep() // to Energy

        viewModel.selectEnergy(Energy.VERY_LOW)

        // Now press Back multiple times
        viewModel.previousStep() // returns to Severity
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(DailyCheckInStep.Severity)
        assertThat(viewModel.uiState.value.selectedSymptoms[SymptomType.NAUSEA]).isEqualTo(Severity.SEVERE)
        assertThat(viewModel.uiState.value.energy).isEqualTo(Energy.VERY_LOW) // preserved!

        viewModel.previousStep() // returns to Symptoms
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(DailyCheckInStep.Symptoms)
        assertThat(viewModel.uiState.value.selectedSymptoms).containsKey(SymptomType.NAUSEA)

        viewModel.previousStep() // returns to Flow
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(DailyCheckInStep.Flow)
        assertThat(viewModel.uiState.value.flow).isEqualTo(FlowLevel.MEDIUM) // preserved!

        viewModel.previousStep() // returns to Initial
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(DailyCheckInStep.Initial)
    }

    // 8. Save persists to repository and sets save state to Saved (transient)
    @Test
    fun `save persists log through repository and sets Saved`() = runTest(mainDispatcherRule.testDispatcher) {
        val today = LocalDate.of(2026, 8, 23)
        viewModel.load(1L, today)

        viewModel.selectFlow(FlowLevel.HEAVY)
        viewModel.toggleSymptom(SymptomType.KRAM, Severity.SEVERE)
        viewModel.selectMood(Mood.LOW)
        viewModel.selectEnergy(Energy.VERY_LOW)
        viewModel.setNote("Catatan privat hari ini")

        viewModel.save()

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.saveState).isEqualTo(SaveState.Saved)
            assertThat(state.saved).isTrue()
            assertThat(state.isSaving).isFalse()
            assertThat(state.error).isNull()
        }

        // Verify repository received the save call via the in-memory DAOs
        assertThat(fakeDailyLogDao.savedLogs).hasSize(1)
        val saved = fakeDailyLogDao.savedLogs.single()
        assertThat(saved.flow).isEqualTo(FlowLevel.HEAVY)
        assertThat(saved.mood).isEqualTo(Mood.LOW)
        assertThat(saved.energy).isEqualTo(Energy.VERY_LOW)
        assertThat(saved.note).isEqualTo("Catatan privat hari ini")
        assertThat(fakeSymptomLogDao.savedSymptoms.single().symptomType).isEqualTo(SymptomType.KRAM)
        assertThat(fakeSymptomLogDao.savedSymptoms.single().severity).isEqualTo(Severity.SEVERE)
    }

    // 8b. Same session identity is idempotent; a new identity resets state.
    @Test
    fun `same session load preserves answers but new session load resets them`() = runTest(mainDispatcherRule.testDispatcher) {
        val today = LocalDate.of(2026, 8, 23)
        viewModel.load(1L, today)
        viewModel.selectFlow(FlowLevel.LIGHT)
        viewModel.setNote("isi sementara")

        viewModel.load(1L, today)
        assertThat(viewModel.uiState.value.flow).isEqualTo(FlowLevel.LIGHT)
        assertThat(viewModel.uiState.value.note).isEqualTo("isi sementara")

        viewModel.load(2L, today)
        assertThat(viewModel.uiState.value.flow).isNull()
        assertThat(viewModel.uiState.value.note).isEmpty()
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(DailyCheckInStep.Initial)
    }

    // 8c. Acknowledging the save resets the session cleanly (reopen starts fresh)
    @Test
    fun `acknowledgeSaved resets session for clean reopen`() = runTest(mainDispatcherRule.testDispatcher) {
        val today = LocalDate.of(2026, 8, 23)
        viewModel.load(1L, today)
        viewModel.selectFlow(FlowLevel.LIGHT)
        viewModel.setNote("isi lama")
        viewModel.save()
        viewModel.acknowledgeSaved()

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.saveState).isEqualTo(SaveState.Idle)
            assertThat(state.currentStep).isEqualTo(DailyCheckInStep.Initial)
            assertThat(state.stepHistory).containsExactly(DailyCheckInStep.Initial)
            assertThat(state.flow).isNull()
            assertThat(state.note).isEmpty()
        }
    }

    // 9. NLP Suggestions applied to state
    @Test
    fun `applySuggestions updates state from NLP analysis result`() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.setNote("Sakit kepala dan kembung")
        val analysis = NoteAnalysisResult(
            normalizedText = "sakit kepala dan kembung",
            symptomSuggestions = listOf(
                SymptomSuggestion(
                    symptomType = SymptomType.HEADACHE,
                    severity = Severity.MODERATE,
                    confidence = ConfidenceLevel.HIGH,
                    matchedTerms = listOf("sakit kepala"),
                ),
            ),
            moodSuggestions = emptyList(),
            energySuggestion = null,
            flowSuggestion = null,
            dischargeSuggestion = null,
            durationSuggestion = null,
            pregnancyContext = null,
            detectedIntents = emptyList(),
            safetyAlerts = emptyList(),
            relevantKnowledgeCards = emptyList(),
            ruleVersion = "1.0",
        )
        fakeAnalyzer.stubResult = analysis

        viewModel.runAnalysis()
        testScheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.showAnalysisResult).isTrue()
        assertThat(viewModel.uiState.value.analysisResult).isNotNull()

        viewModel.applySuggestions()

        assertThat(viewModel.uiState.value.selectedSymptoms).containsKey(SymptomType.HEADACHE)
        assertThat(viewModel.uiState.value.selectedSymptoms[SymptomType.HEADACHE]).isEqualTo(Severity.MODERATE)
        assertThat(viewModel.uiState.value.showAnalysisResult).isFalse()
    }

    // 10. Period context: no ongoing period -> PERIOD_FLOW requires explicit confirmation
    @Test
    fun `period start confirmation creates a period record and continues to Flow`() = runTest(mainDispatcherRule.testDispatcher) {
        val today = LocalDate.of(2026, 8, 23)
        viewModel.load(1L, today)

        viewModel.selectInitialChoice(InitialChoiceOption.PERIOD_FLOW)
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(DailyCheckInStep.PeriodStartConfirm)

        viewModel.confirmPeriodStart()
        testScheduler.advanceUntilIdle()

        // Period record created and we continue to Flow
        assertThat(fakePeriodRecordDao.getAll()).hasSize(1)
        assertThat(fakePeriodRecordDao.getAll().single().startEpochDay).isEqualTo(today.toEpochDay())
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(DailyCheckInStep.Flow)
        assertThat(viewModel.uiState.value.isPeriodContextAvailable).isTrue()
    }

    // 10b. Declining the period start persists nothing and returns to Initial
    @Test
    fun `declining period start persists nothing and returns to Initial`() = runTest(mainDispatcherRule.testDispatcher) {
        val today = LocalDate.of(2026, 8, 23)
        viewModel.load(1L, today)

        viewModel.selectInitialChoice(InitialChoiceOption.PERIOD_FLOW)
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(DailyCheckInStep.PeriodStartConfirm)

        viewModel.cancelPeriodStart()

        assertThat(fakePeriodRecordDao.getAll()).isEmpty()
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(DailyCheckInStep.Initial)
    }

    // 10c. When a period already covers the day, PERIOD_FLOW goes straight to Flow
    @Test
    fun `period flow goes straight to Flow when period context is available`() = runTest(mainDispatcherRule.testDispatcher) {
        val today = LocalDate.of(2026, 8, 23)
        fakePeriodRecordDao.ongoing = PeriodRecordEntity(
            id = 1L,
            startEpochDay = today.toEpochDay(),
            endEpochDay = null,
            createdAt = 0L,
            updatedAt = 0L,
        )

        viewModel.load(1L, today)
        assertThat(viewModel.uiState.value.isPeriodContextAvailable).isTrue()

        viewModel.selectInitialChoice(InitialChoiceOption.PERIOD_FLOW)
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(DailyCheckInStep.Flow)
        // The seeded ongoing period is still there; no *new* period was created.
        assertThat(fakePeriodRecordDao.getAll()).hasSize(1)
    }

    // ───────────────────────── Fake Implementations ─────────────────────────

    /** In-memory DailyLogDao recording saved logs (no Room needed). */
    private class FakeDailyLogDao : DailyLogDao {
        val savedLogs = mutableListOf<DailyLogEntity>()
        private val state = MutableStateFlow<List<DailyLogEntity>>(emptyList())

        override fun observeAll(): Flow<List<DailyLogEntity>> = state
        override fun observeBetween(fromDay: Long, toDay: Long): Flow<List<DailyLogEntity>> =
            state.map { list -> list.filter { it.dateEpochDay in fromDay..toDay } }
        override suspend fun getAll(): List<DailyLogEntity> = savedLogs
        override suspend fun getById(id: Long): DailyLogEntity? =
            savedLogs.firstOrNull { it.id == id }
        override suspend fun getByDate(epochDay: Long): DailyLogEntity? =
            savedLogs.firstOrNull { it.dateEpochDay == epochDay }
        override suspend fun count(): Int = savedLogs.size
        override suspend fun upsert(log: DailyLogEntity): Long {
            val existing = savedLogs.firstOrNull { it.dateEpochDay == log.dateEpochDay }
            val saved = if (existing != null) {
                savedLogs.remove(existing)
                log.copy(id = existing.id)
            } else {
                log.copy(id = (savedLogs.maxOfOrNull { it.id } ?: 0L) + 1L)
            }
            savedLogs += saved
            state.value = savedLogs.sortedBy { it.dateEpochDay }
            return saved.id
        }
        override suspend fun deleteById(id: Long) {
            savedLogs.removeAll { it.id == id }
            state.value = savedLogs
        }
        override suspend fun deleteAll() {
            savedLogs.clear()
            state.value = emptyList()
        }
    }

    /** In-memory SymptomLogDao recording saved symptoms. */
    private class FakeSymptomLogDao : SymptomLogDao {
        val savedSymptoms = mutableListOf<SymptomLogEntity>()

        override fun observeForLog(dailyLogId: Long): Flow<List<SymptomLogEntity>> =
            MutableStateFlow(savedSymptoms.filter { it.dailyLogId == dailyLogId })
        override fun observeAll(): Flow<List<SymptomLogEntity>> = MutableStateFlow(savedSymptoms)
        override suspend fun getForLog(dailyLogId: Long): List<SymptomLogEntity> =
            savedSymptoms.filter { it.dailyLogId == dailyLogId }
        override suspend fun getFrequencyByType(): List<SymptomLogDao.SymptomFrequency> = emptyList()
        override suspend fun upsert(symptom: SymptomLogEntity): Long {
            savedSymptoms.removeAll { it.dailyLogId == symptom.dailyLogId && it.symptomType == symptom.symptomType }
            savedSymptoms += symptom
            return 1L
        }
        override suspend fun deleteForLogAndType(dailyLogId: Long, symptomType: String) {
            savedSymptoms.removeAll { it.dailyLogId == dailyLogId && it.symptomType.name == symptomType }
        }
        override suspend fun deleteForLog(dailyLogId: Long) {
            savedSymptoms.removeAll { it.dailyLogId == dailyLogId }
        }
        override suspend fun deleteAll() {
            savedSymptoms.clear()
        }
    }

    private class FakePeriodRecordDao : PeriodRecordDao {
        private val records = mutableMapOf<Long, PeriodRecordEntity>()
        private val state = MutableStateFlow<List<PeriodRecordEntity>>(emptyList())
        private var nextId = 1L

        var ongoing: PeriodRecordEntity?
            get() = records.values.firstOrNull { it.endEpochDay == null }
            set(value) {
                records.clear()
                if (value != null) {
                    records[value.id] = value
                    state.value = records.values.sortedBy { it.startEpochDay }
                } else {
                    state.value = emptyList()
                }
            }

        override fun observeAll(): Flow<List<PeriodRecordEntity>> = state

        override suspend fun getAll(): List<PeriodRecordEntity> =
            records.values.sortedBy { it.startEpochDay }

        override suspend fun getById(id: Long): PeriodRecordEntity? = records[id]

        override suspend fun getByStartDay(startEpochDay: Long): PeriodRecordEntity? =
            records.values.firstOrNull { it.startEpochDay == startEpochDay }

        override suspend fun getOngoing(): PeriodRecordEntity? =
            records.values.firstOrNull { it.endEpochDay == null }

        override suspend fun getActiveOn(epochDay: Long): PeriodRecordEntity? =
            records.values.firstOrNull {
                it.startEpochDay <= epochDay && (it.endEpochDay == null || it.endEpochDay >= epochDay)
            }

        override suspend fun count(): Int = records.size

        override suspend fun upsert(record: PeriodRecordEntity): Long {
            val id = if (record.id != 0L) record.id else nextId++
            val saved = record.copy(id = id)
            records[id] = saved
            state.value = records.values.sortedBy { it.startEpochDay }
            return id
        }

        override suspend fun deleteById(id: Long) {
            records.remove(id)
            state.value = records.values.sortedBy { it.startEpochDay }
        }

        override suspend fun deleteAll() {
            records.clear()
            state.value = emptyList()
        }
    }

    private class FakeCyclePredictionDao : id.rona.app.data.db.dao.CyclePredictionDao {
        override fun observeLatest(): Flow<id.rona.app.data.db.entity.CyclePredictionEntity?> =
            MutableStateFlow(null)
        override suspend fun getLatest(): id.rona.app.data.db.entity.CyclePredictionEntity? = null
        override suspend fun upsert(prediction: id.rona.app.data.db.entity.CyclePredictionEntity): Long = 1L
        override suspend fun trimTo(keep: Int) = Unit
        override suspend fun deleteAll() = Unit
    }

    private class FakeNlpSuggestionDao : NlpSuggestionDao {
        val list = mutableListOf<NlpSuggestionEntity>()
        override fun observeForLog(dailyLogId: Long): Flow<List<NlpSuggestionEntity>> = MutableStateFlow(list)
        override suspend fun getForLog(dailyLogId: Long): List<NlpSuggestionEntity> = list.filter { it.dailyLogId == dailyLogId }
        override suspend fun getByStatus(status: NlpStatus): List<NlpSuggestionEntity> = list.filter { it.status == status }
        override suspend fun upsert(suggestion: NlpSuggestionEntity): Long {
            list.add(suggestion)
            return 1L
        }
        override suspend fun updateStatus(id: Long, status: NlpStatus) = Unit
        override suspend fun deleteForLog(dailyLogId: Long) {
            list.removeAll { it.dailyLogId == dailyLogId }
        }
        override suspend fun deleteAll() {
            list.clear()
        }
    }

    private class FakeLocalNoteAnalyzer : LocalNoteAnalyzer {
        var stubResult = NoteAnalysisResult(
            normalizedText = "",
            symptomSuggestions = emptyList(),
            moodSuggestions = emptyList(),
            energySuggestion = null,
            flowSuggestion = null,
            dischargeSuggestion = null,
            durationSuggestion = null,
            pregnancyContext = null,
            detectedIntents = emptyList(),
            safetyAlerts = emptyList(),
            relevantKnowledgeCards = emptyList(),
            ruleVersion = "1.0",
        )
        override fun analyze(input: NoteAnalysisInput): NoteAnalysisResult = stubResult
    }
}

package id.rona.app.ui.log

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import id.rona.app.data.db.dao.NlpSuggestionDao
import id.rona.app.data.db.dao.PeriodRecordDao
import id.rona.app.data.db.dao.DailyLogDao
import id.rona.app.data.db.dao.SymptomLogDao
import id.rona.app.data.db.entity.NlpSuggestionEntity
import id.rona.app.data.db.entity.PeriodRecordEntity
import id.rona.app.data.db.entity.DailyLogEntity
import id.rona.app.data.db.entity.SymptomLogEntity
import id.rona.app.data.repository.DailyLogRepository
import id.rona.app.data.repository.PeriodRecordRepository
import id.rona.app.domain.model.Energy
import id.rona.app.domain.model.FlowLevel
import id.rona.app.domain.model.Mood
import id.rona.app.domain.model.NlpStatus
import id.rona.app.domain.model.Severity
import id.rona.app.domain.model.SymptomType
import id.rona.app.domain.nlp.LocalNoteAnalyzer
import id.rona.app.domain.nlp.NoteAnalysisInput
import id.rona.app.domain.nlp.NoteAnalysisResult
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

/**
 * Behaviour matrix for the guided-flow state machine after Phase A:
 * - no fabricated mood/energy on "Baik-baik saja"
 * - QuickEnergy is optional and continues to Summary
 * - PERIOD_FLOW requires explicit confirmation when no ongoing period exists
 * - Energy reached via the full path continues to Note (preserves existing flow)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GuidedCheckInFlowMatrixTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var dao: FakePeriodRecordDao
    private lateinit var periodRepository: PeriodRecordRepository
    private lateinit var viewModel: LogEditorViewModel

    @Before
    fun setUp() {
        dao = FakePeriodRecordDao()
        periodRepository = PeriodRecordRepository(
            transactionRunner = object : id.rona.app.data.db.TransactionRunner {
                override suspend fun <T> invoke(block: suspend () -> T): T = block()
            },
            periodRecordDao = dao,
            cyclePredictionDao = FakeCyclePredictionDao(),
        )
        viewModel = LogEditorViewModel(
            repository = DailyLogRepository(
                transactionRunner = object : id.rona.app.data.db.TransactionRunner {
                    override suspend fun <T> invoke(block: suspend () -> T): T = block()
                },
                dailyLogDao = FakeDailyLogDao(),
                symptomLogDao = FakeSymptomLogDao(),
            ),
            localNoteAnalyzer = FakeAnalyzer(),
            periodRecordDao = dao,
            periodRecordRepository = periodRepository,
            nlpSuggestionDao = FakeNlpSuggestionDao(),
        )
    }

    // ── "Baik-baik saja" fast path ────────────────────────────────────────────

    @Test
    fun `feeling good goes to QuickEnergy with no fabricated values`() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.selectInitialChoice(InitialChoiceOption.FEELING_GOOD)

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.currentStep).isEqualTo(DailyCheckInStep.QuickEnergy)
            assertThat(state.mood).isNull()
            assertThat(state.energy).isNull()
            assertThat(state.flow).isNull()
            assertThat(state.selectedSymptoms).isEmpty()
        }
    }

    @Test
    fun `feeling good then skip lands on Summary with all fields empty`() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.selectInitialChoice(InitialChoiceOption.FEELING_GOOD)
        viewModel.skipStep()

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.currentStep).isEqualTo(DailyCheckInStep.Summary)
            assertThat(state.mood).isNull()
            assertThat(state.energy).isNull()
            assertThat(state.flow).isNull()
            assertThat(state.selectedSymptoms).isEmpty()
        }
    }

    @Test
    fun `feeling good with explicit energy keeps only the chosen energy`() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.selectInitialChoice(InitialChoiceOption.FEELING_GOOD)
        viewModel.selectEnergy(Energy.LOW)
        viewModel.navigateToStep(DailyCheckInStep.Summary)

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.energy).isEqualTo(Energy.LOW)
            assertThat(state.mood).isNull()
            assertThat(state.flow).isNull()
        }
    }

    @Test
    fun `quick energy next goes straight to Summary`() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.selectInitialChoice(InitialChoiceOption.FEELING_GOOD)
        viewModel.nextStep()

        assertThat(viewModel.uiState.value.currentStep).isEqualTo(DailyCheckInStep.Summary)
    }

    // ── Period-flow branch ────────────────────────────────────────────────────

    @Test
    fun `period flow with no ongoing period goes to confirm`() = runTest(mainDispatcherRule.testDispatcher) {
        val today = LocalDate.of(2026, 8, 23)
        viewModel.load(today)
        viewModel.selectInitialChoice(InitialChoiceOption.PERIOD_FLOW)

        assertThat(viewModel.uiState.value.currentStep).isEqualTo(DailyCheckInStep.PeriodStartConfirm)
        assertThat(viewModel.uiState.value.isPeriodContextAvailable).isFalse()
    }

    @Test
    fun `confirm creates period and continues to Flow`() = runTest(mainDispatcherRule.testDispatcher) {
        val today = LocalDate.of(2026, 8, 23)
        viewModel.load(today)
        viewModel.selectInitialChoice(InitialChoiceOption.PERIOD_FLOW)
        viewModel.confirmPeriodStart()
        testScheduler.advanceUntilIdle()

        assertThat(periodRepository.getAllPeriods().single().startDate).isEqualTo(today)
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(DailyCheckInStep.Flow)
        assertThat(viewModel.uiState.value.isPeriodContextAvailable).isTrue()
    }

    @Test
    fun `declining period start creates nothing and returns to Initial`() = runTest(mainDispatcherRule.testDispatcher) {
        val today = LocalDate.of(2026, 8, 23)
        viewModel.load(today)
        viewModel.selectInitialChoice(InitialChoiceOption.PERIOD_FLOW)
        viewModel.cancelPeriodStart()

        assertThat(periodRepository.getAllPeriods()).isEmpty()
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(DailyCheckInStep.Initial)
    }

    @Test
    fun `period flow with active period skips confirmation`() = runTest(mainDispatcherRule.testDispatcher) {
        val today = LocalDate.of(2026, 8, 23)
        dao.ongoing = PeriodRecordEntity(
            id = 1L,
            startEpochDay = today.toEpochDay(),
            endEpochDay = null,
            createdAt = 0L,
            updatedAt = 0L,
        )
        viewModel.load(today)
        viewModel.selectInitialChoice(InitialChoiceOption.PERIOD_FLOW)

        assertThat(viewModel.uiState.value.currentStep).isEqualTo(DailyCheckInStep.Flow)
        assertThat(periodRepository.getAllPeriods()).hasSize(1)
    }

    // ── Full path preserves Energy -> Note (no QuickEnergy history) ───────────

    @Test
    fun `energy reached via full path continues to Note`() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.navigateToStep(DailyCheckInStep.Symptoms)
        viewModel.toggleSymptom(SymptomType.KRAM, Severity.MILD)
        viewModel.nextStep() // Symptoms -> Severity
        viewModel.nextStep() // Severity -> Energy
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(DailyCheckInStep.Energy)

        viewModel.nextStep() // Energy -> Note
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(DailyCheckInStep.Note)
    }

    @Test
    fun `energy reached via quick path continues to Summary`() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.selectInitialChoice(InitialChoiceOption.FEELING_GOOD)
        viewModel.selectEnergy(Energy.NEUTRAL)
        viewModel.nextStep() // QuickEnergy -> Summary

        assertThat(viewModel.uiState.value.currentStep).isEqualTo(DailyCheckInStep.Summary)
    }

    // ── Session reset ─────────────────────────────────────────────────────────

    @Test
    fun `load resets stale answers between sessions`() = runTest(mainDispatcherRule.testDispatcher) {
        val today = LocalDate.of(2026, 8, 23)
        viewModel.load(today)
        viewModel.selectFlow(FlowLevel.HEAVY)
        viewModel.toggleSymptom(SymptomType.NAUSEA, Severity.SEVERE)
        viewModel.setNote("isi lama")

        // Reopen same date: old session must not bleed through.
        viewModel.load(today)
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.currentStep).isEqualTo(DailyCheckInStep.Initial)
            assertThat(state.stepHistory).containsExactly(DailyCheckInStep.Initial)
            assertThat(state.flow).isNull()
            assertThat(state.selectedSymptoms).isEmpty()
            assertThat(state.note).isEmpty()
        }
    }

    // ── Fakes ─────────────────────────────────────────────────────────────────

    private class FakeDailyLogDao : DailyLogDao {
        private val logs = mutableMapOf<Long, DailyLogEntity>()
        private val state = MutableStateFlow<List<DailyLogEntity>>(emptyList())
        private var nextId = 1L

        override fun observeAll(): Flow<List<DailyLogEntity>> = state
        override fun observeBetween(fromDay: Long, toDay: Long): Flow<List<DailyLogEntity>> =
            state.map { list -> list.filter { it.dateEpochDay in fromDay..toDay } }
        override suspend fun getAll(): List<DailyLogEntity> = logs.values.sortedBy { it.dateEpochDay }
        override suspend fun getById(id: Long): DailyLogEntity? = logs[id]
        override suspend fun getByDate(epochDay: Long): DailyLogEntity? =
            logs.values.firstOrNull { it.dateEpochDay == epochDay }
        override suspend fun count(): Int = logs.size
        override suspend fun upsert(log: DailyLogEntity): Long {
            val id = if (log.id != 0L) log.id else nextId++
            val saved = log.copy(id = id)
            logs[id] = saved
            state.value = logs.values.sortedBy { it.dateEpochDay }
            return id
        }
        override suspend fun deleteById(id: Long) {
            logs.remove(id)
            state.value = logs.values.sortedBy { it.dateEpochDay }
        }
        override suspend fun deleteAll() {
            logs.clear()
            state.value = emptyList()
        }
    }

    private class FakeSymptomLogDao : SymptomLogDao {
        private val symptoms = mutableListOf<SymptomLogEntity>()

        override fun observeForLog(dailyLogId: Long): Flow<List<SymptomLogEntity>> =
            MutableStateFlow(symptoms.filter { it.dailyLogId == dailyLogId })
        override fun observeAll(): Flow<List<SymptomLogEntity>> = MutableStateFlow(symptoms)
        override suspend fun getForLog(dailyLogId: Long): List<SymptomLogEntity> =
            symptoms.filter { it.dailyLogId == dailyLogId }
        override suspend fun getFrequencyByType(): List<SymptomLogDao.SymptomFrequency> = emptyList()
        override suspend fun upsert(symptom: SymptomLogEntity): Long {
            symptoms.removeAll { it.dailyLogId == symptom.dailyLogId && it.symptomType == symptom.symptomType }
            symptoms += symptom
            return 1L
        }
        override suspend fun deleteForLogAndType(dailyLogId: Long, symptomType: String) {
            symptoms.removeAll { it.dailyLogId == dailyLogId && it.symptomType.name == symptomType }
        }
        override suspend fun deleteForLog(dailyLogId: Long) {
            symptoms.removeAll { it.dailyLogId == dailyLogId }
        }
        override suspend fun deleteAll() {
            symptoms.clear()
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
        override fun observeForLog(dailyLogId: Long): Flow<List<NlpSuggestionEntity>> = MutableStateFlow(emptyList())
        override suspend fun getForLog(dailyLogId: Long): List<NlpSuggestionEntity> = emptyList()
        override suspend fun getByStatus(status: NlpStatus): List<NlpSuggestionEntity> = emptyList()
        override suspend fun upsert(suggestion: NlpSuggestionEntity): Long = 1L
        override suspend fun updateStatus(id: Long, status: NlpStatus) = Unit
        override suspend fun deleteForLog(dailyLogId: Long) = Unit
        override suspend fun deleteAll() = Unit
    }

    private class FakeAnalyzer : LocalNoteAnalyzer {
        override fun analyze(input: NoteAnalysisInput): NoteAnalysisResult = NoteAnalysisResult(
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
    }
}

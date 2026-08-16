package id.rona.app.data.db

import androidx.room.TypeConverter
import id.rona.app.domain.model.Confidence
import id.rona.app.domain.model.Energy
import id.rona.app.domain.model.FlowLevel
import id.rona.app.domain.model.Mood
import id.rona.app.domain.model.NlpStatus
import id.rona.app.domain.model.PrivacyMode
import id.rona.app.domain.model.Severity
import id.rona.app.domain.model.SymptomType
import id.rona.app.domain.model.ThemeMode

class Converters {
    @TypeConverter fun flowToString(value: FlowLevel): String = value.name
    @TypeConverter fun stringToFlow(value: String): FlowLevel = FlowLevel.valueOf(value)

    @TypeConverter fun symptomToString(value: SymptomType): String = value.name
    @TypeConverter fun stringToSymptom(value: String): SymptomType = SymptomType.valueOf(value)

    @TypeConverter fun severityToString(value: Severity): String = value.name
    @TypeConverter fun stringToSeverity(value: String): Severity = Severity.valueOf(value)

    @TypeConverter fun moodToString(value: Mood): String = value.name
    @TypeConverter fun stringToMood(value: String): Mood = Mood.valueOf(value)

    @TypeConverter fun energyToString(value: Energy): String = value.name
    @TypeConverter fun stringToEnergy(value: String): Energy = Energy.valueOf(value)

    @TypeConverter fun confidenceToString(value: Confidence): String = value.name
    @TypeConverter fun stringToConfidence(value: String): Confidence = Confidence.valueOf(value)

    @TypeConverter fun privacyToString(value: PrivacyMode): String = value.name
    @TypeConverter fun stringToPrivacy(value: String): PrivacyMode = PrivacyMode.valueOf(value)

    @TypeConverter fun themeToString(value: ThemeMode): String = value.name
    @TypeConverter fun stringToTheme(value: String): ThemeMode = ThemeMode.valueOf(value)

    @TypeConverter fun nlpStatusToString(value: NlpStatus): String = value.name
    @TypeConverter fun stringToNlpStatus(value: String): NlpStatus = NlpStatus.valueOf(value)
}

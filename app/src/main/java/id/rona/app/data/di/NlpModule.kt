package id.rona.app.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import id.rona.app.domain.nlp.KnowledgeCardRepository
import id.rona.app.domain.nlp.LocalNoteAnalyzer
import id.rona.app.domain.nlp.SafetyTriageEngine
import id.rona.app.domain.nlp.engine.RuleBasedNoteAnalyzer
import id.rona.app.domain.nlp.knowledge.LocalKnowledgeCardRepository
import id.rona.app.domain.nlp.safety.SafetyTriageEngineImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NlpModule {

    @Binds
    @Singleton
    abstract fun bindLocalNoteAnalyzer(impl: RuleBasedNoteAnalyzer): LocalNoteAnalyzer

    @Binds
    @Singleton
    abstract fun bindKnowledgeCardRepository(impl: LocalKnowledgeCardRepository): KnowledgeCardRepository

    @Binds
    @Singleton
    abstract fun bindSafetyTriageEngine(impl: SafetyTriageEngineImpl): SafetyTriageEngine
}

package br.ufrn.noscroll.architecture

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import kotlin.test.Test
import kotlin.test.assertTrue

class ArchitectureTest {

    private val classes: JavaClasses = ClassFileImporter()
        .withImportOption(ImportOption.DoNotIncludeTests())
        .importPackages("br.ufrn.noscroll")

    @Test
    fun domainDoesNotDependOnFrameworksOrInfrastructure() = domainRule("br.ufrn.noscroll.domain..").check(classes)

    @Test
    fun domainDoesNotDependOnAdapters() =
        noClasses().that().resideInAPackage("br.ufrn.noscroll.domain..")
            .should().dependOnClassesThat().resideInAPackage("br.ufrn.noscroll.adapters..")
            .check(classes)

    @Test
    fun domainRuleCatchesAViolation() {
        val fixture = ClassFileImporter().importPackages("br.ufrn.noscroll.architecture.violation")
        val result = domainRule("br.ufrn.noscroll.architecture.violation.domain..").evaluate(fixture)
        assertTrue(result.hasViolation(), "a regra do domínio deveria acusar o import de Ktor")
    }

    companion object {
        fun domainRule(domainPackage: String): ArchRule =
            noClasses().that().resideInAPackage(domainPackage)
                .should().dependOnClassesThat().resideInAnyPackage(
                    "io.ktor..", "org.koin..", "org.jetbrains.exposed..", "java.sql..", "javax.sql..",
                    "com.zaxxer..", "org.flywaydb..", "org.postgresql..",
                )
                .because("o domínio não sabe de HTTP, banco nem injeção de dependência")
    }
}

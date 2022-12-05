package dev.umerov.kspcomplier

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import kotlinx.serialization.SerialName


class KspProcessor(private val env: SymbolProcessorEnvironment) : SymbolProcessor {
    private fun checkAnnotatedClass(list: Sequence<KSAnnotation>, className: String): Boolean {
        for (i in list) {
            if (i.shortName.getShortName() == className) {
                return true
            }
        }
        return false
    }

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        /*insert {
        plugins {
            id("com.android.application")
            id("kotlin-android")
            id("kotlinx-serialization")
            id 'com.google.devtools.ksp' version "1.7.0-1.0.6"
        }
        ksp project(':kspcomplier')
        }*/
        val out = ArrayList<String>()
        val symbols = resolver.getSymbolsWithAnnotation(SerialName::class.java.name)
        for (i in symbols) {
            if (i.parent is KSClassDeclaration) {
                if (!checkAnnotatedClass(
                        (i.parent as KSClassDeclaration).annotations,
                        kotlinx.serialization.Serializable::class.java.simpleName
                    )
                ) {
                    if (!out.contains((i.parent as KSClassDeclaration).toString())) {
                        out.add((i.parent as KSClassDeclaration).toString())
                    }
                }
            }
        }
        for (i in out) {
            env.logger.warn("SerialNameChecker Found: $i")
        }
        for (i in resolver.getDeclarationsFromPackage("dev.ragnarok.fenrir.api.model")) {
            if (!checkAnnotatedClass(
                    i.annotations,
                    kotlinx.serialization.Serializable::class.java.simpleName
                )
            ) {
                env.logger.warn("ModelChecker Found: $i")
            }
        }
        return emptyList()
    }

    override fun finish() {
        env.logger.info("finish\r\n")
    }
}

class KspProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment) = KspProcessor(environment)
}
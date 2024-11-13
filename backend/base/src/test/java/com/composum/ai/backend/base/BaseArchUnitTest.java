package com.composum.ai.backend.base;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import org.junit.runner.RunWith;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitRunner;
import com.tngtech.archunit.lang.ArchRule;


/**
 * We try to cover some architecture failures that were there - package dependendies that should not be there.
 *
 * @see "https://www.archunit.org/userguide/html/000_Index.html"
 */
@RunWith(ArchUnitRunner.class)
@AnalyzeClasses(packages = "com.composum.ai.backend.base")
public class BaseArchUnitTest {

    /**
     * Classes from API packages should not import anything from the ..impl.. packages.
     */
    @ArchTest
    public static final ArchRule noimportImpl = classes()
            .that().resideInAPackage("com.composum.ai.backend.base.service.chat")
            .should().onlyDependOnClassesThat().resideOutsideOfPackages("com.composum.ai..impl..");

}

/** MODELED-Java

Copyright 2024 Stefan Zimmermann <user@zimmermann.co>

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package me.modeled.processor;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

import lombok.NonNull;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import one.util.streamex.StreamEx;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.helper.StringHelpers;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;

import me.modeled.Modeled;


/** The processor for {@link Modeled} class annotations.

    <p> It uses {@link AutoService} to register itself as a service provider for the Java compiler.
*/
@AutoService(Processor.class)
@SupportedAnnotationTypes({"me.modeled.Modeled"})
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class ModeledProcessor extends AbstractModeledProcessor {

    /** The Handlebars instance used for template rendering.
    */
    @NonNull
    private static final Handlebars HANDLEBARS = new Handlebars()
            .with(new ClassPathTemplateLoader("/templates/" + ModeledProcessor.class.getPackageName()))
            .registerHelpers(StringHelpers.class);

    /** Processes the given annotations, i.e. {@link Modeled}, as requested by the given round environment.

        @param annotations the annotation interfaces requested to be processed
        @param roundEnv environment for information about the current and prior round

        @return {@code true} if the process completed successfully, {@code false} otherwise
    */
    @Override
    public boolean process(@NonNull final Set<? extends TypeElement> annotations, @NonNull final RoundEnvironment roundEnv) {
        for (@NonNull final var annotatedElement : StreamEx.of(annotations).map(roundEnv::getElementsAnnotatedWith)
                .flatMap(Collection::stream)) {

            if (annotatedElement.getKind() != ElementKind.CLASS) {
                throw new IllegalArgumentException(String.format("Annotated element passed to %s via %s is not a class: %s",
                        this.getClass(), roundEnv, annotatedElement));
            }

            @NonNull final var annotatedClass = (TypeElement) annotatedElement;

            @NonNull final var className = ClassName.get(annotatedClass);
            @NonNull final var packageName = className.packageName();
            @NonNull final var simpleName = className.simpleName();

            @NonNull final var modelClassName = ClassName.get(packageName, simpleName + "_Model");
            super.printNote("Generating sealed interface %s", modelClassName);

            try (@NonNull final var writer = super.processingEnv.getFiler()
                    .createSourceFile(modelClassName.canonicalName(), annotatedClass)
                    .openWriter()) {

                HANDLEBARS.compile("_Model.java").apply(Map.of("class", simpleName, "package", packageName, "properties", super
                        .filterElementsByAnnotation(super.streamEnclosedFieldElementsOf(annotatedClass), Modeled.Property.class)
                        .map(property -> {
                            @NonNull final var propertyAnnotation = property.getAnnotation(Modeled.Property.class);

                            final boolean isAtomicReference = super.elementHasType(property, AtomicReference.class);
                            final boolean isCollection = super.elementHasCollectionType(property);

                            return Map.of(
                                    "name", property.getSimpleName(),

                                    "isAtomicReference", isAtomicReference,
                                    "isCollection", isCollection,
                                    "isFinal", super.elementIsFinal(property),
                                    "isImmutable", propertyAnnotation.immutable(),

                                    "isNonNull", StreamEx
                                            .of(javax.annotation.Nonnull.class, NonNull.class,
                                                    org.checkerframework.checker.nullness.qual.NonNull.class)

                                            .findAny(annotationClass -> property.getAnnotation(annotationClass) != null)
                                            .isPresent(),

                                    "type", TypeName.get((isAtomicReference || isCollection)
                                            ? super.streamElementTypeArgumentsOf(property).findFirst().orElseThrow(
                                                    () -> new IllegalStateException(String
                                                            .format("%s should have generic type but %s has no type attribute",
                                                                    property, property.asType())))

                                            : property.asType()));

                        }).toImmutableList()), writer);

            } catch (final IOException e) {
                super.printError("Failed creating source file for generated interface %s", modelClassName);
                throw new RuntimeException(String.format("Failed processing annotation %s", Modeled.class), e);
            }
        }

        return true;
    }
}

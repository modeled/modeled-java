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

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Optional;

import javax.annotation.processing.AbstractProcessor;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import javax.tools.Diagnostic;

import lombok.NonNull;

import one.util.streamex.StreamEx;

import me.modeled.Modeled;


/** Abstract class with helper methods for processors of {@link Modeled} and other annotations.
*/
public abstract class AbstractModeledProcessor extends AbstractProcessor {

    /** Filters the elements from the given iterable based on the presence of the given annotation.

        @param elements the iterable containing the elements to be filtered
        @param annotation the annotation class used for filtering

        @param <E> the type of the elements in the iterable
        @param <A> the type of the annotation

        @return a streamex of elements that have the specified annotation
    */
    protected <E extends Element, A extends Annotation>
    StreamEx<E> filterElementsByAnnotation(@NonNull final Iterable<E> elements, @NonNull final Class<A> annotation) {
        return StreamEx.of(elements.iterator()).filter(element -> element.getAnnotation(annotation) != null);
    }

    /** Retrieves the annotation processing environment's utilities for dealing with code elements.

        @return the utilities instance
    */
    protected Elements elementUtils() {
        return super.processingEnv.getElementUtils();
    }

    /** Searches for a class or interface code element by name.

        @param name the qualified name of the type element to find

        @return an optional, either containing the type element instance or empty if not found
    */
    protected Optional<TypeElement> findTypeElement(@NonNull final String name) {
        return Optional.ofNullable(this.elementUtils().getTypeElement(name));
    }

    /** Retrieves a class or interface code element by name, throwing an exception if it can't be found.

        @param name the qualified name of the type element to find

        @return the type element instance

        @throws NoSuchElementException if the type could not be found
    */
    protected TypeElement requireTypeElement(@NonNull final String name) {
        return this.findTypeElement(name).orElseThrow(() -> new NoSuchElementException(String
                .format("No type element with name: %s", name)));
    }

    /** Creates a stream of the given type element's enclosed elements of {@link ElementKind#FIELD}.

        @param typeElement the element whose enclosed field elements are to be streamed

        @return a streamex of all enclosed field element instances
    */
    protected StreamEx<VariableElement> streamEnclosedFieldElementsOf(@NonNull final TypeElement typeElement) {
        return StreamEx.of(typeElement.getEnclosedElements()).filter(element -> element.getKind() == ElementKind.FIELD)
                .map(VariableElement.class::cast);
    }

    /** Checks if the given element has all the given modifiers.

        @param element the element to check for modifiers
        @param modifiers the modifiers to check for

        @return {@code true} if the element has all the specified modifiers, {@code false} otherwise
    */
    protected boolean elementHasModifiers(@NonNull final Element element, @NonNull final Modifier... modifiers) {
        return element.getModifiers().containsAll(StreamEx.of(modifiers).toImmutableList());
    }

    /** Checks if given code element is declared as public.

        @param element the element to check

        @return {@code true} if the element is public, {@code false} otherwise
     */
    protected boolean elementIsPublic(@NonNull final Element element) {
        return element.getModifiers().contains(Modifier.PUBLIC);
    }

    /** Checks if given code element is declared as protected.

        @param element the element to check

        @return {@code true} if the element is protected, {@code false} otherwise
    */
    protected boolean elementIsProtected(@NonNull final Element element) {
        return element.getModifiers().contains(Modifier.PROTECTED);
    }

    /** Checks if given code element is declared as private.

        @param element the element to check

        @return {@code true} if the element is private, {@code false} otherwise
    */
    protected boolean elementIsPrivate(@NonNull final Element element) {
        return element.getModifiers().contains(Modifier.PRIVATE);
    }

    /** Checks if given code element is declared as final.

        @param element the element to check

        @return {@code true} if the element is final, {@code false} otherwise
    */
    protected boolean elementIsFinal(@NonNull final Element element) {
        return element.getModifiers().contains(Modifier.FINAL);
    }

    /** Retrieves the annotation processing environment's utilities for dealing with type declarations.

        @return the utilities instance
    */
    protected Types typeUtils() {
        return super.processingEnv.getTypeUtils();
    }

    /** Checks if the given type is implementing the given interface type.

        @param type the type to check
        @param implementedType the interface type to check against

        @return {@code true} if the given type is implementing the interface, {@code false} otherwise
    */
    protected boolean typeIsImplementing(@NonNull final TypeMirror type, @NonNull final TypeMirror implementedType) {
        if (type instanceof DeclaredType declaredType) {
            for (@NonNull final var interfaceType : this.typeElementOf(declaredType).getInterfaces()) {

                if (this.typeUtils().isSameType(interfaceType, implementedType)
                        || interfaceType instanceof DeclaredType declaredInterfaceType && (
                                this.typeUtils().isSameType(declaredInterfaceType.asElement().asType(), implementedType)
                                        || this.typeIsImplementing(interfaceType, implementedType))) {

                    return true;
                }
            }
        }

        return false;
    }

    /** Checks if the given type is implementing the given interface type.

        @param type the type to check
        @param implementedType the interface type to check against

        @return {@code true} if the given type is implementing the interface, {@code false} otherwise
    */
    protected boolean typeIsImplementing(@NonNull final TypeMirror type, @NonNull final Class<?> implementedType) {
        return this.typeIsImplementing(type, this.typeOf(implementedType));
    }

    /** Checks if the type of the given element is implementing the given interface type.

        @param element the element whose type is to be checked
        @param implementedType the interface type to check against

        @return {@code true} if the element's type is implementing the interface, {@code false} otherwise
    */
    protected boolean elementTypeIsImplementing(@NonNull final Element element, @NonNull final TypeMirror implementedType) {
        return this.typeIsImplementing(element.asType(), implementedType);
    }

    /** Checks if the type of the given element is implementing the given interface type.

        @param element the element whose type is to be checked.
        @param implementedType the interface type to check against.

        @return {@code true} if the element's type is implementing the interface, {@code false} otherwise.
    */
    protected boolean elementTypeIsImplementing(@NonNull final Element element, @NonNull final Class<?> implementedType) {
        return this.elementTypeIsImplementing(element, this.typeOf(implementedType));
    }

    /** Checks if the given element has the given type.

        @param element the element to check
        @param type the type to check against

        @return {@code true} if the element has the given type, {@code false} otherwise
    */
    protected boolean elementHasType(@NonNull final Element element, @NonNull final TypeMirror type) {
        return this.typeUtils().isSameType(element.asType(), type);
    }

    /** Checks if the given element has the given type.

        @param element the element to check
        @param type the type to check against

        @return {@code true} if the element has the given type, {@code false} otherwise
    */
    protected boolean elementHasType(@NonNull final Element element, @NonNull final Class<?> type) {
        return this.elementHasType(element, this.typeOf(type));
    }

    /** Checks if the given element has a type that implements {@link Collection}.

        @param element the element to check

        @return {@code true} if the element has a collection type, {@code false} otherwise
    */
    protected boolean elementHasCollectionType(@NonNull final Element element) {
        return this.elementTypeIsImplementing(element, Collection.class);
    }

    /** Retrieves the type element corresponding to the given class.

        @param type the class for which to get the corresponding element

        @return the type element instance
    */
    protected TypeElement typeElementOf(@NonNull final Class<?> type) {
        return this.requireTypeElement(type.getName());
    }

    /** Retrieves the type element corresponding to the given type.

        @param type the type for which to get the corresponding element

        @return the type element instance
    */
    protected TypeElement typeElementOf(@NonNull final DeclaredType type) {
        return (TypeElement) this.typeUtils().asElement(type);
    }

    /** Retrieves the type corresponding to the given class.

        @param type the class for which to retrieve the type

        @return the type corresponding to the given class
    */
    protected TypeMirror typeOf(@NonNull final Class<?> type) {
        return this.typeElementOf(type).asType();
    }

    /** Retrieves the type corresponding to the given element and, in case of a generic type reference, its arguments.

        @param element the type element referring to the type
        @param args the type mirrors referring to the arguments of a parametrized type

        @return the type declaration

        @throws IllegalArgumentException if inapplicable type arguments are given
    */
    protected DeclaredType declaredTypeFrom(@NonNull final TypeElement element, @NonNull final TypeMirror... args) {
        return this.typeUtils().getDeclaredType(element, args);
    }

    /** Retrieves the type corresponding to the given class and, in case of a generic class, its type arguments.

        @param type the class representing the desired type
        @param args the types representing the type arguments

        @return the type declaration corresponding to the class and type arguments
    */
    protected DeclaredType declaredTypeFrom(@NonNull final Class<?> type, @NonNull final TypeMirror... args) {
        return this.declaredTypeFrom(this.typeElementOf(type), args);
    }

    /** Retrieves the type corresponding to the given name and, in case of a generic type's name, its type arguments.

        @param name the name of the type
        @param args the types referring to the arguments of a generic type

        @return the type declaration

        @throws IllegalArgumentException if inapplicable type arguments are given
        @throws NoSuchElementException if a type element of given name cannot be found
    */
    protected DeclaredType declaredTypeFrom(@NonNull final String name, @NonNull final TypeMirror... args) {
        return this.declaredTypeFrom(this.requireTypeElement(name), args);
    }

    /** Streams the type arguments of the given type, returning an empty stream in case of a non-declared or non-generic type.

        @param type the type delaration from which to retrieve its type arguments

        @return a streamex of type arguments, or an empty stream if the type is not a declared type or not generic
    */
    protected StreamEx<TypeMirror> streamTypeArgumentsOf(@NonNull final TypeMirror type) {
        return type instanceof DeclaredType declaredType ? StreamEx.of(declaredType.getTypeArguments()) : StreamEx.empty();
    }

    /** Streams the type arguments of the given type element, returning an empty stream for non-generic types.

        @param element the type element to retrieve the type arguments from

        @return a streamex of type arguments, or an empty stream if the type element represents a non-generic type
    */
    protected StreamEx<TypeMirror> streamTypeArgumentsOf(@NonNull final TypeElement element) {
        return this.streamTypeArgumentsOf(element.asType());
    }

    /** Streams the type arguments of the given class, returning an empty stream for non-generic types.

        @param type the class to retrieve the type arguments from

        @return a streamex of type argument, or an empty stream if the class is non-generic
    */
    protected StreamEx<TypeMirror> streamTypeArgumentsOf(@NonNull final Class<?> type) {
        return this.streamTypeArgumentsOf(this.typeOf(type));
    }

    /** Streams the type arguments of the given element's type, returning an empty stream for non-declared or non-generic types.

        @param element the element whose type to get the type arguments from

        @return A stream of the type arguments, or an empty stream if the element has a non-declared or non-generic type
    */
    protected StreamEx<TypeMirror> streamElementTypeArgumentsOf(@NonNull final Element element) {
        return this.streamTypeArgumentsOf(element.asType());
    }

    /** Prints a message of {@link Diagnostic.Kind#ERROR} through the annotation processing environment.

        @param format the format string for the error message
        @param args the arguments to be formatted into the error message
    */
    protected void printError(@NonNull final String format, @NonNull final Object... args) {
        super.processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, String.format(format, args));
    }

    /** Prints a message of {@link Diagnostic.Kind#WARNING} through the annotation processing environment.

        @param format the format string for the warning message
        @param args the arguments to be formatted into the warning message
    */
    protected void printWarning(@NonNull final String format, @NonNull final Object... args) {
        super.processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, String.format(format, args));
    }

    /** Prints a message of {@link Diagnostic.Kind#MANDATORY_WARNING} through the annotation processing environment.

        @param format the format string for the mandatory warning
        @param args the arguments to be formatted into the mandatory warning
    */
    protected void printMandatoryWarning(@NonNull final String format, @NonNull final Object... args) {
        super.processingEnv.getMessager().printMessage(Diagnostic.Kind.MANDATORY_WARNING, String.format(format, args));
    }

    /** Prints a message of {@link Diagnostic.Kind#NOTE} through the annotation processing environment.

        @param format the format string for the note
        @param args the arguments to be formatted into the note
    */
    protected void printNote(@NonNull final String format, @NonNull final Object... args) {
        super.processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, String.format(format, args));
    }

    /** Prints a message of {@link Diagnostic.Kind#OTHER} through the annotation processing environment.

        @param format the format string for the message
        @param args the arguments to be formatted into the message
    */
    protected void printOther(@NonNull final String format, @NonNull final Object... args) {
        super.processingEnv.getMessager().printMessage(Diagnostic.Kind.OTHER, String.format(format, args));
    }
}

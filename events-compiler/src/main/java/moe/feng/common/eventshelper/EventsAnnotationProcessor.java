package moe.feng.common.eventshelper;

import androidx.annotation.RestrictTo;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("moe.feng.common.eventshelper.EventsListener")
@AutoService(Processor.class)
public class EventsAnnotationProcessor extends AbstractProcessor {

    private static boolean isValidEventsListenerElement(Element e) {
        ElementKind kind = e.getKind();
        return kind == ElementKind.INTERFACE ||
                (kind == ElementKind.CLASS && e.getModifiers().contains(Modifier.ABSTRACT));
    }

    static class ClassNames {

        static final ClassName EventsHelper = ClassName.get(
                "moe.feng.common.eventshelper", "EventsHelper");

    }

    private Filer filer;
    private Messager messager;
    private Elements elements;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        filer = processingEnvironment.getFiler();
        messager = processingEnvironment.getMessager();
        elements = processingEnvironment.getElementUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment env) {
        Collection<? extends Element> elements = env.getElementsAnnotatedWith(EventsListener.class);
        for (Element element : elements) {
            if (isValidEventsListenerElement(element)) {
                processEventsListener((TypeElement) element);
            } else {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "Element: " + element + " is not a valid EventsListener.");
            }
        }

        return true;
    }

    private void processEventsListener(TypeElement e) {
        String listenerClassName = e.getQualifiedName().toString();
        TypeName listenerClassTypeName = TypeName.get(e.asType());

        String helperClassName = "Helper$$" + listenerClassName.replace(".", "_");

        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(helperClassName)
                .addModifiers(Modifier.FINAL)
                .addSuperinterface(listenerClassTypeName)
                .addAnnotation(AnnotationSpec.builder(RestrictTo.class)
                        .addMember("value", "$L", "RestrictTo.Scope.LIBRARY_GROUP")
                        .build())
                .addField(String.class, "mTag", Modifier.PRIVATE)
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(String.class, "tag")
                        .addStatement("this.$N = $N", "mTag", "tag")
                        .build());

        for (Element enclosedElement : e.getEnclosedElements()) {
            if (enclosedElement.getKind() == ElementKind.METHOD) {
                ExecutableElement element = (ExecutableElement) enclosedElement;

                if (element.getReturnType().getKind() != TypeKind.VOID) {
                    messager.printMessage(Diagnostic.Kind.ERROR,
                            "Method " + element + " in " + listenerClassName
                                    + " class doesn't return void type.");
                }

                TypeName listOfListeners = ParameterizedTypeName.get(
                        ClassName.get(List.class), listenerClassTypeName);

                EventsOnThread threadAnnotation = element.getAnnotation(EventsOnThread.class);
                int threadType = EventsOnThread.CURRENT_THREAD;
                if (threadAnnotation != null) {
                    threadType = threadAnnotation.value();
                }

                StringBuilder invokeStatement = new StringBuilder();
                invokeStatement.append("EventsHelper.scheduleRunnable(() -> listener.");
                invokeStatement.append(element.getSimpleName()).append("(");
                List<? extends VariableElement> parameters = element.getParameters();
                if (parameters != null && !parameters.isEmpty()) {
                    for (VariableElement variableElement : element.getParameters()) {
                        invokeStatement.append(variableElement.getSimpleName()).append(", ");
                    }
                    invokeStatement.setLength(invokeStatement.length() - 2);
                }
                invokeStatement.append("), ").append(threadType).append(")");

                classBuilder.addMethod(MethodSpec.overriding(element)
                        .addStatement("$T listeners = $T.getListenersByClass($T.class, $N)",
                                listOfListeners, ClassNames.EventsHelper, listenerClassTypeName, "mTag")
                        .beginControlFlow("for ($T listener : listeners)", listenerClassTypeName)
                        .addStatement(invokeStatement.toString())
                        .endControlFlow()
                        .build());
            }
        }

        try {
            JavaFile.builder("moe.feng.common.eventshelper", classBuilder.build())
                    .build()
                    .writeTo(filer);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

}

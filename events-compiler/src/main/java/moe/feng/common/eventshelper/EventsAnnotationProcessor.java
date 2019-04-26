package moe.feng.common.eventshelper;

import com.google.auto.service.AutoService;
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
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
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

        static final ClassName List = ClassName.get("java.util", "List");

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
        String listenerName = e.getQualifiedName().toString();
        TypeName listenerClassTypeName = TypeName.get(e.asType());

        Name packageName = elements.getPackageOf(e).getQualifiedName();

        String helperClassName = e.getSimpleName().toString() + "$$Helper";

        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(helperClassName)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(listenerClassTypeName)
                .addField(EventsListenerProvider.class, "mListenerProvider", Modifier.PRIVATE)
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(EventsListenerProvider.class, "listenerProvider")
                        .addStatement("this.$N = $N", "mListenerProvider", "listenerProvider")
                        .build());

        for (Element enclosedElement : e.getEnclosedElements()) {
            if (enclosedElement.getKind() == ElementKind.METHOD) {
                ExecutableElement element = (ExecutableElement) enclosedElement;

                TypeName listOfListeners = ParameterizedTypeName.get(
                        ClassNames.List, listenerClassTypeName);

                StringBuilder invokeStatement = new StringBuilder("listener.");
                invokeStatement.append(element.getSimpleName()).append("(");
                List<? extends VariableElement> parameters = element.getParameters();
                if (parameters != null && !parameters.isEmpty()) {
                    for (VariableElement variableElement : element.getParameters()) {
                        invokeStatement.append(variableElement.getSimpleName()).append(", ");
                    }
                    invokeStatement.setLength(invokeStatement.length() - 2);
                }
                invokeStatement.append(")");

                classBuilder.addMethod(MethodSpec.overriding(element)
                        .addStatement("$T listeners = $N.getListenersByClass($T.class)",
                                listOfListeners, "mListenerProvider", listenerClassTypeName)
                        .beginControlFlow("for ($T listener : listeners)", listenerClassTypeName)
                        .addStatement(invokeStatement.toString())
                        .endControlFlow()
                        .build());
            }
        }

        try {
            JavaFile.builder(packageName.toString(), classBuilder.build())
                    .build()
                    .writeTo(filer);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

}

package io.swagger.v3.springmvc.util;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.core.util.ParameterProcessor;
import io.swagger.v3.core.util.ReflectionUtils;
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.springmvc.ext.OpenAPIExtension;
import io.swagger.v3.springmvc.ext.OpenAPIExtensions;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class ReaderUtils {
    private static final String GET_METHOD = "get";
    private static final String POST_METHOD = "post";
    private static final String PUT_METHOD = "put";
    private static final String DELETE_METHOD = "delete";
    private static final String HEAD_METHOD = "head";
    private static final String OPTIONS_METHOD = "options";
    private static final String PATH_DELIMITER = "/";

    /**
     * Collects constructor-level parameters from class.
     *
     * @param cls        is a class for collecting
     * @param components
     * @return the collection of supported parameters
     */
    public static List<Parameter> collectConstructorParameters(Class<?> cls, Components components, String[] classConsumes, JsonView jsonViewAnnotation) {
        if (cls.isLocalClass() || (cls.isMemberClass() && !Modifier.isStatic(cls.getModifiers()))) {
            return Collections.emptyList();
        }

        List<Parameter> selected = Collections.emptyList();
        int maxParamsCount = 0;

        for (Constructor<?> constructor : cls.getDeclaredConstructors()) {
            if (!ReflectionUtils.isConstructorCompatible(constructor)
                    && !ReflectionUtils.isInject(Arrays.asList(constructor.getDeclaredAnnotations()))) {
                continue;
            }

            final Type[] genericParameterTypes = constructor.getGenericParameterTypes();
            final Annotation[][] annotations = constructor.getParameterAnnotations();

            int paramsCount = 0;
            final List<Parameter> parameters = new ArrayList<Parameter>();
            for (int i = 0; i < genericParameterTypes.length; i++) {
                final List<Annotation> tmpAnnotations = Arrays.asList(annotations[i]);
                final Type genericParameterType = genericParameterTypes[i];
                final List<Parameter> tmpParameters = collectParameters(genericParameterType, tmpAnnotations, components, classConsumes, jsonViewAnnotation);
                if (tmpParameters.size() >= 1) {
                    for (Parameter tmpParameter : tmpParameters) {
                        Parameter processedParameter = ParameterProcessor.applyAnnotations(
                                tmpParameter,
                                genericParameterType,
                                tmpAnnotations,
                                components,
                                classConsumes == null ? new String[0] : classConsumes,
                                null,
                                jsonViewAnnotation);
                        if (processedParameter != null) {
                            parameters.add(processedParameter);
                        }
                    }
                    paramsCount++;
                }
            }

            if (paramsCount >= maxParamsCount) {
                maxParamsCount = paramsCount;
                selected = parameters;
            }
        }

        return selected;
    }

    /**
     * Collects field-level parameters from class.
     *
     * @param cls        is a class for collecting
     * @param components
     * @return the collection of supported parameters
     */
    public static List<Parameter> collectFieldParameters(Class<?> cls, Components components, String[] classConsumes, JsonView jsonViewAnnotation) {
        final List<Parameter> parameters = new ArrayList<Parameter>();
        for (Field field : ReflectionUtils.getDeclaredFields(cls)) {
            final List<Annotation> annotations = Arrays.asList(field.getAnnotations());
            final Type genericType = field.getGenericType();
            parameters.addAll(collectParameters(genericType, annotations, components, classConsumes, jsonViewAnnotation));
        }
        return parameters;
    }

    private static List<Parameter> collectParameters(Type type, List<Annotation> annotations, Components components, String[] classConsumes, JsonView jsonViewAnnotation) {
        final Iterator<OpenAPIExtension> chain = OpenAPIExtensions.chain();
        return chain.hasNext() ? chain.next().extractParameters(annotations, type, new HashSet<>(), components, classConsumes, null, false, jsonViewAnnotation, chain).parameters :
                Collections.emptyList();
    }

    public static Optional<List<String>> getStringListFromStringArray(String[] array) {
        if (array == null) {
            return Optional.empty();
        }
        List<String> list = new ArrayList<>();
        boolean isEmpty = true;
        for (String value : array) {
            if (StringUtils.isNotBlank(value)) {
                isEmpty = false;
            }
            list.add(value);
        }
        if (isEmpty) {
            return Optional.empty();
        }
        return Optional.of(list);
    }

    public static boolean isIgnored(String[] paths, OpenAPIConfiguration config) {
        if (config.getIgnoredRoutes() == null) {
            return false;
        }
        for (String item : config.getIgnoredRoutes()) {
            final int length = item.length();
            for (String path : paths) {
                if (path.startsWith(item) && (path.length() == length || path.startsWith(PATH_DELIMITER, length))) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String[] getPath(String[] classLevelPaths, String[] methodLevelPaths) {
        if (classLevelPaths == null && methodLevelPaths == null) {
            return null;
        }


        String[] rootPaths = (classLevelPaths == null || classLevelPaths.length == 0) ? new String[]{"/"} : classLevelPaths;
        String[] methodsPaths = (methodLevelPaths == null || methodLevelPaths.length == 0) ? new String[]{""} : methodLevelPaths;

        String[] result = new String[rootPaths.length * methodsPaths.length];
        int i = 0;
        for (String root : rootPaths) {
            for (String path : methodsPaths) {
                StringBuilder b = new StringBuilder();
                appendPathComponent(root, b);
                appendPathComponent(path, b);
                result[i++] = b.toString();
            }
        }
        return result;
    }

    /**
     * appends a path component string to a StringBuilder
     * guarantees:
     * <ul>
     *     <li>nulls, empty strings and "/" are nops</li>
     *     <li>output will always start with "/" and never end with "/"</li>
     * </ul>
     *
     * @param component component to be added
     * @param to        output
     */
    private static void appendPathComponent(String component, StringBuilder to) {
        if (component == null || component.isEmpty() || "/".equals(component)) {
            return;
        }
        if (!component.startsWith("/") && (to.length() == 0 || '/' != to.charAt(to.length() - 1))) {
            to.append("/");
        }
        if (component.endsWith("/")) {
            to.append(component, 0, component.length() - 1);
        } else {
            to.append(component);
        }
    }

    public static String[] extractOperationMethod(Method method, Iterator<OpenAPIExtension> chain) {
        RequestMethod[] methods = method.getAnnotation(RequestMapping.class).method();
        if (methods.length == 0) {
            return new String[]{GET_METHOD};
        }
        String[] result = new String[methods.length];
        return (String[]) Arrays.stream(methods).distinct().map(m -> m.name().toLowerCase()).toArray();
    }
}

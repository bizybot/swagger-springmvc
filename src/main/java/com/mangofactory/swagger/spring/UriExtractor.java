package com.mangofactory.swagger.spring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.AnnotatedElement;

import static com.google.common.base.Strings.*;
import static com.mangofactory.swagger.spring.Descriptions.splitCamelCase;

@Slf4j
public class UriExtractor {

    public static String getDocumentationEndpointUri(Class<?> controllerClass) {
        String classLevelUri = resolveRequestUri(controllerClass, requestMapping(controllerClass));
        String defaultUri = splitCamelCase(controllerClass.getSimpleName(), "-").toLowerCase();
        if (isNullOrEmpty(classLevelUri)) {
            classLevelUri = "/" + defaultUri;
        }
        if (!classLevelUri.startsWith("/")) {
            classLevelUri = String.format("/%s", classLevelUri);
        }
        UriBuilder builder = new UriBuilder();
        maybeAppendPath(builder, classLevelUri);
        // Changes for removing *
        String str = builder.toString();
        if(str.contains("/*/")) {
            str = str.substring(0, str.indexOf("/*/")+1) + str.substring(str.indexOf("/*/")+3);
        }
        return str;
    }

    public static String getMethodLevelUri(Class<?> controllerClass, HandlerMethod handlerMethod) {
        String classLevelUri = resolveRequestUri(controllerClass, requestMapping(controllerClass));
        if (isNullOrEmpty(classLevelUri)) {
            classLevelUri = "/";
        }
        if (!classLevelUri.startsWith("/")) {
            classLevelUri = String.format("/%s", classLevelUri);
        }
        String methodLevelUri = resolveRequestUri(controllerClass, requestMapping(handlerMethod.getMethod()));
        UriBuilder builder = new UriBuilder();

        maybeAppendPath(builder, classLevelUri);
        maybeAppendPath(builder, methodLevelUri);
        // Changes for removing *
        String str = builder.toString();
        if(str.contains("/*/")) {
            str = str.substring(0, str.indexOf("/*/")+1) + str.substring(str.indexOf("/*/")+3);
        }
        return str;
    }

    private static void maybeAppendPath(UriBuilder builder, String toAppendUri) {
        if (!isNullOrEmpty(toAppendUri)) {
            builder.appendPath(toAppendUri);
        }
    }

    private static RequestMapping requestMapping(AnnotatedElement annotated) {
        return annotated.getAnnotation(RequestMapping.class);
    }

    protected static String resolveRequestUri(Class clazz, RequestMapping requestMapping) {
        if (requestMapping == null) {
            log.info("Class {} has no @RequestMapping", clazz);
            return null;
        }
        String[] requestUris = requestMapping.value();
        if (requestUris == null || requestUris.length == 0) {
            log.info("Class {} contains a @RequestMapping, but could not resolve the uri", clazz);
            return null;
        }
        if (requestUris.length > 1) {
            log.info("Class {} contains a @RequestMapping with multiple uri's. Only the first one will be documented.",
                    clazz);
        }
        return requestUris[0];
    }


}

package com.mangofactory.swagger.filters;

import com.fasterxml.classmate.ResolvedType;
import com.google.common.base.Objects;
import com.mangofactory.swagger.ControllerDocumentation;
import com.mangofactory.swagger.annotations.ApiModel;
import com.mangofactory.swagger.models.Model;
import com.mangofactory.swagger.spring.AllowableRangesParser;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.core.DocumentationAllowableListValues;
import com.wordnik.swagger.core.DocumentationAllowableValues;
import com.wordnik.swagger.core.DocumentationParameter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang.StringUtils;
import org.springframework.core.MethodParameter;

import java.util.Arrays;

import static com.google.common.base.Strings.*;
import static com.mangofactory.swagger.annotations.Annotations.*;
import static com.mangofactory.swagger.models.ResolvedTypes.*;

@Slf4j
public class AnnotatedParameterFilter implements Filter<DocumentationParameter> {
    @Override
    public void apply(FilterContext<DocumentationParameter> context) {
        DocumentationParameter parameter = context.subject();
        MethodParameter methodParameter = context.get("methodParameter");
        ControllerDocumentation controllerDocumentation = context.get("controllerDocumentation");
        ResolvedType resolvedType = context.get("parameterType");

        documentParameter(controllerDocumentation, parameter, methodParameter, resolvedType);
    }

    private void documentParameter(ControllerDocumentation controllerDocumentation, DocumentationParameter parameter,
                                   MethodParameter methodParameter, ResolvedType resolvedType) {

        ApiParam apiParam = methodParameter.getParameterAnnotation(ApiParam.class);
        if (apiParam == null) {
            AnnotatedParameterFilter.log.warn("{} is missing @ApiParam annotation - so generating default documentation",
                    methodParameter.getMethod());
            return;
        }
        val allowableValues = convertToAllowableValues(apiParam.allowableValues());
        String description = apiParam.value();
        boolean isRequired = apiParam.required();

        String name = selectBestParameterName(methodParameter);
        if (!isNullOrEmpty(name)) {
            parameter.setName(name);
        }
        if (!isNullOrEmpty(description)) {
            parameter.setDescription(description);
        }
        parameter.setNotes(apiParam.internalDescription());
        parameter.setDefaultValue(apiParam.defaultValue());
        parameter.setAllowableValues(allowableValues);
        parameter.setRequired(isRequired);
        parameter.setAllowMultiple(apiParam.allowMultiple());
        ApiModel apiModel = methodParameter.getParameterAnnotation(ApiModel.class);
        if (apiModel != null) {
            if (Objects.equal(resolvedType.getErasedType(), getAnnotatedType(apiModel))) {
                parameter.setDataType(getAnnotatedType(apiModel));
                String simpleName = apiModel.type().getSimpleName();
                controllerDocumentation.putModel(simpleName, new Model(simpleName, asResolvedType(apiModel.type())));
            } else {
                log.warn("Api Model override does not match the resolved type");
            }
        }

    }

    private String selectBestParameterName(MethodParameter methodParameter) {
        ApiParam apiParam = methodParameter.getParameterAnnotation(ApiParam.class);
        if (apiParam != null && !StringUtils.isEmpty(apiParam.name())) {
            return apiParam.name();
        }
        // Default
        return methodParameter.getParameterName();
    }

    protected DocumentationAllowableValues convertToAllowableValues(String csvString) {
        if (csvString.toLowerCase().startsWith("range[")) {
            val ranges = csvString.substring(6, csvString.length() - 1).split(",");
            return AllowableRangesParser.buildAllowableRangeValues(ranges, csvString);
        } else if (csvString.toLowerCase().startsWith("rangeexclusive[")) {
            val ranges = csvString.substring(15, csvString.length() - 1).split(",");
            return AllowableRangesParser.buildAllowableRangeValues(ranges, csvString);
        }
        // else..
        if (csvString == null || csvString.length() == 0) {
            return null;
        }
        val params = Arrays.asList(csvString.split(","));
        return new DocumentationAllowableListValues(params);
    }
}

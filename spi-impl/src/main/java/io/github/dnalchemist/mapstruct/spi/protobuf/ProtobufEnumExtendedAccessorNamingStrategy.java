package io.github.dnalchemist.mapstruct.spi.protobuf;

/*-
 * #%L
 * protobuf-spi-impl
 * %%
 * Copyright (C) 2025 Ruslan Mikhalev
 * %%
 * Licensed under the EUPL, Version 1.1 or – as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * #L%
 */

import java.util.HashMap;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import org.mapstruct.ap.spi.util.IntrospectorUtils;

/**
 * This class is responsible for naming the accessor methods for Protobuf enums.
 * It's use getUserEnumValue() instead converted to getUserEnum() to prevent data loss if the enum value is not presented in contract
 *
 * @author Ruslan Mikhalev
 */
public class ProtobufEnumExtendedAccessorNamingStrategy extends ProtobufAccessorNamingStrategy {
    private static final String PROTOBUF_ENUM_INTERFACE = "com.google.protobuf.ProtocolMessageEnum";
    private static final String PROTOBUF_LITE_ENUM_INTERFACE = "com.google.protobuf.Internal.EnumLite";
    private final HashMap<TypeElement, Boolean> KNOWN_ENUMS = new HashMap<>();

    public boolean isProtobufEnum(TypeMirror typeMirror) {
        TypeElement enumType = asTypeElement(typeMirror);
        if (enumType == null) {
            return false;
        }
        Boolean isProtobufEnum = KNOWN_ENUMS.get(enumType);
        if (isProtobufEnum == null) {
            List<? extends TypeMirror> interfaces = enumType.getInterfaces();
            isProtobufEnum = Boolean.FALSE;
            for (TypeMirror implementedInterface : interfaces) {
                String implementedInterfaceName = implementedInterface.toString();
                if (PROTOBUF_ENUM_INTERFACE.equals(implementedInterfaceName) || PROTOBUF_LITE_ENUM_INTERFACE.equals(implementedInterfaceName)) {
                    isProtobufEnum = Boolean.TRUE;
                    break;
                }
            }
            KNOWN_ENUMS.put(enumType, isProtobufEnum);
        }

        return isProtobufEnum;
    }

    public static TypeElement asTypeElement(TypeMirror typeMirror) {
        if (typeMirror instanceof DeclaredType) {
            Element element = ((DeclaredType) typeMirror).asElement();
            if (element instanceof TypeElement) {
                return (TypeElement) element;
            }
        }
        return null;
    }

    @Override
    public boolean isGetterMethod(ExecutableElement method) {
        String methodName = method.getSimpleName().toString();
        if (isGetterForEnumPlainIntValue(methodName)) {
            return true;
        }

        if (isProtobufEnum(method.getReturnType()) && !isOneOfDataCase(method)) {
            return false;
        }
        return super.isGetterMethod(method);
    }

    /**
     * If the method is a getter for an enum plain int value, the property name is the name of the enum field.
     * e.g. for a method named getUserEnumValue() the property name is userEnum.
     *
     * @param method the method to check
     * @return the property name
     */
    @Override
    public String getPropertyName(ExecutableElement method) {
        String methodName = method.getSimpleName().toString();

        if (isGetterForEnumPlainIntValue(methodName)) {
            return IntrospectorUtils.decapitalize(methodName.substring(3, methodName.length() - "Value".length()));
        }
        return super.getPropertyName(method);
    }

    /**
     * Check if method is protobuf-style getter for enum raw value
     * That protobuf getter provides raw int value for enum, even if it is not defined in proto file
     *
     * @param methodName that will be checked
     * @return true if method is like get<EnumName>Value()
     */
    public static boolean isGetterForEnumPlainIntValue(String methodName) {
        return methodName.startsWith("get") && methodName.endsWith("Value");
    }

    public static boolean isOneOfDataCase(ExecutableElement method) {
        String methodName = method.getSimpleName().toString();
        return methodName.startsWith("get") && methodName.endsWith("Case");
    }
}

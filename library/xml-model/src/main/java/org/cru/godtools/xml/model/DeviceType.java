package org.cru.godtools.xml.model;

import android.support.annotation.Nullable;

import com.annimon.stream.Stream;
import com.google.common.collect.Sets;

import org.jetbrains.annotations.Contract;

import java.util.EnumSet;
import java.util.Set;

enum DeviceType {
    MOBILE, UNKNOWN;

    public static final Set<DeviceType> ALL = Sets.immutableEnumSet(EnumSet.allOf(DeviceType.class));

    private static final String XML_DEVICE_TYPE_MOBILE = "mobile";

    @Nullable
    @Contract("!null -> !null; null -> null")
    static DeviceType parseSingle(@Nullable final String type) {
        if (type == null) {
            return null;
        }

        switch (type) {
            case XML_DEVICE_TYPE_MOBILE:
                return MOBILE;
            default:
                return UNKNOWN;
        }

    }

    @Nullable
    @Contract("_,!null -> !null; !null,_ -> !null")
    static Set<DeviceType> parse(@Nullable final String types, @Nullable final Set<DeviceType> defValue) {
        if (types == null) {
            return defValue;
        }

        return Sets.immutableEnumSet(
                Stream.of(types.split("\\s+"))
                        .map(DeviceType::parseSingle)
                        .distinct()
                        .toList()
        );
    }
}
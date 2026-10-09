package com.ultramega.refinedflowanalytics.resource;

import com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey;

public record ResourceChangeGranularityKey(PlatformResourceKey resourceKey, short sign, int granularity) {
}

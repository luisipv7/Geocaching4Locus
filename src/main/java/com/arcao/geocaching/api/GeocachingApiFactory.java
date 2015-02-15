package com.arcao.geocaching.api;

import com.arcao.geocaching.api.configuration.impl.DefaultStagingGeocachingApiConfiguration;
import com.arcao.geocaching.api.impl.LiveGeocachingApi;
import com.arcao.geocaching4locus.BuildConfig;

public class GeocachingApiFactory {
	public static GeocachingApi create() {
		if (BuildConfig.GEOCACHING_API_STAGING) {
			return LiveGeocachingApi.Builder.liveGeocachingApi().withConfiguration(new DefaultStagingGeocachingApiConfiguration()).build();
		} else {
			return LiveGeocachingApi.Builder.liveGeocachingApi().build();
		}
	}
}

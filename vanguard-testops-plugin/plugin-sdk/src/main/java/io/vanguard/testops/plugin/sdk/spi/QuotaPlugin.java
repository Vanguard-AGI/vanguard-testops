package io.vanguard.testops.plugin.sdk.spi;

import io.vanguard.testops.plugin.sdk.util.MSPluginException;

public abstract class QuotaPlugin extends AbstractMsPlugin {
    public abstract void interceptor(Object pjp) throws MSPluginException;
}

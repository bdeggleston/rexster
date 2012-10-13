package com.tinkerpop.rexster.protocol;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

/**
 * Holder class for different script engines.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class EngineHolder {

    private final String languageName;
    private final String languageVersion;
    private final String engineName;
    private final String engineVersion;

    private ScriptEngine engine;

    public EngineHolder(final ScriptEngineFactory factory) {
        this.languageName = factory.getLanguageName();
        this.languageVersion = factory.getLanguageVersion();
        this.engineName = factory.getEngineName();
        this.engineVersion = factory.getEngineVersion();
        this.engine = factory.getScriptEngine();
    }

    public String getLanguageVersion() {
        return languageVersion;
    }

    public String getEngineName() {
        return engineName;
    }

    public String getEngineVersion() {
        return engineVersion;
    }

    public String getLanguageName() {
        return languageName;
    }

    public ScriptEngine getEngine() {
        return this.engine;
    }
}

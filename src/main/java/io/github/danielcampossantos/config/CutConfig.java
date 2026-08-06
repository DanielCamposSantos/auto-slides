package io.github.danielcampossantos.config;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.github.danielcampossantos.model.PdfConfigModel;

import java.util.HashMap;
import java.util.Map;

public record CutConfig(Map<String, PdfConfigModel> configs) {

    @JsonCreator
    public CutConfig() {
        this(new HashMap<>());
    }

    @JsonAnySetter
    public void addConfig(String key, PdfConfigModel value) {
        if (this.configs != null) {
            this.configs.put(key, value);
        }
    }

    public PdfConfigModel getConfig(String prefixo) {
        return this.configs != null ? this.configs.get(prefixo) : null;
    }
}

package com.vcsm.dto;

import java.util.List;

public class IvrNode {
    private String id;
    private String prompt;
    private String pattern;
    private String action;
    private List<IvrNode> options;

    public IvrNode() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public List<IvrNode> getOptions() { return options; }
    public void setOptions(List<IvrNode> options) { this.options = options; }
}

package online.shikshaplus.ChatBot.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StreamEvent {
    private String type; // "token", "tool_call", "tool_result", "metrics", "error", "done"
    private String content;
    private String toolName;
    private String toolArgs;
    private String toolResult;
    private Integer tokensPrompt;
    private Integer tokensCompletion;
    private Integer tokensSaved;

    public StreamEvent() {
    }

    public static StreamEvent token(String content) {
        StreamEvent event = new StreamEvent();
        event.setType("token");
        event.setContent(content);
        return event;
    }

    public static StreamEvent toolCall(String toolName, String toolArgs) {
        StreamEvent event = new StreamEvent();
        event.setType("tool_call");
        event.setToolName(toolName);
        event.setToolArgs(toolArgs);
        return event;
    }

    public static StreamEvent toolResult(String toolName, String toolResult) {
        StreamEvent event = new StreamEvent();
        event.setType("tool_result");
        event.setToolName(toolName);
        event.setToolResult(toolResult);
        return event;
    }

    public static StreamEvent metrics(int promptTokens, int completionTokens, int savedTokens) {
        StreamEvent event = new StreamEvent();
        event.setType("metrics");
        event.setTokensPrompt(promptTokens);
        event.setTokensCompletion(completionTokens);
        event.setTokensSaved(savedTokens);
        return event;
    }

    public static StreamEvent error(String message) {
        StreamEvent event = new StreamEvent();
        event.setType("error");
        event.setContent(message);
        return event;
    }

    public static StreamEvent done() {
        StreamEvent event = new StreamEvent();
        event.setType("done");
        return event;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public String getToolArgs() {
        return toolArgs;
    }

    public void setToolArgs(String toolArgs) {
        this.toolArgs = toolArgs;
    }

    public String getToolResult() {
        return toolResult;
    }

    public void setToolResult(String toolResult) {
        this.toolResult = toolResult;
    }

    public Integer getTokensPrompt() {
        return tokensPrompt;
    }

    public void setTokensPrompt(Integer tokensPrompt) {
        this.tokensPrompt = tokensPrompt;
    }

    public Integer getTokensCompletion() {
        return tokensCompletion;
    }

    public void setTokensCompletion(Integer tokensCompletion) {
        this.tokensCompletion = tokensCompletion;
    }

    public Integer getTokensSaved() {
        return tokensSaved;
    }

    public void setTokensSaved(Integer tokensSaved) {
        this.tokensSaved = tokensSaved;
    }
}

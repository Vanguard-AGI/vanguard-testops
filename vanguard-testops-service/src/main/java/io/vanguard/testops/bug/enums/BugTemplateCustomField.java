package io.vanguard.testops.bug.enums;

import io.vanguard.testops.sdk.util.Translator;
import lombok.Getter;

public enum BugTemplateCustomField {

    /**
     * 处理人
     */
    HANDLE_USER("handleUser", "bug.handle_user"),
    /**
     * 状态
     */
    STATUS("status", "bug.status");


    @Getter
	private final String id;

    private final String name;

    BugTemplateCustomField(String id, String name) {
        this.id = id;
        this.name = name;
    }

	public String getName(String language) {
        return Translator.get(name, language);
    }
}

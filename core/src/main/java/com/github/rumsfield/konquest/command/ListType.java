package com.github.rumsfield.konquest.command;

import com.github.rumsfield.konquest.utility.MessagePath;

public enum ListType {

    KINGDOM     (MessagePath.LABEL_KINGDOM.getMessage()),
    TOWN        (MessagePath.LABEL_TOWN.getMessage()),
    CAMP        (MessagePath.LABEL_CAMP.getMessage()),
    RUIN        (MessagePath.LABEL_RUIN.getMessage()),
    SANCTUARY   (MessagePath.LABEL_SANCTUARY.getMessage());

    private final String label;

    ListType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

}

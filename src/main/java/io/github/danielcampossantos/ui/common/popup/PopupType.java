package io.github.danielcampossantos.ui.common.popup;

public enum PopupType {

    INFORMATION("i", "popup-information"),
    SUCCESS("✓", "popup-success"),
    WARNING("!", "popup-warning"),
    ERROR("×", "popup-error");

    private final String symbol;

    private final String styleClass;

    PopupType(String symbol, String styleClass) {
        this.symbol = symbol;
        this.styleClass = styleClass;
    }

    public String symbol() {
        return symbol;
    }

    public String styleClass() {
        return styleClass;
    }
}
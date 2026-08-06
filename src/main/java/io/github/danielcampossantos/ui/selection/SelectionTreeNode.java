package io.github.danielcampossantos.ui.selection;

public record SelectionTreeNode(
        NodeType type,
        Object value,
        String text
) {

    @Override
    public String toString() {

        return text;

    }

}
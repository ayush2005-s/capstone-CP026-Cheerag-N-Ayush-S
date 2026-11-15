package com.nutriplanner.web.html;

import java.util.ArrayList;
import java.util.List;

/** A group of nodes without a wrapping element. */
public final class Fragment implements Node {

    private final List<Node> nodes = new ArrayList<>();

    public Fragment add(Node... more) {
        for (Node n : more) {
            if (n != null) nodes.add(n);
        }
        return this;
    }

    public Fragment add(List<? extends Node> more) {
        for (Node n : more) {
            if (n != null) nodes.add(n);
        }
        return this;
    }

    public boolean isEmpty() {
        return nodes.isEmpty();
    }

    @Override
    public void render(StringBuilder out) {
        for (Node n : nodes) n.render(out);
    }
}

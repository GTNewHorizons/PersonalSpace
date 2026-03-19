package me.eigenraven.personalspace.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BoundaryBlockRules {

    private BoundaryBlockRules() {}

    public static List<AllowedBoundaryBlock> parseAll(List<String> raw) {
        List<AllowedBoundaryBlock> out = new ArrayList<>();
        if (raw == null) {
            return out;
        }
        for (String s : raw) {
            AllowedBoundaryBlock parsed = null;
            try {
                parsed = AllowedBoundaryBlock.parse(s);
            } catch (Exception ignored) {}
            if (parsed != null) {
                out.add(parsed);
            }
        }
        return out;
    }

    public static AllowedBoundaryBlock findByBlockName(List<AllowedBoundaryBlock> rules, String blockName) {
        if (rules == null || blockName == null || blockName.isEmpty()) {
            return null;
        }
        for (AllowedBoundaryBlock rule : rules) {
            if (blockName.equals(rule.blockName())) {
                return rule;
            }
        }
        return null;
    }

    public static List<String> extractBlockNames(List<AllowedBoundaryBlock> rules) {
        if (rules == null || rules.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> out = new ArrayList<>(rules.size() + 1);
        out.add("");
        for (AllowedBoundaryBlock rule : rules) {
            out.add(rule.blockName());
        }
        return out;
    }
}

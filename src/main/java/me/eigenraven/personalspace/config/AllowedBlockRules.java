package me.eigenraven.personalspace.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AllowedBlockRules {

    private AllowedBlockRules() {}

    public static List<AllowedBlock> parseAll(List<String> raw) {
        List<AllowedBlock> out = new ArrayList<>();
        if (raw == null) {
            return out;
        }
        for (String s : raw) {
            AllowedBlock parsed = null;
            try {
                parsed = AllowedBlock.parse(s);
            } catch (NumberFormatException ignored) {}
            if (parsed != null) {
                out.add(parsed);
            }
        }
        return out;
    }

    public static AllowedBlock findByBlockName(List<AllowedBlock> rules, String blockName) {
        if (rules == null || blockName == null || blockName.isEmpty()) {
            return null;
        }
        for (AllowedBlock rule : rules) {
            if (blockName.equals(rule.blockName())) {
                return rule;
            }
        }
        return null;
    }

    public static List<String> extractBlockNames(List<AllowedBlock> rules) {
        if (rules == null || rules.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> out = new ArrayList<>(rules.size() + 1);
        out.add("");
        for (AllowedBlock rule : rules) {
            out.add(rule.blockName());
        }
        return out;
    }
}

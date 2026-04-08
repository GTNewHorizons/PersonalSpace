package me.eigenraven.personalspace.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import com.github.bsideup.jabel.Desugar;

@Desugar
public record AllowedBoundaryBlock(String blockName, List<MetaRange> ranges, String original) {

    @Desugar
    public record MetaRange(int min, int max, boolean negated) {

        public MetaRange(int min, int max, boolean negated) {
            this.min = Math.min(min, max);
            this.max = Math.max(min, max);
            this.negated = negated;
        }

        public boolean contains(int value) {
            return value >= min && value <= max;
        }

        @Nonnull
        @Override
        public String toString() {
            String range = min == max ? Integer.toString(min) : (min + "-" + max);
            return negated ? ("!" + range) : range;
        }
    }

    public AllowedBoundaryBlock(String blockName, List<MetaRange> ranges, String original) {
        this.blockName = blockName;
        this.ranges = Collections.unmodifiableList(new ArrayList<>(ranges));
        this.original = original;
    }

    public boolean isMetaAllowed(int meta) {
        boolean hasPositive = false;
        boolean matchedPositive = false;

        for (MetaRange range : ranges) {
            if (range.negated()) {
                if (range.contains(meta)) {
                    return false;
                }
            } else {
                hasPositive = true;
                if (range.contains(meta)) {
                    matchedPositive = true;
                }
            }
        }

        return !hasPositive || matchedPositive;
    }

    public int clampMeta(int meta) {
        if (ranges.isEmpty()) {
            return 0;
        }
        if (isMetaAllowed(meta)) {
            return meta;
        }

        int best = -1;
        int bestDist = Integer.MAX_VALUE;

        int lo = getMinAllowedMeta();
        int hi = getMaxAllowedMeta();
        for (int i = lo; i <= hi; i++) {
            if (isMetaAllowed(i)) {
                int dist = Math.abs(meta - i);
                if (dist < bestDist) {
                    best = i;
                    bestDist = dist;
                }
            }
        }
        return Math.max(best, 0);
    }

    public int getMinAllowedMeta() {
        int min = Integer.MAX_VALUE;
        for (MetaRange range : ranges) {
            if (!range.negated() && range.min < min) {
                min = range.min;
            }
        }
        if (min == Integer.MAX_VALUE) {
            return 0;
        }
        for (int i = min;; i++) {
            if (isMetaAllowed(i)) {
                return i;
            }
            if (i > min + 1000) break;
        }
        return min;
    }

    public int getMaxAllowedMeta() {
        int max = 0;
        for (MetaRange range : ranges) {
            if (!range.negated() && range.max > max) {
                max = range.max;
            }
        }
        for (int i = max; i >= 0; i--) {
            if (isMetaAllowed(i)) {
                return i;
            }
        }
        return 0;
    }

    public String getMetaDescription() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ranges.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(ranges.get(i).toString());
        }
        return sb.toString();
    }

    public static AllowedBoundaryBlock parse(String s) {
        if (s == null) {
            return null;
        }
        String str = s.trim();
        if (str.isEmpty()) {
            return null;
        }

        int lastColon = str.lastIndexOf(':');
        if (lastColon <= 0 || lastColon >= str.length() - 1) {
            return null;
        }

        String blockName = str.substring(0, lastColon).trim();
        String metaSpec = str.substring(lastColon + 1).trim();

        if (blockName.isEmpty() || metaSpec.isEmpty()) {
            return null;
        }

        String[] blockSplit = blockName.split(":");
        if (blockSplit.length != 2) {
            return null;
        }

        List<MetaRange> ranges = new ArrayList<>();
        String[] parts = metaSpec.split(",");
        for (String partRaw : parts) {
            String part = partRaw.trim();
            if (part.isEmpty()) {
                continue;
            }

            boolean negated = false;
            if (part.startsWith("!")) {
                negated = true;
                part = part.substring(1).trim();
                if (part.isEmpty()) {
                    continue;
                }
            }

            int sep = part.indexOf('-');
            if (sep < 0) {
                sep = part.indexOf('~');
            }
            if (sep >= 0) {
                int a = Integer.parseInt(part.substring(0, sep).trim());
                int b = Integer.parseInt(part.substring(sep + 1).trim());
                a = Math.max(a, 0);
                b = Math.max(b, 0);
                ranges.add(new MetaRange(a, b, negated));
            } else {
                int m = Integer.parseInt(part);
                m = Math.max(m, 0);
                ranges.add(new MetaRange(m, m, negated));
            }
        }

        if (ranges.isEmpty()) {
            return null;
        }

        return new AllowedBoundaryBlock(blockName, ranges, str);
    }
}

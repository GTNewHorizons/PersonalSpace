package me.eigenraven.personalspace.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.util.MathHelper;

import com.github.bsideup.jabel.Desugar;

@Desugar
public record AllowedBoundaryBlock(String blockName, List<MetaRange> ranges, String original) {

    @Desugar
    public record MetaRange(int min, int max) {

        public MetaRange(int min, int max) {
            this.min = Math.min(min, max);
            this.max = Math.max(min, max);
        }

        public boolean contains(int value) {
            return value >= min && value <= max;
        }

        @Override
        public String toString() {
            return min == max ? Integer.toString(min) : (min + "~" + max);
        }
    }

    public AllowedBoundaryBlock(String blockName, List<MetaRange> ranges, String original) {
        this.blockName = blockName;
        this.ranges = Collections.unmodifiableList(new ArrayList<>(ranges));
        this.original = original;
    }

    public boolean isMetaAllowed(int meta) {
        for (MetaRange range : ranges) {
            if (range.contains(meta)) {
                return true;
            }
        }
        return false;
    }

    public int clampMeta(int meta) {
        if (ranges.isEmpty()) {
            return 0;
        }
        if (isMetaAllowed(meta)) {
            return meta;
        }

        int best = ranges.get(0).min;
        int bestDist = Math.abs(meta - best);

        for (MetaRange range : ranges) {
            int candidate = MathHelper.clamp_int(meta, range.min, range.max);
            int dist = Math.abs(meta - candidate);
            if (dist < bestDist) {
                best = candidate;
                bestDist = dist;
            }
        }
        return best;
    }

    public int getMinAllowedMeta() {
        if (ranges.isEmpty()) {
            return 0;
        }
        int min = Integer.MAX_VALUE;
        for (MetaRange range : ranges) {
            if (range.min < min) {
                min = range.min;
            }
        }
        return min;
    }

    public int getMaxAllowedMeta() {
        if (ranges.isEmpty()) {
            return 0;
        }
        int max = Integer.MIN_VALUE;
        for (MetaRange range : ranges) {
            if (range.max > max) {
                max = range.max;
            }
        }
        return max;
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
            int sep = part.indexOf('~');
            if (sep >= 0) {
                int a = Integer.parseInt(part.substring(0, sep).trim());
                int b = Integer.parseInt(part.substring(sep + 1).trim());
                a = MathHelper.clamp_int(a, 0, 15);
                b = MathHelper.clamp_int(b, 0, 15);
                ranges.add(new MetaRange(a, b));
            } else {
                int m = Integer.parseInt(part);
                m = MathHelper.clamp_int(m, 0, 15);
                ranges.add(new MetaRange(m, m));
            }
        }

        if (ranges.isEmpty()) {
            return null;
        }

        return new AllowedBoundaryBlock(blockName, ranges, str);
    }
}

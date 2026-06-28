package com.prometheus.watcher.service;

import java.util.List;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import org.springframework.stereotype.Service;

/** Produces a unified diff and add/remove line counts between two texts. */
@Service
public class DiffService {

    public record DiffResult(String unifiedDiff, int addedLines, int removedLines, boolean changed) {
    }

    public DiffResult diff(String previous, String current) {
        List<String> oldLines = previous.lines().toList();
        List<String> newLines = current.lines().toList();

        Patch<String> patch = DiffUtils.diff(oldLines, newLines);
        if (patch.getDeltas().isEmpty()) {
            return new DiffResult("", 0, 0, false);
        }

        int added = 0;
        int removed = 0;
        for (AbstractDelta<String> delta : patch.getDeltas()) {
            removed += delta.getSource().getLines().size();
            added += delta.getTarget().getLines().size();
        }

        String unified = generateUnifiedDiff(oldLines, patch);
        return new DiffResult(unified, added, removed, true);
    }

    private String generateUnifiedDiff(List<String> oldLines, Patch<String> patch) {
        try {
            List<String> diffLines = UnifiedDiffUtils.generateUnifiedDiff(
                    "previous", "current", oldLines, patch, 3);
            return String.join("\n", diffLines);
        } catch (Exception ex) {
            // Should not happen for in-memory texts; fall back to a marker.
            return "(diff generation failed: " + ex.getMessage() + ")";
        }
    }
}

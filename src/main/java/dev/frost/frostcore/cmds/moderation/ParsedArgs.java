package dev.frost.frostcore.cmds.moderation;

import dev.frost.frostcore.moderation.Punishment;

/**
 * Utility for parsing moderation command arguments.
 * Extracts duration, reason, -s (silent), and -t (template) flags.
 */
public class ParsedArgs {

    public final long duration;      
    public final String reason;
    public final boolean silent;
    public final String template;

    private ParsedArgs(long duration, String reason, boolean silent, String template) {
        this.duration = duration;
        this.reason = reason;
        this.silent = silent;
        this.template = template;
    }

    /**
     * Parse args starting from a given index.
     * Supports: [duration] [reason words...] [-s] [-t template]
     *
     * @param args the full command arguments
     * @param startIndex index to start parsing from (skipping player name etc.)
     * @return ParsedArgs with extracted values
     */
    public static ParsedArgs parse(String[] args, int startIndex) {
        long duration = -1; // Default to permanent
        boolean silent = false;
        String template = null;
        StringBuilder reason = new StringBuilder();

        boolean durationParsed = false;

        for (int i = startIndex; i < args.length; i++) {
            String arg = args[i];

            if (arg.equalsIgnoreCase("-s")) {
                silent = true;
                continue;
            }

            if (arg.equalsIgnoreCase("-t") && i + 1 < args.length) {
                template = args[++i];
                continue;
            }

            // Try to parse as duration (only the first non-flag arg)
            if (!durationParsed) {
                long parsed = Punishment.parseTime(arg);
                if (parsed != -2) {
                    duration = parsed;
                    durationParsed = true;
                    continue;
                }
            }

            // Everything else is part of the reason
            if (!reason.isEmpty()) reason.append(" ");
            reason.append(arg);
        }

        return new ParsedArgs(duration, reason.toString(), silent, template);
    }

    /**
     * Parse with a required duration (for /tempban, /tempmute).
     * Returns ParsedArgs with duration = -2 if not found (caller should error).
     */
    public static ParsedArgs parseRequired(String[] args, int startIndex) {
        if (startIndex >= args.length) {
            return new ParsedArgs(-2, "", false, null);
        }

        long duration = Punishment.parseTime(args[startIndex]);
        if (duration == -2 || duration == -1) {
            return new ParsedArgs(-2, "", false, null);
        }

        // Parse rest as reason + flags
        boolean silent = false;
        String template = null;
        StringBuilder reason = new StringBuilder();

        for (int i = startIndex + 1; i < args.length; i++) {
            String arg = args[i];
            if (arg.equalsIgnoreCase("-s")) { silent = true; continue; }
            if (arg.equalsIgnoreCase("-t") && i + 1 < args.length) { template = args[++i]; continue; }
            if (!reason.isEmpty()) reason.append(" ");
            reason.append(arg);
        }

        return new ParsedArgs(duration, reason.toString(), silent, template);
    }
}

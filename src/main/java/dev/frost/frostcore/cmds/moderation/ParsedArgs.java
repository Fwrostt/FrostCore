package dev.frost.frostcore.cmds.moderation;

import dev.frost.frostcore.moderation.Punishment;


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

    
    public static ParsedArgs parse(String[] args, int startIndex) {
        long duration = -1; 
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

            
            if (!durationParsed) {
                long parsed = Punishment.parseTime(arg);
                if (parsed != -2) {
                    duration = parsed;
                    durationParsed = true;
                    continue;
                }
            }

            
            if (!reason.isEmpty()) reason.append(" ");
            reason.append(arg);
        }

        return new ParsedArgs(duration, reason.toString(), silent, template);
    }

    
    public static ParsedArgs parseReasonOnly(String[] args, int startIndex) {
        boolean silent = false;
        String template = null;
        StringBuilder reason = new StringBuilder();

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
            if (!reason.isEmpty()) reason.append(" ");
            reason.append(arg);
        }

        return new ParsedArgs(-1, reason.toString(), silent, template);
    }

    
    public static ParsedArgs parseRequired(String[] args, int startIndex) {
        if (startIndex >= args.length) {
            return new ParsedArgs(-2, "", false, null);
        }

        long duration = Punishment.parseTime(args[startIndex]);
        if (duration == -2 || duration == -1) {
            return new ParsedArgs(-2, "", false, null);
        }

        
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

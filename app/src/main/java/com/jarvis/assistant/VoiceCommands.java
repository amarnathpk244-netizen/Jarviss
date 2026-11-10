package com.jarvis.assistant;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Shared by the Android service and the host-side regression checks. */
public final class VoiceCommands {
    private VoiceCommands() {}
    private static final Pattern WAKE = Pattern.compile(
        "^(?:(?:hey|hi|hello|ഹേയ്|ഹേ|ഹായ്|ഹലോ)\\s+)?(?:jarvis|jarviss|ജാർവിസ്|ജാർവിസേ|ജാർവിസ്‌|ജാർവിസ്സ്|ജാര്വിസ്|ജാര്വിസ)(?=$|[\\s,!.:;?])[\\s,!.:;?]*(.*)$");
    public static String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).replace('\u200c', ' ')
            .replace('\u200d', ' ').replaceAll("\\s+", " ").trim();
    }
    /** null means no wake word; empty means a wake word without a command. */
    public static String afterWake(String text) {
        Matcher match = WAKE.matcher(normalize(text));
        return match.matches() ? match.group(1).trim() : null;
    }
    private static boolean has(String text, String... choices) {
        for (String choice : choices) if (text.contains(choice)) return true;
        return false;
    }
    public static final class Command {
        public final String kind;
        public final String argument;
        Command(String kind, String argument) { this.kind = kind; this.argument = argument; }
    }
    public static Command parse(String text) {
        String s = normalize(text);
        if (s.matches("stop|cancel|never mind|നിർത്തൂ|നിർത്തു|വേണ്ട")) return new Command("cancel", "");
        if (s.matches("stop listening|shutdown|shut down|ജാർവിസ് നിർത്തൂ")) return new Command("stop", "");
        if (s.matches(".*\\b(time|clock)\\b.*") || has(s, "സമയം", "സമയമായി")) return new Command("time", "");
        if (s.matches(".*\\b(date|day)\\b.*") || has(s, "തീയതി", "തിയ്യതി", "ദിവസം")) return new Command("date", "");
        Matcher nav = Pattern.compile("^(?:navigate(?: to)?|take me to|directions? to|വഴി കാണിക്കൂ|വഴി കാണിക്ക്)\\s+(.+)$").matcher(s);
        if (nav.matches()) return new Command("navigate", nav.group(1));
        Matcher mlNav = Pattern.compile("^(.+?(?:ലേക്ക്|യിലേക്ക്|ലോട്ടു|യിലോട്ട്))\\s*(?:വഴി|പോകണം|നാവിഗേറ്റ്).*$").matcher(s);
        if (mlNav.matches()) return new Command("navigate", mlNav.group(1).trim());
        Matcher call = Pattern.compile("^(?:call|dial|വിളിക്കൂ|വിളിക്ക്)\\s+(.+)$").matcher(s);
        if (call.matches()) return new Command("dial", call.group(1));
        boolean open = has(s, "open", "launch", "തുറക്ക്", "തുറക്കൂ", "തുറക്ക", "ഓപ്പൺ", "ഓപ്പണ്");
        if (open) {
            String[][] apps = {
                {"com.instagram.android", "instagram", "ഇൻസ്റ്റാഗ്രാം", "ഇൻസ്റ്റഗ്രാം", "ഇന്‍സ്റ്റഗ്രാം"},
                {"com.whatsapp", "whatsapp", "വാട്സ്ആപ്പ്", "വാട്ട്സ്ആപ്പ്", "വാട്സാപ്പ്", "വാട്ട്സാപ്പ്"},
                {"com.google.android.youtube", "youtube", "you tube", "യൂട്യൂബ്", "യൂടൂബ്"},
                {"com.google.android.apps.maps", "maps", "മാപ്സ്", "മാപ്പ്"},
                {"com.android.chrome", "chrome", "ക്രോം"}
            };
            for (String[] app : apps) for (int i = 1; i < app.length; i++)
                if (s.contains(app[i])) return new Command("app", app[0]);
            if (has(s, "camera", "ക്യാമറ", "കാമറ")) return new Command("camera", "");
            if (has(s, "settings", "സെറ്റിംഗ്സ്", "സെറ്റിങ്സ്")) return new Command("settings", "");
        }
        return new Command("unknown", s);
    }
}

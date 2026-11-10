import com.jarvis.assistant.VoiceCommands;

public class VoiceRegression {
    private static int checks;
    private static void eq(Object expected, Object actual) {
        checks++;
        if (!java.util.Objects.equals(expected, actual)) throw new AssertionError("Expected " + expected + " but got " + actual);
    }
    public static void main(String[] args) {
        eq("", VoiceCommands.afterWake("Hey Jarvis"));
        eq("", VoiceCommands.afterWake("ഹേയ് ജാർവിസ്"));
        eq("", VoiceCommands.afterWake("ജാർവിസ്"));
        eq("open youtube!", VoiceCommands.afterWake("Hey Jarvis, open YouTube!"));
        eq("യൂട്യൂബ് തുറക്കൂ", VoiceCommands.afterWake("ഹായ് ജാർവിസ് യൂട്യൂബ് തുറക്കൂ"));
        eq(null, VoiceCommands.afterWake("I was talking about Jarvis"));
        eq(null, VoiceCommands.afterWake("jarvison open youtube"));
        eq("app", VoiceCommands.parse("YouTube തുറക്കൂ").kind);
        eq("com.google.android.youtube", VoiceCommands.parse("യൂട്യൂബ് തുറക്കൂ").argument);
        eq("com.whatsapp", VoiceCommands.parse("വാട്ട്സാപ്പ് ഓപ്പൺ ചെയ്യൂ").argument);
        eq("com.instagram.android", VoiceCommands.parse("Open Instagram").argument);
        eq("com.google.android.apps.maps", VoiceCommands.parse("മാപ്പ് തുറക്ക്").argument);
        eq("com.android.chrome", VoiceCommands.parse("Launch Chrome").argument);
        eq("unknown", VoiceCommands.parse("I watched YouTube yesterday").kind);
        eq("time", VoiceCommands.parse("സമയം എത്രയായി").kind);
        eq("time", VoiceCommands.parse("What is the time?").kind);
        eq("date", VoiceCommands.parse("ഇന്നത്തെ തീയതി എന്താണ്").kind);
        eq("unknown", VoiceCommands.parse("update the app").kind);
        eq("navigate", VoiceCommands.parse("navigate to Kozhikode").kind);
        eq("kozhikode", VoiceCommands.parse("navigate to Kozhikode").argument);
        eq("കോഴിക്കോടിലേക്ക്", VoiceCommands.parse("കോഴിക്കോടിലേക്ക് വഴി കാണിക്കൂ").argument);
        eq("maps office", VoiceCommands.parse("navigate to maps office").argument);
        eq("camera", VoiceCommands.parse("ക്യാമറ തുറക്കൂ").kind);
        eq("settings", VoiceCommands.parse("Open settings").kind);
        eq("dial", VoiceCommands.parse("call 1234567890").kind);
        eq("cancel", VoiceCommands.parse("നിർത്തൂ").kind);
        eq("stop", VoiceCommands.parse("stop listening").kind);
        eq("unknown", VoiceCommands.parse("stop by the shop").kind);
        System.out.println("PASS: " + checks + " voice command regression checks");
    }
}

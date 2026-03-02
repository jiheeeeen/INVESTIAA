package Tests;

import Services.GroqChatService;

public class MainGroqChat {
    public static void main(String[] args) {
        String apiKey = System.getenv("GROQ_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("Missing GROQ_API_KEY environment variable.");
            return;
        }

        String prompt = (args.length > 0) ? args[0] : "Donne un resume court de la plateforme.";

        try {
            GroqChatService chatService = new GroqChatService(apiKey, "openai/gpt-oss-120b");
            String answer = chatService.chat(prompt);
            System.out.println("Assistant:");
            System.out.println(answer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

package ht.mbds.calebtoussaint.tp4ragwebcalebtoussaint.llm;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.enterprise.context.Dependent;

import java.io.Serializable;

/**
 * Classe "métier" qui gère l'interface avec l'API du LLM (Gemini), via LangChain4j.
 * Portée CDI "Dependent" : une instance de cette classe est créée et détruite en même temps
 * que le backing bean (Bb) dans lequel elle est injectée.
 */
@Dependent
public class LlmClient implements Serializable {

    /** Le rôle système choisi par l'utilisateur. */
    private String systemRole;

    /** Le service IA utilisé pour dialoguer avec le LLM. */
    private Assistant assistant;

    /** La mémoire utilisée par l'assistant pour garder l'historique de la conversation. */
    private ChatMemory chatMemory;

    /**
     * Récupère la clé secrète et crée le modèle et l'assistant IA.
     */
    public LlmClient() {
        String cle = System.getenv("GEMINI_KEY");

        ChatModel model = GoogleAiGeminiChatModel.builder()
                .apiKey(cle)
                .modelName("gemini-2.5-flash")
                .temperature(0.7)
                .build();

        this.chatMemory = MessageWindowChatMemory.withMaxMessages(20);
        this.assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .chatMemory(chatMemory)
                .build();
    }

    /**
     * Fixe le rôle système de l'assistant, pour toute la conversation.
     * A appeler une seule fois, au tout début de la conversation.
     */
    public void setSystemRole(String systemRole) {
        this.systemRole = systemRole;
        this.chatMemory.add(SystemMessage.from(systemRole));
    }

    /**
     * Envoie une question au LLM et retourne sa réponse.
     */
    public String envoyer(String question) {
        return this.assistant.chat(question);
    }
}
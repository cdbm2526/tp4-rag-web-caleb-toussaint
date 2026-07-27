package ht.mbds.calebtoussaint.tp4ragwebcalebtoussaint.llm;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.query.router.LanguageModelQueryRouter;
import dev.langchain4j.rag.query.router.QueryRouter;
import dev.langchain4j.rag.query.transformer.CompressingQueryTransformer;
import dev.langchain4j.service.AiServices;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import java.io.Serializable;

/**
 * Classe "métier" qui gère l'interface avec l'API du LLM (Gemini), via LangChain4j,
 * avec RAG : routage entre 2 documents PDF et transformation de la question
 * pour tenir compte de l'historique de la conversation.
 * Portée CDI "Dependent" : une instance de cette classe est créée et détruite en même temps
 * que le backing bean (Bb) dans lequel elle est injectée.
 */
@Dependent
public class LlmClient implements Serializable {

    @Inject
    private RagResources ragResources;

    /** Le rôle système choisi par l'utilisateur. */
    private String systemRole;

    /** Le service IA utilisé pour dialoguer avec le LLM. */
    private Assistant assistant;

    /** La mémoire utilisée par l'assistant pour garder l'historique de la conversation. */
    private ChatMemory chatMemory;

    /**
     * Constructeur par defaut requis par CDI. L'initialisation reelle se fait
     * dans init(), une fois que RagResources a ete injecte.
     */
    public LlmClient() {
    }

    /**
     * Cree l'assistant IA, avec routage entre les 2 documents et transformation
     * de la question. Appelee au premier usage (les champs injectes ne sont
     * disponibles qu'apres la construction de l'objet par CDI).
     */
    private void initAssistantSiNecessaire() {
        if (this.assistant != null) {
            return;
        }

        QueryRouter queryRouter = LanguageModelQueryRouter.builder()
                .chatModel(ragResources.getChatModel())
                .retrieverToDescription(ragResources.getDescriptions())
                .build();

        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .queryTransformer(new CompressingQueryTransformer(ragResources.getChatModel()))
                .queryRouter(queryRouter)
                .build();

        this.chatMemory = MessageWindowChatMemory.withMaxMessages(20);
        this.assistant = AiServices.builder(Assistant.class)
                .chatModel(ragResources.getChatModel())
                .chatMemory(chatMemory)
                .retrievalAugmentor(retrievalAugmentor)
                .build();
    }

    /**
     * Fixe le rôle système de l'assistant, pour toute la conversation.
     * A appeler une seule fois, au tout début de la conversation.
     */
    public void setSystemRole(String systemRole) {
        initAssistantSiNecessaire();
        this.systemRole = systemRole;
        this.chatMemory.add(SystemMessage.from(systemRole));
    }

    /**
     * Envoie une question au LLM et retourne sa réponse.
     */
    public String envoyer(String question) {
        initAssistantSiNecessaire();
        return this.assistant.chat(question);
    }
}
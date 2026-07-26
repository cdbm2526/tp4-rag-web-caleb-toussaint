package ht.mbds.calebtoussaint.tp4ragwebcalebtoussaint.llm;

/**
 * Service IA : décrit les interactions possibles entre l'application et le LLM.
 * LangChain4j fournit automatiquement une implémentation de cette interface.
 */
public interface Assistant {
    String chat(String prompt);
}
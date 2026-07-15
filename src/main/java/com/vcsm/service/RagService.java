package com.vcsm.service;

import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RagService {

    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    @Autowired
    public RagService(VectorStore vectorStore, ChatClient chatClient) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClient;
    }

    public void ingestPdf(MultipartFile file) throws Exception {
        Resource resource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        };

        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource);
        
        TokenTextSplitter textSplitter = new TokenTextSplitter();
        List<Document> splitDocuments = textSplitter.apply(pdfReader.get());
        vectorStore.add(splitDocuments);
    }

    public String answerQuestion(String query) {
        List<Document> similarDocuments = vectorStore.similaritySearch(SearchRequest.query(query).withTopK(3));
        String documentsContext = similarDocuments.stream()
                .map(Document::getContent)
                .collect(Collectors.joining("\n"));

        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(
                "You are a helpful community assistant. Use the following context from community rulebooks and lease agreements to answer the user's question.\n" +
                        "If you don't know the answer based on the context, politely inform the user that you don't have that information.\n" +
                        "Context:\n{context}"
        );
        Message systemMessage = systemPromptTemplate.createMessage(Map.of("context", documentsContext));
        UserMessage userMessage = new UserMessage(query);

        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));
        return chatClient.call(prompt).getResult().getOutput().getContent();
    }
}

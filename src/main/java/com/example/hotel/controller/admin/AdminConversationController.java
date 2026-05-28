package com.example.hotel.controller.admin;

import com.example.hotel.entity.Conversation;
import com.example.hotel.entity.ConversationCategory;
import com.example.hotel.entity.ConversationStatus;
import com.example.hotel.form.ConversationForm;
import com.example.hotel.repository.ConversationRepository;
import com.example.hotel.service.ConversationService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class AdminConversationController {

    private final ConversationRepository conversationRepository;
    private final ConversationService conversationService;

    public AdminConversationController(ConversationRepository conversationRepository,
                                       ConversationService conversationService) {
        this.conversationRepository = conversationRepository;
        this.conversationService = conversationService;
    }

    @ModelAttribute("categories")
    ConversationCategory[] categories() {
        return ConversationCategory.values();
    }

    @ModelAttribute("statuses")
    ConversationStatus[] statuses() {
        return ConversationStatus.values();
    }

    @GetMapping("/admin/conversations")
    public String conversations(@RequestParam(required = false) ConversationCategory category,
                                @RequestParam(required = false) ConversationStatus status,
                                Model model) {
        model.addAttribute("conversations", filter(category, status));
        model.addAttribute("selectedCategory", category);
        model.addAttribute("selectedStatus", status);
        return "admin/conversations/list";
    }

    @GetMapping("/admin/conversations/new")
    public String newConversation(Model model) {
        ConversationForm form = new ConversationForm();
        form.setTurns(List.of(newTurn("USER"), newTurn("ASSISTANT")));
        model.addAttribute("conversationForm", form);
        model.addAttribute("pageTitle", "New Conversation");
        return "admin/conversations/form";
    }

    @PostMapping("/admin/conversations")
    public String create(@Valid @ModelAttribute ConversationForm conversationForm,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "New Conversation");
            return "admin/conversations/form";
        }
        try {
            conversationService.saveFromForm(conversationForm, null);
            redirectAttributes.addFlashAttribute("success", "Conversation saved.");
            return "redirect:/admin/conversations";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("turns", ex.getMessage());
            model.addAttribute("pageTitle", "New Conversation");
            return "admin/conversations/form";
        }
    }

    @GetMapping("/admin/conversations/{id}")
    public String view(@PathVariable Long id, Model model) {
        model.addAttribute("conversation", conversationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Conversation not found")));
        return "admin/conversations/view";
    }

    @GetMapping("/admin/conversations/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        Conversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Conversation not found"));
        model.addAttribute("conversationForm", conversationService.toForm(conversation));
        model.addAttribute("conversationId", id);
        model.addAttribute("pageTitle", "Edit Conversation");
        return "admin/conversations/form";
    }

    @PostMapping("/admin/conversations/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute ConversationForm conversationForm,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("conversationId", id);
            model.addAttribute("pageTitle", "Edit Conversation");
            return "admin/conversations/form";
        }
        try {
            conversationService.saveFromForm(conversationForm, id);
            redirectAttributes.addFlashAttribute("success", "Conversation updated.");
            return "redirect:/admin/conversations";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("turns", ex.getMessage());
            model.addAttribute("conversationId", id);
            model.addAttribute("pageTitle", "Edit Conversation");
            return "admin/conversations/form";
        }
    }

    @PostMapping("/admin/conversations/{id}/approve")
    public String approve(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Conversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Conversation not found"));
        conversation.setStatus(ConversationStatus.APPROVED);
        conversationRepository.save(conversation);
        redirectAttributes.addFlashAttribute("success", "Conversation approved.");
        return "redirect:/admin/conversations";
    }

    @PostMapping("/admin/conversations/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        conversationRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Conversation deleted.");
        return "redirect:/admin/conversations";
    }

    private List<Conversation> filter(ConversationCategory category, ConversationStatus status) {
        if (category != null && status != null) {
            return conversationRepository.findByCategoryAndStatusOrderByCreatedAtDesc(category, status);
        }
        if (category != null) {
            return conversationRepository.findByCategoryOrderByCreatedAtDesc(category);
        }
        if (status != null) {
            return conversationRepository.findByStatusOrderByCreatedAtDesc(status);
        }
        return conversationRepository.findAllByOrderByCreatedAtDesc();
    }

    private ConversationForm.TurnForm newTurn(String role) {
        ConversationForm.TurnForm turn = new ConversationForm.TurnForm();
        turn.setRole(com.example.hotel.entity.MessageRole.valueOf(role));
        turn.setContent("");
        return turn;
    }
}

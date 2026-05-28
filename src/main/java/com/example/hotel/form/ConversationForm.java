package com.example.hotel.form;

import com.example.hotel.entity.ConversationCategory;
import com.example.hotel.entity.MessageRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ConversationForm {

    @NotBlank
    private String title;

    @NotNull
    private ConversationCategory category;

    @Valid
    private List<TurnForm> turns = new ArrayList<>();

    @Getter
    @Setter
    public static class TurnForm {
        @NotNull
        private MessageRole role;

        @NotBlank
        private String content;
    }
}

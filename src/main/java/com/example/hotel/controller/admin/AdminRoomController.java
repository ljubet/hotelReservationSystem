package com.example.hotel.controller.admin;

import com.example.hotel.entity.Room;
import com.example.hotel.entity.RoomType;
import com.example.hotel.form.RoomForm;
import com.example.hotel.repository.RoomRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminRoomController {

    private final RoomRepository roomRepository;

    public AdminRoomController(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @ModelAttribute("roomTypes")
    RoomType[] roomTypes() {
        return RoomType.values();
    }

    @GetMapping("/admin/rooms")
    public String rooms(Model model) {
        model.addAttribute("rooms", roomRepository.findAll());
        return "admin/rooms/list";
    }

    @GetMapping("/admin/rooms/new")
    public String newRoom(Model model) {
        model.addAttribute("roomForm", new RoomForm());
        model.addAttribute("pageTitle", "Add Room");
        return "admin/rooms/form";
    }

    @PostMapping("/admin/rooms")
    public String create(@Valid @ModelAttribute RoomForm roomForm,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Add Room");
            return "admin/rooms/form";
        }
        roomRepository.save(toRoom(new Room(), roomForm));
        redirectAttributes.addFlashAttribute("success", "Room created.");
        return "redirect:/admin/rooms";
    }

    @GetMapping("/admin/rooms/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        Room room = roomRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Room not found"));
        model.addAttribute("roomForm", toForm(room));
        model.addAttribute("roomId", id);
        model.addAttribute("pageTitle", "Edit Room");
        return "admin/rooms/form";
    }

    @PostMapping("/admin/rooms/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute RoomForm roomForm,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("roomId", id);
            model.addAttribute("pageTitle", "Edit Room");
            return "admin/rooms/form";
        }
        Room room = roomRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Room not found"));
        roomRepository.save(toRoom(room, roomForm));
        redirectAttributes.addFlashAttribute("success", "Room updated.");
        return "redirect:/admin/rooms";
    }

    @PostMapping("/admin/rooms/{id}/toggle-renovation")
    public String toggleRenovation(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Room room = roomRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Room not found"));
        room.setUnderRenovation(!room.isUnderRenovation());
        if (room.isUnderRenovation()) {
            room.setAvailable(false);
        }
        roomRepository.save(room);
        redirectAttributes.addFlashAttribute("success", "Renovation status updated.");
        return "redirect:/admin/rooms";
    }

    @PostMapping("/admin/rooms/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        roomRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Room deleted.");
        return "redirect:/admin/rooms";
    }

    private Room toRoom(Room room, RoomForm form) {
        room.setName(form.getName());
        room.setDescription(form.getDescription());
        room.setRoomType(form.getRoomType());
        room.setPricePerNight(form.getPricePerNight());
        room.setRating(form.getRating());
        room.setCapacity(form.getCapacity());
        room.setImageUrl(form.getImageUrl());
        room.setUnderRenovation(form.isUnderRenovation());
        room.setAvailable(!form.isUnderRenovation() && form.isAvailable());
        room.setAmenities(form.getAmenities());
        return room;
    }

    private RoomForm toForm(Room room) {
        RoomForm form = new RoomForm();
        form.setName(room.getName());
        form.setDescription(room.getDescription());
        form.setRoomType(room.getRoomType());
        form.setPricePerNight(room.getPricePerNight());
        form.setRating(room.getRating());
        form.setCapacity(room.getCapacity());
        form.setImageUrl(room.getImageUrl());
        form.setAvailable(room.isAvailable());
        form.setUnderRenovation(room.isUnderRenovation());
        form.setAmenities(room.getAmenities());
        return form;
    }
}

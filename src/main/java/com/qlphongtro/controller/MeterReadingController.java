package com.qlphongtro.controller;

import com.qlphongtro.entity.MeterReading;
import com.qlphongtro.service.MeterReadingService;
import com.qlphongtro.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequestMapping("/meter-readings")
public class MeterReadingController {

    private final MeterReadingService meterReadingService;
    private final RoomService roomService;

    @Autowired
    public MeterReadingController(MeterReadingService meterReadingService, RoomService roomService) {
        this.meterReadingService = meterReadingService;
        this.roomService = roomService;
    }

    // 1. Danh sách các lần chốt chỉ số điện/nước
    @GetMapping
    public String listReadings(Model model) {
        model.addAttribute("readings", meterReadingService.getAllReadings());
        return "meter-reading/list";
    }

    // 2. Hiển thị form chốt chỉ số mới
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        MeterReading reading = new MeterReading();
        reading.setReadDate(LocalDate.now());
        reading.setBillingMonth(LocalDate.now().withDayOfMonth(1));

        model.addAttribute("reading", reading);
        model.addAttribute("rooms", roomService.getAllRooms());
        return "meter-reading/form";
    }

    // 3. Hiển thị form chỉnh sửa chỉ số
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes) {
        try {
            MeterReading reading = meterReadingService.getReadingById(id);
            model.addAttribute("reading", reading);
            model.addAttribute("rooms", roomService.getAllRooms());
            return "meter-reading/form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/meter-readings";
        }
    }

    // 4. Lưu chốt chỉ số: Controller TỰ ĐỘNG TÍNH TOÁN consumption = end_index - start_index
    @PostMapping("/save")
    public String saveReading(
            @ModelAttribute("reading") MeterReading reading,
            Model model,
            RedirectAttributes redirectAttributes) {

        // Kiểm tra hợp lệ: start_index và end_index bắt buộc phải có
        if (reading.getStartIndex() == null || reading.getEndIndex() == null) {
            model.addAttribute("errorMessage", "Vui lòng nhập đầy đủ chỉ số đầu và chỉ số cuối!");
            model.addAttribute("rooms", roomService.getAllRooms());
            return "meter-reading/form";
        }

        // Kiểm tra logic: Chỉ số cuối không được nhỏ hơn chỉ số đầu
        if (reading.getEndIndex().compareTo(reading.getStartIndex()) < 0) {
            model.addAttribute("errorMessage", "Chỉ số cuối (" + reading.getEndIndex() 
                    + ") không được nhỏ hơn chỉ số đầu (" + reading.getStartIndex() + ")!");
            model.addAttribute("rooms", roomService.getAllRooms());
            return "meter-reading/form";
        }

        // >>> YÊU CẦU: Controller tự động tính toán consumption = end_index - start_index trước khi lưu <<<
        BigDecimal consumption = reading.getEndIndex().subtract(reading.getStartIndex());
        reading.setConsumption(consumption);

        // Gán ngày chốt chỉ số mặc định là hôm nay nếu người dùng chưa chọn
        if (reading.getReadDate() == null) {
            reading.setReadDate(LocalDate.now());
        }

        try {
            meterReadingService.recordReading(reading);
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Chốt chỉ số thành công! Số lượng tiêu thụ được tính: " + consumption);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi lưu dữ liệu: " + e.getMessage());
        }

        return "redirect:/meter-readings";
    }

    // 5. Xóa bản ghi chỉ số
    @GetMapping("/delete/{id}")
    public String deleteReading(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            meterReadingService.deleteReading(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa bản ghi chỉ số thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa: " + e.getMessage());
        }
        return "redirect:/meter-readings";
    }
}

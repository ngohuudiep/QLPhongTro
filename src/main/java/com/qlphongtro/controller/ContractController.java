package com.qlphongtro.controller;

import com.qlphongtro.entity.Contract;
import com.qlphongtro.service.ContractService;
import com.qlphongtro.service.RoomService;
import com.qlphongtro.service.TenantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/contracts")
public class ContractController {

    private final ContractService contractService;
    private final RoomService roomService;
    private final TenantService tenantService;

    @Autowired
    public ContractController(
            ContractService contractService,
            RoomService roomService,
            TenantService tenantService) {
        this.contractService = contractService;
        this.roomService = roomService;
        this.tenantService = tenantService;
    }

    // 1. Danh sách hợp đồng thuê phòng
    @GetMapping
    public String listContracts(Model model) {
        model.addAttribute("contracts", contractService.getAllContracts());
        return "contract/list";
    }

    // 2. Form lập hợp đồng mới
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        Contract contract = new Contract();
        contract.setStartDate(LocalDate.now());
        contract.setEndDate(LocalDate.now().plusMonths(6)); // Mặc định 6 tháng
        contract.setStatus("Hiệu lực");

        model.addAttribute("contract", contract);
        model.addAttribute("rooms", roomService.getAllRooms());
        model.addAttribute("tenants", tenantService.getAllTenants());
        return "contract/form";
    }

    // 3. Form chỉnh sửa hợp đồng
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Contract contract = contractService.getContractById(id);
            model.addAttribute("contract", contract);
            model.addAttribute("rooms", roomService.getAllRooms());
            model.addAttribute("tenants", tenantService.getAllTenants());
            return "contract/form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/contracts";
        }
    }

    // 4. Lưu hợp đồng: Service sẽ tự động cập nhật status của Room thành "Đang thuê" (OCCUPIED)
    @PostMapping("/save")
    public String saveContract(
            @ModelAttribute("contract") Contract contract,
            RedirectAttributes redirectAttributes) {
        try {
            boolean isNew = (contract.getContractId() == null);
            contractService.saveContract(contract);

            String message = isNew 
                    ? "Tạo hợp đồng thành công! Phòng đã được tự động chuyển sang trạng thái 'Đang thuê'."
                    : "Cập nhật thông tin hợp đồng thành công!";
            redirectAttributes.addFlashAttribute("successMessage", message);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/contracts";
    }

    // 5. Xóa hợp đồng
    @GetMapping("/delete/{id}")
    public String deleteContract(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            contractService.deleteContract(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa hợp đồng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa: " + e.getMessage());
        }
        return "redirect:/contracts";
    }

    // 6. Thêm khách thuê vào hợp đồng (Contract_Tenants)
    @PostMapping("/{id}/add-tenant")
    public String addTenantToContract(
            @PathVariable("id") Integer contractId,
            @RequestParam("tenantId") Integer tenantId,
            @RequestParam("moveInDate") LocalDate moveInDate,
            RedirectAttributes redirectAttributes) {
        try {
            contractService.addTenantToContract(contractId, tenantId, moveInDate);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm khách thuê vào hợp đồng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/contracts";
    }
}

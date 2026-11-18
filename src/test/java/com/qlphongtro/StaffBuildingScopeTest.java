package com.qlphongtro;

import com.qlphongtro.entity.Building;
import com.qlphongtro.entity.StaffBuildingAssignment;
import com.qlphongtro.repository.ContractRepository;
import com.qlphongtro.repository.ContractTenantRepository;
import com.qlphongtro.repository.RoomRepository;
import com.qlphongtro.repository.StaffBuildingAssignmentRepository;
import com.qlphongtro.repository.TenantRepository;
import com.qlphongtro.repository.ContractTenantRepository;
import com.qlphongtro.security.BuildingAccess;
import com.qlphongtro.service.impl.ContractServiceImpl;
import com.qlphongtro.service.impl.TenantServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class StaffBuildingScopeTest {
    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void staffContractLookupUsesAssignedBuildingScopeAndHidesOtherBuildings() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        "staff-x", "", List.of(new SimpleGrantedAuthority("ROLE_STAFF"))));
        ContractRepository contracts = mock(ContractRepository.class);
        StaffBuildingAssignmentRepository assignments = mock(StaffBuildingAssignmentRepository.class);
        BuildingAccess access = new BuildingAccess(assignments);
        when(assignments.findByStaff_Username("staff-x")).thenReturn(List.of(assignment(10)));
        when(contracts.findByContractIdAndRoom_Building_BuildingIdIn(9, List.of(10))).thenReturn(Optional.empty());
        ContractServiceImpl service = new ContractServiceImpl(contracts, mock(ContractTenantRepository.class),
                mock(RoomRepository.class), mock(TenantRepository.class), access);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> service.getContractById(9));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(contracts).findByContractIdAndRoom_Building_BuildingIdIn(9, List.of(10));
        verify(contracts, never()).findById(9);
    }

    @Test
    void buildingAccessRejectsBuildingOutsideStaffAssignments() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        "staff-x", "", List.of(new SimpleGrantedAuthority("ROLE_STAFF"))));
        StaffBuildingAssignmentRepository assignments = mock(StaffBuildingAssignmentRepository.class);
        when(assignments.existsByStaff_UsernameAndBuilding_BuildingId("staff-x", 20)).thenReturn(false);
        BuildingAccess access = new BuildingAccess(assignments);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> access.requireBuilding(20));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(assignments).existsByStaff_UsernameAndBuilding_BuildingId("staff-x", 20);
    }

    @Test
    void staffCannotLoadTenantWithoutContractInAnAssignedBuilding() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        "staff-x", "", List.of(new SimpleGrantedAuthority("ROLE_STAFF"))));
        StaffBuildingAssignmentRepository assignments = mock(StaffBuildingAssignmentRepository.class);
        BuildingAccess access = new BuildingAccess(assignments);
        when(assignments.findByStaff_Username("staff-x")).thenReturn(List.of(assignment(10)));
        ContractRepository contracts = mock(ContractRepository.class);
        when(contracts.findDistinctByRoom_Building_BuildingIdIn(List.of(10))).thenReturn(List.of());
        TenantServiceImpl service = new TenantServiceImpl(mock(TenantRepository.class), mock(ContractTenantRepository.class), contracts, access);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> service.getTenantById(55));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(contracts).findDistinctByRoom_Building_BuildingIdIn(List.of(10));
    }

    private StaffBuildingAssignment assignment(Integer buildingId) {
        Building building = new Building();
        building.setBuildingId(buildingId);
        StaffBuildingAssignment assignment = new StaffBuildingAssignment();
        assignment.setBuilding(building);
        return assignment;
    }
}

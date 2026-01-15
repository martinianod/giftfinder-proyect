package com.findoraai.giftfinder.organization.service;

import com.findoraai.giftfinder.auth.model.Role;
import com.findoraai.giftfinder.auth.model.User;
import com.findoraai.giftfinder.auth.repository.UserRepository;
import com.findoraai.giftfinder.notifications.model.Recipient;
import com.findoraai.giftfinder.notifications.repository.RecipientRepository;
import com.findoraai.giftfinder.organization.dto.AddMemberRequest;
import com.findoraai.giftfinder.organization.dto.OrganizationMemberResponse;
import com.findoraai.giftfinder.organization.dto.OrganizationRequest;
import com.findoraai.giftfinder.organization.dto.OrganizationResponse;
import com.findoraai.giftfinder.organization.model.Organization;
import com.findoraai.giftfinder.organization.model.OrganizationMember;
import com.findoraai.giftfinder.organization.model.OrganizationRole;
import com.findoraai.giftfinder.organization.repository.OrganizationMemberRepository;
import com.findoraai.giftfinder.organization.repository.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceImplTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private OrganizationMemberRepository organizationMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RecipientRepository recipientRepository;

    @InjectMocks
    private OrganizationServiceImpl organizationService;

    private User testUser;
    private Organization testOrganization;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .role(Role.USER)
                .build();

        testOrganization = Organization.builder()
                .id(1L)
                .name("Test Organization")
                .description("Test Description")
                .giftBudget(new BigDecimal("1000.00"))
                .build();
    }

    @Test
    void testCreateOrganization() {
        OrganizationRequest request = OrganizationRequest.builder()
                .name("Test Organization")
                .description("Test Description")
                .giftBudget(new BigDecimal("1000.00"))
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(organizationRepository.save(any(Organization.class))).thenReturn(testOrganization);
        when(organizationMemberRepository.save(any(OrganizationMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrganizationResponse response = organizationService.createOrganization(request, 1L);

        assertNotNull(response);
        assertEquals("Test Organization", response.getName());
        assertEquals("Test Description", response.getDescription());
        assertEquals(new BigDecimal("1000.00"), response.getGiftBudget());

        verify(organizationRepository).save(any(Organization.class));
        verify(organizationMemberRepository).save(any(OrganizationMember.class));
    }

    @Test
    void testAddMember_Success() {
        User newMember = User.builder()
                .id(2L)
                .email("member@example.com")
                .name("New Member")
                .role(Role.USER)
                .build();

        OrganizationMember ownerMember = OrganizationMember.builder()
                .id(1L)
                .organization(testOrganization)
                .user(testUser)
                .role(OrganizationRole.OWNER)
                .build();

        AddMemberRequest request = AddMemberRequest.builder()
                .userEmail("member@example.com")
                .role(OrganizationRole.MEMBER)
                .build();

        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(newMember));
        when(organizationMemberRepository.findByOrganizationAndUser(testOrganization, testUser))
                .thenReturn(Optional.of(ownerMember));
        when(organizationMemberRepository.findByOrganizationAndUser(testOrganization, newMember))
                .thenReturn(Optional.empty());
        when(organizationMemberRepository.save(any(OrganizationMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrganizationMemberResponse response = organizationService.addMember(1L, request, 1L);

        assertNotNull(response);
        assertEquals("member@example.com", response.getUserEmail());
        assertEquals(OrganizationRole.MEMBER, response.getRole());

        verify(organizationMemberRepository).save(any(OrganizationMember.class));
    }

    @Test
    void testAddMember_NotAuthorized() {
        User regularMember = User.builder()
                .id(2L)
                .email("member@example.com")
                .name("Regular Member")
                .role(Role.USER)
                .build();

        OrganizationMember memberRecord = OrganizationMember.builder()
                .id(1L)
                .organization(testOrganization)
                .user(regularMember)
                .role(OrganizationRole.MEMBER)
                .build();

        AddMemberRequest request = AddMemberRequest.builder()
                .userEmail("newuser@example.com")
                .role(OrganizationRole.MEMBER)
                .build();

        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
        when(userRepository.findById(2L)).thenReturn(Optional.of(regularMember));
        when(organizationMemberRepository.findByOrganizationAndUser(testOrganization, regularMember))
                .thenReturn(Optional.of(memberRecord));

        assertThrows(SecurityException.class, () -> {
            organizationService.addMember(1L, request, 2L);
        });

        verify(organizationMemberRepository, never()).save(any(OrganizationMember.class));
    }

    @Test
    void testGetOrganizationRecipients() {
        OrganizationMember member = OrganizationMember.builder()
                .organization(testOrganization)
                .user(testUser)
                .role(OrganizationRole.MEMBER)
                .build();

        Recipient recipient1 = new Recipient();
        recipient1.setName("Recipient 1");
        Recipient recipient2 = new Recipient();
        recipient2.setName("Recipient 2");

        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(organizationMemberRepository.findByOrganizationAndUser(testOrganization, testUser))
                .thenReturn(Optional.of(member));
        when(recipientRepository.findByOrganization(testOrganization))
                .thenReturn(Arrays.asList(recipient1, recipient2));

        List<Recipient> recipients = organizationService.getOrganizationRecipients(1L, 1L);

        assertNotNull(recipients);
        assertEquals(2, recipients.size());
        assertEquals("Recipient 1", recipients.get(0).getName());
        assertEquals("Recipient 2", recipients.get(1).getName());
    }

    @Test
    void testIsOwnerOrAdmin_Owner() {
        OrganizationMember ownerMember = OrganizationMember.builder()
                .organization(testOrganization)
                .user(testUser)
                .role(OrganizationRole.OWNER)
                .build();

        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(organizationMemberRepository.findByOrganizationAndUser(testOrganization, testUser))
                .thenReturn(Optional.of(ownerMember));

        boolean result = organizationService.isOwnerOrAdmin(1L, 1L);

        assertTrue(result);
    }

    @Test
    void testIsOwnerOrAdmin_Admin() {
        OrganizationMember adminMember = OrganizationMember.builder()
                .organization(testOrganization)
                .user(testUser)
                .role(OrganizationRole.ADMIN)
                .build();

        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(organizationMemberRepository.findByOrganizationAndUser(testOrganization, testUser))
                .thenReturn(Optional.of(adminMember));

        boolean result = organizationService.isOwnerOrAdmin(1L, 1L);

        assertTrue(result);
    }

    @Test
    void testIsOwnerOrAdmin_Member() {
        OrganizationMember regularMember = OrganizationMember.builder()
                .organization(testOrganization)
                .user(testUser)
                .role(OrganizationRole.MEMBER)
                .build();

        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(organizationMemberRepository.findByOrganizationAndUser(testOrganization, testUser))
                .thenReturn(Optional.of(regularMember));

        boolean result = organizationService.isOwnerOrAdmin(1L, 1L);

        assertFalse(result);
    }
}
